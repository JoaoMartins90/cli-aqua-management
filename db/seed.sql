-- =============================================================================
-- gerenciador - carga inicial
--
--   psql -U postgres -d gerenciador -f db/seed.sql
--
-- Rode depois de db/schema.sql. Duas partes:
--   1. dados que o sistema PRECISA para funcionar (a conta da empresa e o
--      catalogo de caixas);
--   2. um bloco DEMO, no fim, so para ter um cliente e um vendedor com que
--      testar a venda. Apague esse bloco antes de usar de verdade.
--
-- Setor e tipo de servico nao aparecem aqui: viraram enum no Kotlin, entao nao
-- ha o que carregar - a lista mora no codigo.
--
-- Os ids sao GENERATED ALWAYS: nenhum INSERT informa id. As chaves estrangeiras
-- sao resolvidas por subconsulta no CPF/CNPJ, para o script poder ser lido.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. A empresa e a conta dela
--    A empresa e uma pessoa juridica como qualquer outra; a conta e a conta
--    dessa pessoa. E o que permite pagar salario pelo mesmo caminho por onde
--    se recebe de um cliente.
--    >> Troque nome, CNPJ e telefone pelos dados reais da loja.
-- -----------------------------------------------------------------------------
INSERT INTO pessoa (nome, cpf_cnpj, telefone) VALUES
    ('Gerenciador Caixas Ltda', '11222333000181', '8330000000');

INSERT INTO conta (pessoa_id, descricao, saldo)
SELECT id, 'Caixa da loja', 0
  FROM pessoa
 WHERE cpf_cnpj = '11222333000181';


-- -----------------------------------------------------------------------------
-- 2. Catalogo de caixas
--    Medidas em metros. cor, material e formato guardam o name() dos enums
--    Kotlin em src/enums - o banco nao valida mais esses valores, quem valida
--    e o Service antes do INSERT.
-- -----------------------------------------------------------------------------
INSERT INTO caixa_da_agua
    (marca, modelo, capacidade_litros, altura, largura, profundidade,
     cor, material, formato, preco, estoque_atual)
VALUES
    ('Tigre',   'Basic 310',      310, 0.55, 0.90, 0.90, 'AZUL', 'PVC', 'QUADRADA',  189.90, 12),
    ('Tigre',   'Basic 500',      500, 0.65, 1.05, 1.05, 'AZUL', 'PVC', 'QUADRADA',  279.90,  8),
    ('Fortlev', 'Standard 1000', 1000, 0.85, 1.30, 1.30, 'AZUL', 'PVC', 'QUADRADA',  549.90,  5),
    ('Fortlev', 'Standard 2000', 2000, 1.05, 1.60, 1.60, 'AZUL', 'PVC', 'QUADRADA', 1049.90,  2);


-- =============================================================================
-- 3. DEMO - apague daqui para baixo antes de usar em producao
--    Um vendedor e um cliente, so para ter com quem fechar a primeira venda.
-- =============================================================================

INSERT INTO pessoa (nome, cpf_cnpj, telefone) VALUES
    ('Maria Vendedora', '11122233344', '83988880000'),
    ('Joao Cliente',    '55566677788', '83977770000');

INSERT INTO funcionario (pessoa_id, setor, salario, data_admissao)
SELECT id, 'VENDAS', 1800.00, CURRENT_DATE
  FROM pessoa
 WHERE cpf_cnpj = '11122233344';

INSERT INTO cliente (pessoa_id, limite_credito)
SELECT id, 1000.00
  FROM pessoa
 WHERE cpf_cnpj = '55566677788';
