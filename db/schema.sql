-- =============================================================================
-- gerenciador - esquema completo (PostgreSQL 18)
--
-- Reescrito do zero com os ajustes do documento de analise.
-- Rode com o banco ainda vazio: este script APAGA tudo que existe no schema
-- public antes de recriar (inclusive as 2 linhas de teste de caixa_da_agua).
--
--   psql -U postgres -d gerenciador -f db/schema.sql
--   psql -U postgres -d gerenciador -f db/seed.sql
--
-- Convencoes adotadas:
--   * tabela no singular, snake_case;
--   * id sempre INTEGER GENERATED ALWAYS AS IDENTITY;
--   * dinheiro em NUMERIC, nunca em double precision;
--   * dominio fechado (cor, status, setor, tipo de servico, forma de pagamento)
--     vive como enum no Kotlin; o banco guarda o name() em VARCHAR e NAO tem
--     CHECK. Uma fonte de verdade so - quem valida e o Service, antes do INSERT.
--     Preco de item de venda continua NUMERIC, nunca double;
--   * TIMESTAMP sem timezone, para casar com LocalDateTime no Kotlin (loja
--     unica, um fuso so).
-- =============================================================================

DROP VIEW  IF EXISTS vw_conferencia_venda       CASCADE;
DROP VIEW  IF EXISTS vw_conferencia_saldo       CASCADE;
DROP VIEW  IF EXISTS vw_contas_receber          CASCADE;
DROP VIEW  IF EXISTS vw_ordem_servico_a_faturar CASCADE;
DROP VIEW  IF EXISTS vw_ordem_servico_aberta    CASCADE;

DROP TABLE IF EXISTS movimento     CASCADE;
DROP TABLE IF EXISTS venda_item    CASCADE;
DROP TABLE IF EXISTS venda         CASCADE;
DROP TABLE IF EXISTS ordem_servico CASCADE;
DROP TABLE IF EXISTS caixa_da_agua CASCADE;
DROP TABLE IF EXISTS conta         CASCADE;
DROP TABLE IF EXISTS funcionario   CASCADE;
DROP TABLE IF EXISTS cliente       CASCADE;
DROP TABLE IF EXISTS pessoa        CASCADE;

-- tabelas que deixaram de existir: viraram enum no Kotlin ou foram renomeadas
DROP TABLE IF EXISTS setor        CASCADE;
DROP TABLE IF EXISTS tipo_servico CASCADE;
DROP TABLE IF EXISTS servico      CASCADE;
DROP TABLE IF EXISTS servicos     CASCADE;
DROP TABLE IF EXISTS financeiro   CASCADE;


-- =============================================================================
-- 1. PESSOAS E PAPEIS
--    Uma pessoa, N papeis. O CPF/CNPJ e a chave de negocio: e por ele que o
--    cadastro entra, e e ele que impede a mesma pessoa virar duas linhas.
-- =============================================================================

CREATE TABLE pessoa (
    id        INTEGER      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome      VARCHAR(120) NOT NULL,
    cpf_cnpj  VARCHAR(14)  NOT NULL,
    telefone  VARCHAR(11)  NOT NULL,
    criado_em TIMESTAMP    NOT NULL DEFAULT now(),

    CONSTRAINT pessoa_cpf_cnpj_uk UNIQUE (cpf_cnpj),
    CONSTRAINT pessoa_nome_ck     CHECK (length(btrim(nome)) >= 3),
    CONSTRAINT pessoa_cpf_cnpj_ck CHECK (cpf_cnpj ~ '^\d{11}$' OR cpf_cnpj ~ '^\d{14}$'),
    CONSTRAINT pessoa_telefone_ck CHECK (telefone ~ '^\d{10,11}$')
);

CREATE TABLE cliente (
    id             INTEGER       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    pessoa_id      INTEGER       NOT NULL,
    limite_credito NUMERIC(10,2) NOT NULL DEFAULT 0,
    criado_em      TIMESTAMP     NOT NULL DEFAULT now(),

    CONSTRAINT cliente_pessoa_fk FOREIGN KEY (pessoa_id) REFERENCES pessoa (id),
    -- a mesma pessoa nao pode ser cliente duas vezes
    CONSTRAINT cliente_pessoa_uk UNIQUE (pessoa_id),
    CONSTRAINT cliente_limite_ck CHECK (limite_credito >= 0)
);

