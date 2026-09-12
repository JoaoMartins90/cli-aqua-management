package venda

import caixadaagua.CaixaDaAguaService
import enums.CondicaoPagamento
import enums.FormaPagamento
import pessoa.Pessoa
import pessoa.PessoaService
import utils.escolher
import utils.lerBigDecimal
import utils.lerDigitos
import utils.lerInt
import utils.lerSimNao
import utils.lerTexto
import java.sql.SQLException

fun menuVenda(
    vendas: VendaService,
    pessoas: PessoaService,
    caixas: CaixaDaAguaService
) {
    do {
        println("0 - VOLTAR AO MENU PRINCIPAL")
        println("1 - NOVA VENDA")
        println("2 - LISTAR VENDAS")
        println("3 - DETALHE DA VENDA")
        println("4 - CANCELAR VENDA")

        val op = readln()

        try {
            when (op) {
                "0" -> {}
                "1" -> novaVenda(vendas, pessoas, caixas)
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
    caixas: CaixaDaAguaService
) {
    println("=== NOVA VENDA ===")

    val clienteId = identificarCliente(pessoas) ?: return
    val funcionarioId = identificarVendedor(pessoas) ?: return

    val itens = montarItens(caixas)
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

private fun identificarCliente(pessoas: PessoaService): Int? {
    val cpfCnpj = lerDigitos("CPF ou CNPJ do cliente:", listOf(11, 14))

    val pessoa = pessoas.buscarPorCpfCnpj(cpfCnpj) ?: cadastrarNaHora(pessoas, cpfCnpj) ?: return null

    val cliente = pessoas.clienteDe(pessoa.id!!)
    if (cliente != null) {
        println("Cliente: ${pessoa.nome}")
        return cliente.id
    }

    println("${pessoa.nome} está cadastrado, mas ainda não é cliente.")
    if (!lerSimNao("Tornar cliente agora?")) return null
    return pessoas.tornarCliente(pessoa.id, lerBigDecimal("Limite de crédito:"))
}

private fun cadastrarNaHora(pessoas: PessoaService, cpfCnpj: String): Pessoa? {
    println("Documento não cadastrado.")
    if (!lerSimNao("Cadastrar a pessoa agora?")) return null

    val id = pessoas.cadastrar(
        Pessoa(
            nome = lerTexto("Nome:", minimo = 3),
            cpfCnpj = cpfCnpj,
            telefone = lerDigitos("Telefone com DDD:", listOf(10, 11))
        )
    )
    return pessoas.buscarPorId(id)
}

private fun identificarVendedor(pessoas: PessoaService): Int? {
    val ativos = pessoas.funcionariosAtivos()
    if (ativos.isEmpty()) {
        println("Nenhum funcionário ativo. Cadastre um antes de vender.")
        return null
    }

    val vendedor = escolher("Vendedor:", ativos) {
        "${pessoas.nomeDe(it.pessoaId)} - ${it.setor}"
    }
    return vendedor.id
}

private fun montarItens(caixas: CaixaDaAguaService): List<VendaItem> {
    val itens = mutableListOf<VendaItem>()

    do {
        val disponiveis = caixas.listarAtivas().filter { it.estoqueAtual > 0 }
        if (disponiveis.isEmpty()) {
            println("Nenhuma caixa ativa com estoque.")
            break
        }

        val caixa = escolher("Caixa:", disponiveis) {
            "${it.marca} ${it.modelo} ${it.capacidade}l - R\$ ${it.preco} (estoque ${it.estoqueAtual})"
        }

        val quantidade = lerInt("Quantidade:", min = 1, max = caixa.estoqueAtual)
        val preco = lerBigDecimal(
            "Preço unitário (Enter para ${caixa.preco}):",
            padrao = caixa.preco
        )

        itens.add(
            VendaItem(
                caixaDaAguaId = caixa.id,
                descricao = "${caixa.marca} ${caixa.modelo} ${caixa.capacidade}l",
                quantidade = quantidade,
                precoUnitario = preco
            )
        )
        println("Item adicionado. Itens no pedido: ${itens.size}")
    } while (lerSimNao("Adicionar outro item?"))

    return itens
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
