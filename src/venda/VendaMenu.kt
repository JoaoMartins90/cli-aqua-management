package venda

import caixadaagua.CaixaDaAgua
import caixadaagua.CaixaDaAguaService
import enums.CondicaoPagamento
import enums.FormaPagamento
import pessoa.PessoaService
import pessoa.escolherFuncionarioAtivo
import pessoa.identificarCliente
import servico.OrdemServico
import servico.OrdemServicoService
import utils.escolher
import utils.lerBigDecimal
import utils.lerInt
import utils.lerOpcao
import utils.lerSimNao
import utils.lerTexto
import java.sql.SQLException

private const val CAIXA = "Caixa d'água"
private const val ORDEM = "Ordem de serviço concluída"

fun menuVenda(
    vendas: VendaService,
    pessoas: PessoaService,
    caixas: CaixaDaAguaService,
    ordens: OrdemServicoService
) {
    do {
        println("0 - VOLTAR A OPERACAO")
        println("1 - NOVA VENDA")
        println("2 - LISTAR VENDAS")
        println("3 - DETALHE DA VENDA")
        println("4 - CANCELAR VENDA")

        val op = lerOpcao()

        try {
            when (op) {
                "0" -> {}
                "1" -> novaVenda(vendas, pessoas, caixas, ordens)
                "2" -> listar(vendas, pessoas)
                "3" -> detalhe(vendas, pessoas)
                "4" -> cancelar(vendas)
                else -> println("Opção inválida!")
            }
        } catch (ex: IllegalArgumentException) {
            println(" >> ${ex.message}")
        } catch (ex: IllegalStateException) {
            println(" >> ${ex.message}")
        } catch (ex: SQLException) {
            println(" >> Erro de banco: ${ex.message}")
        }
    } while (op != "0")
}

private fun novaVenda(
    vendas: VendaService,
    pessoas: PessoaService,
    caixas: CaixaDaAguaService,
    ordens: OrdemServicoService
) {
    println("=== NOVA VENDA ===")

    val clienteId = identificarCliente(pessoas) ?: return
    val funcionarioId = escolherFuncionarioAtivo(pessoas, "Vendedor:") ?: return

    val itens = montarItens(clienteId, caixas, ordens)
    if (itens.isEmpty()) {
        println("Venda sem itens. Nada foi gravado.")
        return
    }

    val condicao = escolher("Condição de pagamento:", CondicaoPagamento.entries) { it.toString() }
    val forma =
        if (condicao == CondicaoPagamento.A_VISTA)
            escolher("Forma de pagamento:", FormaPagamento.entries) { it.toString() }
        else null

    val pedido = Pedido(
        clienteId = clienteId,
        funcionarioId = funcionarioId,
        condicaoPagamento = condicao,
        formaPagamento = forma,
        observacao = lerTexto("Observação (Enter para pular):", obrigatorio = false).ifBlank { null },
        itens = itens
    )

    mostrarResumo(pedido)
    if (!lerSimNao("Confirmar a venda?")) {
        println("Venda abandonada. Nada foi gravado.")
        return
    }

    val id = vendas.vender(pedido)
    println("Venda $id gravada. Total R\$ ${pedido.valorTotal}")
    if (condicao == CondicaoPagamento.A_PRAZO) {
        println("A prazo: a venda nasce em aberto e aparece em contas a receber.")
    }
}

private fun montarItens(
    clienteId: Int,
    caixas: CaixaDaAguaService,
    ordens: OrdemServicoService
): List<VendaItem> {
    val itens = mutableListOf<VendaItem>()

    fun restante(caixa: CaixaDaAgua) =
        caixa.estoqueAtual - itens.filter { it.caixaDaAguaId == caixa.id }.sumOf { it.quantidade }

    do {
        val caixasDisponiveis = caixas.listarAtivas().filter { restante(it) > 0 }
        val ordensDisponiveis = ordens.aFaturarDoCliente(clienteId)
            .filter { ordem -> itens.none { it.ordemServicoId == ordem.id } }

        val tipo = when {
            caixasDisponiveis.isEmpty() && ordensDisponiveis.isEmpty() -> {
                println("Não há caixa com estoque nem ordem de serviço a cobrar deste cliente.")
                break
            }
            ordensDisponiveis.isEmpty() -> CAIXA
            caixasDisponiveis.isEmpty() -> ORDEM
            else -> escolher("Tipo de item:", listOf(CAIXA, ORDEM)) { it }
        }

        itens.add(
            if (tipo == CAIXA) itemDeCaixa(caixasDisponiveis) { restante(it) }
            else itemDeOrdem(ordensDisponiveis)
        )
        println("Item adicionado. Itens no pedido: ${itens.size}")
    } while (lerSimNao("Adicionar outro item?"))

    return itens
}