CREATE TABLE funcionario (
    id            INTEGER       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    pessoa_id     INTEGER       NOT NULL,
    setor         VARCHAR(60)   NOT NULL,
    salario       NUMERIC(10,2) NOT NULL,
    data_admissao DATE          NOT NULL DEFAULT CURRENT_DATE,
    -- funcionario ativo = data_demissao IS NULL. Demitido nao vira DELETE:
    -- ele assinou vendas e ordens de servico que continuam existindo.
    data_demissao DATE          NULL,

    CONSTRAINT funcionario_pessoa_fk FOREIGN KEY (pessoa_id) REFERENCES pessoa (id),
    CONSTRAINT funcionario_pessoa_uk UNIQUE (pessoa_id),
    CONSTRAINT funcionario_salario_ck  CHECK (salario > 0),
    CONSTRAINT funcionario_demissao_ck CHECK (data_demissao IS NULL OR data_demissao >= data_admissao)
);

-- Generalizacao da antiga tabela financeiro: qualquer pessoa pode ter conta.
-- A conta da empresa e a conta de uma pessoa juridica - e so isso. Assim,
-- pagar salario usa o mesmo mecanismo de receber de um cliente.
CREATE TABLE conta (
    id        INTEGER       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    pessoa_id INTEGER       NOT NULL,
    descricao VARCHAR(60)   NOT NULL,
    -- saldo e cache: a verdade esta em movimento. vw_conferencia_saldo confere
    -- os dois. Quem atualiza esta coluna e o Service, na mesma transacao que
    -- grava o movimento - nunca em um UPDATE solto.
    saldo     NUMERIC(12,2) NOT NULL DEFAULT 0,

    CONSTRAINT conta_pessoa_fk FOREIGN KEY (pessoa_id) REFERENCES pessoa (id),
    CONSTRAINT conta_pessoa_uk UNIQUE (pessoa_id)
);


-- =============================================================================
-- 2. CATALOGO
--    caixa_da_agua e catalogo de MODELO, nao unidade fisica. Duas caixas iguais
--    no deposito sao uma linha com estoque_atual = 2. Por isso venda_item tem
--    quantidade, e por isso a venda desconta estoque em vez de marcar vendida.
-- =============================================================================

CREATE TABLE caixa_da_agua (
    id                INTEGER       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    marca             VARCHAR(60)   NOT NULL,
    modelo            VARCHAR(60)   NOT NULL,
    -- capacidade e o atributo pelo qual esse produto e vendido; faltava.
    capacidade        INTEGER       NOT NULL,
    altura            NUMERIC(6,2)  NOT NULL,
    largura           NUMERIC(6,2)  NOT NULL,
    profundidade      NUMERIC(6,2)  NOT NULL,
    -- enums Cor, Material e Formato (src/enums). Guardamos o name(), sem CHECK.
    cor               VARCHAR(20)   NOT NULL,
    material          VARCHAR(20)   NOT NULL,
    formato           VARCHAR(20)   NOT NULL,
    preco             NUMERIC(10,2) NOT NULL,
    estoque_atual     INTEGER       NOT NULL DEFAULT 0,
    -- catalogo nao some. Modelo descontinuado vira ativo = FALSE: ele some da
    -- lista de venda e continua existindo para as vendas antigas que o citam.
    -- DELETE so e possivel enquanto nenhum venda_item apontar para a linha.
    ativo             BOOLEAN       NOT NULL DEFAULT TRUE,
    criado_em         TIMESTAMP     NOT NULL DEFAULT now(),

    CONSTRAINT caixa_da_agua_modelo_uk UNIQUE (marca, modelo, capacidade),

    CONSTRAINT caixa_da_agua_capacidade_ck CHECK (capacidade > 0),
    CONSTRAINT caixa_da_agua_medidas_ck    CHECK (altura > 0 AND largura > 0 AND profundidade > 0),
    CONSTRAINT caixa_da_agua_preco_ck      CHECK (preco > 0),
    CONSTRAINT caixa_da_agua_estoque_ck    CHECK (estoque_atual >= 0)
);

-- =============================================================================
-- 3. ORDEM DE SERVICO
--    Era a tabela servico. A tabela tipo_servico deixou de existir: o tipo virou
--    enum TipoServico no Kotlin e o preco e digitado na abertura da ordem, entao
--    nao sobrou nenhuma coluna para a tabela guardar.
--
--    STATUS - so o ciclo do trabalho, uma dimensao so:
--
--      AGENDADO ---> EM_EXECUCAO ---> CONCLUIDO
--          |              |
--          +----> CANCELADO <---+
--
--    Nao existe FATURADO. "Ja foi cobrada?" nao e estado do trabalho, e um fato
--    do financeiro, e ja da para responder olhando venda_item: se existe um item
--    apontando para a ordem, ela foi faturada (ver vw_ordem_servico_a_faturar).
--    Guardar isso tambem no status criaria duas verdades que podem divergir -
--    venda cancelada e ordem parada em FATURADO para sempre.
--
--    Regras de transicao (o Service aplica; o banco nao conhece mais os valores):
--      AGENDADO    -> EM_EXECUCAO   exige funcionario_id preenchido
--      EM_EXECUCAO -> CONCLUIDO     exige data_conclusao
--      AGENDADO / EM_EXECUCAO -> CANCELADO
--      CONCLUIDO e CANCELADO sao finais
--      so CONCLUIDO pode virar item de venda
-- =============================================================================

CREATE TABLE ordem_servico (
    id             INTEGER       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- enum TipoServico: INSTALACAO, LIMPEZA, MANUTENCAO, TROCA...
    tipo_servico   VARCHAR(40)   NOT NULL,
    cliente_id     INTEGER       NOT NULL,
    -- nulo enquanto ninguem foi designado
    funcionario_id INTEGER       NULL,
    -- enum StatusOrdemServico
    status         VARCHAR(15)   NOT NULL DEFAULT 'AGENDADO',
    -- digitado na abertura e congelado aqui. Nao existe tabela de preco de
    -- servico: cada ordem e negociada na hora (distancia, altura, dificuldade).
    preco          NUMERIC(10,2) NOT NULL,
    data_agendada  TIMESTAMP     NOT NULL,
    data_conclusao TIMESTAMP     NULL,
    observacao     VARCHAR(255)  NULL,
    criado_em      TIMESTAMP     NOT NULL DEFAULT now(),

    CONSTRAINT ordem_servico_cliente_fk     FOREIGN KEY (cliente_id)     REFERENCES cliente (id),
    CONSTRAINT ordem_servico_funcionario_fk FOREIGN KEY (funcionario_id) REFERENCES funcionario (id),

    CONSTRAINT ordem_servico_preco_ck CHECK (preco >= 0),
    CONSTRAINT ordem_servico_datas_ck CHECK (
        data_conclusao IS NULL OR data_conclusao >= data_agendada
    )
);


-- =============================================================================
-- 4. VENDA
--    O centro do sistema. venda guarda o cabecalho; venda_item guarda o que foi
--    vendido. Uma venda pode ter N caixas e N servicos - antes cabia uma caixa
--    e um servico, sem quantidade.
-- =============================================================================

CREATE TABLE venda (
    id                 INTEGER       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cliente_id         INTEGER       NOT NULL,
    funcionario_id     INTEGER       NOT NULL,
    data_venda         TIMESTAMP     NOT NULL DEFAULT now(),
    -- enum CondicaoPagamento: A_VISTA, A_PRAZO
    condicao_pagamento VARCHAR(10)   NOT NULL,
    -- enum StatusVenda: EFETIVADA, CANCELADA - e so isso.
    -- PAGA nao entra aqui: se a venda foi quitada e a soma dos movimentos dela,
    -- nao um estado. Ver vw_contas_receber.
    status             VARCHAR(10)   NOT NULL DEFAULT 'EFETIVADA',
    -- soma dos itens, gravada na mesma transacao. vw_conferencia_venda confere.
    valor_total        NUMERIC(12,2) NOT NULL,
    observacao         VARCHAR(255)  NULL,

    CONSTRAINT venda_cliente_fk     FOREIGN KEY (cliente_id)     REFERENCES cliente (id),
    CONSTRAINT venda_funcionario_fk FOREIGN KEY (funcionario_id) REFERENCES funcionario (id),

    CONSTRAINT venda_valor_ck CHECK (valor_total >= 0)
);