private fun itemDeCaixa(disponiveis: List<CaixaDaAgua>, restante: (CaixaDaAgua) -> Int): VendaItem {
    val caixa = escolher("Caixa:", disponiveis) {
        "${it.marca} ${it.modelo} ${it.capacidade}l - R\$ ${it.preco} (estoque ${restante(it)})"
    }

    val quantidade = lerInt("Quantidade:", min = 1, max = restante(caixa))
    val preco = lerBigDecimal(
        "Preço unitário (Enter para ${caixa.preco}):",
        padrao = caixa.preco
    )

    return VendaItem(
        caixaDaAguaId = caixa.id,
        descricao = "${caixa.marca} ${caixa.modelo} ${caixa.capacidade}l",
        quantidade = quantidade,
        precoUnitario = preco
    )
}

private fun itemDeOrdem(disponiveis: List<OrdemServico>): VendaItem {
    val ordem = escolher("Ordem de serviço:", disponiveis) {
        "Ordem ${it.id} - ${it.tipoServico} - concluída em ${it.dataConclusao?.toLocalDate()} - R\$ ${it.preco}"
    }

    val preco = lerBigDecimal(
        "Preço (Enter para ${ordem.preco}):",
        padrao = ordem.preco
    )

    return VendaItem(
        ordemServicoId = ordem.id,
        descricao = "${ordem.tipoServico} (ordem ${ordem.id})",
        quantidade = 1,
        precoUnitario = preco
    )
}

private fun mostrarResumo(pedido: Pedido) {
    println("=== RESUMO ===")
    pedido.itens.forEach { item ->
        println("${item.quantidade}x ${item.descricao} - R\$ ${item.precoUnitario} = R\$ ${item.subtotal}")
    }
    println("Condição: ${pedido.condicaoPagamento}")
    pedido.formaPagamento?.let { println("Forma: $it") }
    println("TOTAL: R\$ ${pedido.valorTotal}")
}

private fun listar(vendas: VendaService, pessoas: PessoaService) {
    println("=== VENDAS ===")

    val lista = vendas.listar()
    if (lista.isEmpty()) {
        println("Nenhuma venda registrada")
        return
    }

    lista.forEach { v ->
        println(
            "Id: ${v.id} | ${v.dataVenda.toLocalDate()} | ${pessoas.nomeDoCliente(v.clienteId)} | " +
                "R\$ ${v.valorTotal} | ${v.condicaoPagamento} | ${v.status}"
        )
    }
}

private fun detalhe(vendas: VendaService, pessoas: PessoaService) {
    println("=== DETALHE DA VENDA ===")

    val id = escolherId(vendas, "ver") ?: return

    val venda = vendas.buscarPorId(id)
    if (venda == null) {
        println("Venda não encontrada")
        return
    }

    println("====================================")
    println(
        """
        Id: ${venda.id}
        Data: ${venda.dataVenda}
        Cliente: ${pessoas.nomeDoCliente(venda.clienteId)}
        Vendedor: ${pessoas.nomeDoFuncionario(venda.funcionarioId)}
        Condição: ${venda.condicaoPagamento}
        Status: ${venda.status}
        Total: R${'$'} ${venda.valorTotal}
        Observação: ${venda.observacao ?: "-"}
        """.trimIndent()
    )

    println("--- itens ---")
    vendas.itensDe(id).forEach { item ->
        println("${item.quantidade}x ${item.descricao} - R\$ ${item.precoUnitario} = R\$ ${item.subtotal}")
    }

    println("--- movimentos ---")
    val movimentos = vendas.movimentosDe(id)
    if (movimentos.isEmpty()) println("Nenhum. A venda está em aberto.")
    else movimentos.forEach { m ->
        val marca = if (m.estornoDeId != null) " (estorno do ${m.estornoDeId})" else ""
        println("${m.dataMovimentacao.toLocalDate()} | ${m.tipo} | R\$ ${m.valor} | ${m.formaPagamento}$marca")
    }
    println("====================================")
}

private fun cancelar(vendas: VendaService) {
    println("=== CANCELAR VENDA ===")

    val id = escolherId(vendas, "cancelar") ?: return

    println("Cancelar devolve o estoque e estorna o que foi pago. A venda continua no histórico.")
    if (!lerSimNao("Confirmar o cancelamento da venda $id?")) {
        println("Nada foi alterado.")
        return
    }

    if (vendas.cancelar(id)) println("Venda $id cancelada")
    else println("Não foi possível cancelar a venda $id")
}

private fun escolherId(vendas: VendaService, acao: String): Int? {
    val ids = vendas.idsExistentes()
    if (ids.isEmpty()) {
        println("Nenhuma venda registrada")
        return null
    }

    println("Ids disponíveis: $ids")
    val id = lerInt("Id da venda que deseja $acao:")

    if (id !in ids) {
        println("Id inexistente")
        return null
    }
    return id
}