CREATE TABLE venda_item (
    id               INTEGER       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    venda_id         INTEGER       NOT NULL,
    -- exatamente um dos dois: ou o item e uma caixa, ou e uma ordem de servico
    caixa_da_agua_id INTEGER       NULL,
    ordem_servico_id INTEGER       NULL,
    -- Por que o item guarda preco e descricao em vez de ler do catalogo:
    --   1. o preco do catalogo e o preco de HOJE. Se a caixa subir de 279,90
    --      para 299,90, uma venda antiga lida por JOIN passa a valer mais do que
    --      o cliente pagou - e vw_contas_receber acusa divida que nao existe;
    --   2. desconto e negociacao: vender por 260 nao tem onde ser gravado se o
    --      valor vem do catalogo;
    --   3. ordem de servico tem preco proprio, digitado na abertura. Com o preco
    --      no item, ler o valor de qualquer linha e sempre a mesma coluna, seja
    --      caixa ou servico;
    --   4. a caixa pode sair do catalogo; a venda continua tendo que dizer
    --      quanto custou.
    -- Mesma logica para descricao: e o nome do produto na data da venda.
    descricao        VARCHAR(120)  NOT NULL,
    quantidade       INTEGER       NOT NULL,
    preco_unitario   NUMERIC(10,2) NOT NULL,
    subtotal         NUMERIC(12,2) GENERATED ALWAYS AS (quantidade * preco_unitario) STORED,

    CONSTRAINT venda_item_venda_fk FOREIGN KEY (venda_id)         REFERENCES venda (id) ON DELETE CASCADE,
    CONSTRAINT venda_item_caixa_fk FOREIGN KEY (caixa_da_agua_id) REFERENCES caixa_da_agua (id),
    CONSTRAINT venda_item_ordem_fk FOREIGN KEY (ordem_servico_id) REFERENCES ordem_servico (id),

    CONSTRAINT venda_item_alvo_ck CHECK (num_nonnulls(caixa_da_agua_id, ordem_servico_id) = 1),
    CONSTRAINT venda_item_qtd_ck  CHECK (quantidade > 0),
    -- servico nao tem quantidade: e uma ordem, uma linha
    CONSTRAINT venda_item_qtd_servico_ck CHECK (ordem_servico_id IS NULL OR quantidade = 1),
    CONSTRAINT venda_item_preco_ck CHECK (preco_unitario >= 0)
);


-- =============================================================================
-- 5. FINANCEIRO
--    Um movimento e sempre positivo; quem da o sinal e o tipo. Estorno nao e um
--    tipo: e um movimento de sinal contrario que aponta para o original, por
--    estorno_de_id. Assim o saldo nunca fica ambiguo e da para auditar o par.
--
--    a vista     -> ENTRADA na hora
--    a prazo     -> nenhum movimento; a venda nasce em aberto
--    recebimento -> ENTRADA com venda_id preenchido, baixa parcial ou total
--    cancelada   -> SAIDA com estorno_de_id apontando para a ENTRADA original
--    salario     -> SAIDA com pessoa_id do funcionario e venda_id nulo
-- =============================================================================

CREATE TABLE movimento (
    id                INTEGER       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    -- ONDE o dinheiro entrou ou saiu. Hoje e sempre o caixa da loja; existe
    -- como coluna para o dia em que houver duas contas (especie e banco). E por
    -- conta_id que o saldo e calculado - por isso e obrigatorio.
    conta_id          INTEGER       NOT NULL,

    -- enum TipoMovimento: ENTRADA ou SAIDA. Quem da o sinal e o tipo; valor e
    -- sempre positivo.
    tipo              VARCHAR(10)   NOT NULL,
    valor             NUMERIC(12,2) NOT NULL,

    -- enum FormaPagamento: DINHEIRO, PIX, DEBITO, CREDITO, BOLETO, TRANSFERENCIA
    forma_pagamento   VARCHAR(15)   NOT NULL,

    -- POR QUE o movimento existe, quando o motivo e uma venda. E o que liga
    -- pagamento a divida: sem ele, contas a receber nao fecha. Nulo em salario,
    -- despesa, aporte.
    venda_id          INTEGER       NULL,

    -- COM QUEM foi a transacao - a contraparte. O cliente que pagou, o
    -- funcionario que recebeu salario. Aponta para pessoa (nao para cliente nem
    -- funcionario) justamente para caber os dois. Nulo quando nao ha contraparte
    -- identificada: taxa do banco, acerto de caixa.
    pessoa_id         INTEGER       NULL,

    -- QUAL movimento este aqui cancela. Movimento nunca e apagado nem editado -
    -- e registro contabil. Errou o valor? Grava um movimento de tipo contrario
    -- apontando para o original. O UNIQUE abaixo garante que um movimento so e
    -- estornado uma vez, e "movimentos validos" viram os que nao tem estorno.
    estorno_de_id     INTEGER       NULL,

    descricao         VARCHAR(120)  NOT NULL,
    data_movimentacao TIMESTAMP     NOT NULL DEFAULT now(),

    CONSTRAINT movimento_conta_fk   FOREIGN KEY (conta_id)      REFERENCES conta (id),
    CONSTRAINT movimento_venda_fk   FOREIGN KEY (venda_id)      REFERENCES venda (id),
    CONSTRAINT movimento_pessoa_fk  FOREIGN KEY (pessoa_id)     REFERENCES pessoa (id),
    CONSTRAINT movimento_estorno_fk FOREIGN KEY (estorno_de_id) REFERENCES movimento (id),

    -- um movimento so pode ser estornado uma vez
    CONSTRAINT movimento_estorno_uk UNIQUE (estorno_de_id),

    CONSTRAINT movimento_valor_ck   CHECK (valor > 0),
    CONSTRAINT movimento_estorno_ck CHECK (estorno_de_id IS NULL OR estorno_de_id <> id)
);

-- Exemplos, para fixar as tres colunas:
--
--  situacao                          | conta_id | tipo    | valor   | venda_id | pessoa_id | estorno_de_id
--  ----------------------------------+----------+---------+---------+----------+-----------+--------------
--  venda a vista de 559,80           | 1 (loja) | ENTRADA |  559,80 |    12    | Joao      |    -
--  parcela de uma venda a prazo      | 1 (loja) | ENTRADA |  300,00 |    12    | Joao      |    -
--  venda 12 cancelada, devolve       | 1 (loja) | SAIDA   |  300,00 |    12    | Joao      |   47
--  salario da Maria                  | 1 (loja) | SAIDA   | 1800,00 |    -     | Maria     |    -
--  tarifa do banco                   | 1 (loja) | SAIDA   |   35,00 |    -     |  -        |    -


-- =============================================================================
-- 6. INDICES
--    O Postgres cria indice para PK e UNIQUE, mas NAO para chave estrangeira.
--    Sem estes, "vendas do cliente X" e varredura de tabela inteira.
-- =============================================================================

CREATE INDEX ix_ordem_servico_cliente     ON ordem_servico (cliente_id);
CREATE INDEX ix_ordem_servico_funcionario ON ordem_servico (funcionario_id);
-- indice parcial: a agenda do dia so olha o que esta em aberto
CREATE INDEX ix_ordem_servico_agenda ON ordem_servico (data_agendada)
    WHERE status IN ('AGENDADO', 'EM_EXECUCAO');

CREATE INDEX ix_venda_cliente     ON venda (cliente_id);
CREATE INDEX ix_venda_funcionario ON venda (funcionario_id);
CREATE INDEX ix_venda_data        ON venda (data_venda);

CREATE INDEX ix_venda_item_venda ON venda_item (venda_id);
CREATE INDEX ix_venda_item_caixa ON venda_item (caixa_da_agua_id);
-- uma ordem de servico so pode ser faturada uma vez
CREATE UNIQUE INDEX ux_venda_item_ordem ON venda_item (ordem_servico_id)
    WHERE ordem_servico_id IS NOT NULL;

CREATE INDEX ix_movimento_conta  ON movimento (conta_id);
CREATE INDEX ix_movimento_venda  ON movimento (venda_id);
CREATE INDEX ix_movimento_pessoa ON movimento (pessoa_id);
CREATE INDEX ix_movimento_data   ON movimento (data_movimentacao);


-- =============================================================================
-- 7. VIEWS - CONSULTAS DO DIA A DIA
--
--    O QUE E: uma view e uma consulta salva com nome. Nao guarda dado nenhum,
--    nao ocupa espaco e nunca desatualiza - a cada SELECT nela, o Postgres roda
--    o SELECT de dentro sobre as tabelas de verdade.
--
--    POR QUE USAR: a consulta fica escrita uma vez, aqui, em vez de virar uma
--    String de 20 linhas copiada em tres DAOs. Do lado do Kotlin o codigo passa
--    a ser "SELECT * FROM vw_contas_receber". Se a regra mudar, muda em um lugar
--    e todo mundo enxerga a mudanca.
--
--    LIMITE: nao da para INSERT/UPDATE nestas (tem JOIN e GROUP BY), e como
--    recalculam a cada chamada, view pesada sobre tabela grande fica lenta - a
--    saida ai e indice, ou MATERIALIZED VIEW (que ai sim grava o resultado e
--    precisa de REFRESH).
--
--    Aqui elas sao de dois tipos: as duas primeiras respondem pergunta de
--    negocio; as duas ultimas sao auditoria e devem voltar SEMPRE VAZIAS.
-- =============================================================================

-- 1) A AGENDA. "O que tem para hoje?" - a tela mais usada no dia a dia.
--    Junta ordem + cliente + telefone + responsavel e filtra so o que esta em
--    aberto, para o atendente ver a lista e ja ter o numero para ligar.
CREATE VIEW vw_ordem_servico_aberta AS
SELECT os.id AS ordem_id,
       os.data_agendada,
       os.status,
       os.tipo_servico AS servico,
       pc.nome         AS cliente,
       pc.telefone     AS telefone_cliente,
       pf.nome         AS responsavel,
       os.preco
  FROM ordem_servico os
  JOIN cliente  c ON  c.id = os.cliente_id
  JOIN pessoa  pc ON pc.id = c.pessoa_id
  LEFT JOIN funcionario f ON  f.id = os.funcionario_id
  LEFT JOIN pessoa     pf ON pf.id = f.pessoa_id
 WHERE os.status IN ('AGENDADO', 'EM_EXECUCAO');

-- 2) O QUE FOI FEITO E NAO FOI COBRADO. Substitui o antigo status FATURADO:
--    a ordem esta concluida e nao existe nenhum item de venda apontando para
--    ela. Serve de fila de faturamento - e o dinheiro esquecido na gaveta.
CREATE VIEW vw_ordem_servico_a_faturar AS
SELECT os.id AS ordem_id,
       os.data_conclusao,
       os.tipo_servico AS servico,
       pc.nome         AS cliente,
       os.preco
  FROM ordem_servico os
  JOIN cliente  c ON  c.id = os.cliente_id
  JOIN pessoa  pc ON pc.id = c.pessoa_id
 WHERE os.status = 'CONCLUIDO'
   AND NOT EXISTS (SELECT 1 FROM venda_item vi WHERE vi.ordem_servico_id = os.id);

-- 3) QUEM ME DEVE. Para cada venda efetivada: valor_total menos a soma dos
--    movimentos daquela venda; so aparece quem sobrou saldo. E a prova de que
--    "contas a receber" nao precisa ser tabela - e uma pergunta, nao um cadastro.
CREATE VIEW vw_contas_receber AS
SELECT v.id AS venda_id,
       v.data_venda,
       p.nome AS cliente,
       p.telefone,
       v.valor_total,
       COALESCE(pg.pago, 0)                 AS pago,
       v.valor_total - COALESCE(pg.pago, 0) AS saldo_devedor
  FROM venda v
  JOIN cliente c ON c.id = v.cliente_id
  JOIN pessoa  p ON p.id = c.pessoa_id
  LEFT JOIN LATERAL (
        SELECT SUM(CASE WHEN m.tipo = 'ENTRADA' THEN m.valor ELSE -m.valor END) AS pago
          FROM movimento m
         WHERE m.venda_id = v.id
  ) pg ON TRUE
 WHERE v.status = 'EFETIVADA'
   AND v.valor_total - COALESCE(pg.pago, 0) > 0;

-- 4) AUDITORIA DO CAIXA. conta.saldo e um numero guardado; a verdade e a soma
--    dos movimentos. Esta view lista as contas onde os dois DISCORDAM, entao o
--    resultado correto e nenhuma linha. Se voltar linha, alguma operacao gravou
--    o movimento e nao atualizou o saldo (ou o contrario) - exatamente o bug que
--    a camada Service com commit/rollback existe para impedir.
CREATE VIEW vw_conferencia_saldo AS
SELECT c.id AS conta_id,
       c.descricao,
       c.saldo AS saldo_gravado,
       COALESCE(SUM(CASE WHEN m.tipo = 'ENTRADA' THEN m.valor ELSE -m.valor END), 0) AS saldo_calculado
  FROM conta c
  LEFT JOIN movimento m ON m.conta_id = c.id
 GROUP BY c.id, c.descricao, c.saldo
HAVING c.saldo <> COALESCE(SUM(CASE WHEN m.tipo = 'ENTRADA' THEN m.valor ELSE -m.valor END), 0);

-- 5) AUDITORIA DA VENDA. Mesma ideia para venda.valor_total contra a soma dos
--    itens. Tambem deve vir vazia. Vale rodar as duas num item "Conferencia" do
--    menu, ou na abertura do sistema.
CREATE VIEW vw_conferencia_venda AS
SELECT v.id AS venda_id,
       v.valor_total,
       COALESCE(SUM(i.subtotal), 0) AS soma_itens
  FROM venda v
  LEFT JOIN venda_item i ON i.venda_id = v.id
 GROUP BY v.id, v.valor_total
HAVING v.valor_total <> COALESCE(SUM(i.subtotal), 0);
