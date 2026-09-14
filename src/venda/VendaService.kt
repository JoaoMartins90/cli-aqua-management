package venda

import caixadaagua.CaixaDaAguaService
import cliente.ClienteService
import db.emTransacao
import enums.CondicaoPagamento
import enums.StatusVenda
import enums.TipoMovimento
import financeiro.ContaService
import funcionario.FuncionarioService
import movimentacao.Movimento
import movimentacao.MovimentoService
import servico.OrdemServicoService
import java.math.BigDecimal
import java.sql.Connection

class VendaService(
    private val conn: Connection,
    private val dao: VendaDAO,
    private val itens: VendaItemDAO,
    private val caixas: CaixaDaAguaService,
    private val ordens: OrdemServicoService,
    private val movimentos: MovimentoService,
    private val contas: ContaService,
    private val clientes: ClienteService,
    private val funcionarios: FuncionarioService
) {

    fun vender(pedido: Pedido): Int =
        conn.emTransacao {
            validar(pedido)

            val vendaId = dao.insert(
                Venda(
                    clienteId = pedido.clienteId,
                    funcionarioId = pedido.funcionarioId,
                    condicaoPagamento = pedido.condicaoPagamento,
                    valorTotal = pedido.valorTotal,
                    observacao = pedido.observacao
                )
            )

            pedido.itens.forEach { item ->
                itens.insert(item.copy(vendaId = vendaId))
                item.caixaDaAguaId?.let { caixas.baixarEstoque(it, item.quantidade) }
            }

            if (pedido.condicaoPagamento == CondicaoPagamento.A_VISTA) {
                registrarEntrada(vendaId, pedido)
            }

            vendaId
        }

    fun cancelar(vendaId: Int): Boolean =
        conn.emTransacao {
            val venda = dao.buscarPorId(vendaId)
                ?: throw IllegalArgumentException("Nao existe venda com o id $vendaId")
            require(venda.status == StatusVenda.EFETIVADA) {
                "A venda $vendaId ja esta ${venda.status}"
            }

            itens.listarPorVenda(vendaId).forEach { item ->
                item.caixaDaAguaId?.let { caixas.devolverEstoque(it, item.quantidade) }
            }


            val lancamentos = movimentos.listarPorVenda(vendaId)
            val jaEstornados = lancamentos.mapNotNull { it.estornoDeId }.toSet()
            lancamentos
                .filter { it.estornoDeId == null && it.id !in jaEstornados }
                .forEach { movimentos.estornar(it.id!!, "Estorno da venda $vendaId") }

            dao.alterarStatus(vendaId, StatusVenda.CANCELADA)
        }

    fun listar(): List<Venda> =
        dao.listar()

    fun buscarPorId(id: Int): Venda? =
        dao.buscarPorId(id)

    fun itensDe(vendaId: Int): List<VendaItem> =
        itens.listarPorVenda(vendaId)

    fun movimentosDe(vendaId: Int): List<Movimento> =
        movimentos.listarPorVenda(vendaId)

    fun idsExistentes(): List<Int> =
        dao.listarIdsVendas()

    private fun registrarEntrada(vendaId: Int, pedido: Pedido) {
        val conta = contas.daLoja()
        val cliente = clientes.buscarPorId(pedido.clienteId)

        movimentos.registrar(
            Movimento(
                contaId = conta.id!!,
                tipo = TipoMovimento.ENTRADA,
                valor = pedido.valorTotal,
                formaPagamento = pedido.formaPagamento!!,
                vendaId = vendaId,
                pessoaId = cliente?.pessoaId,
                descricao = "Venda $vendaId a vista"
            )
        )
    }

    private fun validar(pedido: Pedido) {
        require(pedido.itens.isNotEmpty()) { "A venda precisa de ao menos um item" }
        val cliente = clientes.buscarPorId(pedido.clienteId)
        require(cliente != null) {
            "Nao existe cliente com o id ${pedido.clienteId}"
        }

        val funcionario = funcionarios.buscarPorId(pedido.funcionarioId)
        require(funcionario != null) {
            "Nao existe funcionario com o id ${pedido.funcionarioId}"
        }
        require(funcionario.ativo) { "Funcionario demitido nao pode assinar venda" }

        if (pedido.condicaoPagamento == CondicaoPagamento.A_VISTA) {
            require(pedido.formaPagamento != null) {
                "Venda a vista precisa de forma de pagamento"
            }
        }

        if (pedido.condicaoPagamento == CondicaoPagamento.A_PRAZO) {
            val devedor = dao.saldoDevedorDoCliente(cliente.id!!)
            val disponivel = cliente.limiteCredito - devedor
            require(pedido.valorTotal <= disponivel) {
                "Limite de credito insuficiente: limite R\$ ${cliente.limiteCredito}, " +
                    "ja deve R\$ $devedor, disponivel R\$ $disponivel, venda de R\$ ${pedido.valorTotal}"
            }
        }

        pedido.itens.forEach { item ->
            require((item.caixaDaAguaId == null) != (item.ordemServicoId == null)) {
                "Item da venda tem que ser uma caixa OU uma ordem de servico"
            }
            require(item.quantidade > 0) { "Quantidade do item deve ser maior que zero" }
            require(item.precoUnitario >= BigDecimal.ZERO) {
                "Preco do item nao pode ser negativo"
            }
            require(item.precoUnitario.stripTrailingZeros().scale() <= 2) {
                "Preco do item nao pode ter mais de 2 casas decimais"
            }
            require(item.descricao.isNotBlank()) { "Item da venda precisa de descricao" }

            item.caixaDaAguaId?.let { caixaId ->
                val caixa = caixas.buscarPorId(caixaId)
                require(caixa != null) { "Nao existe caixa com o id $caixaId" }
                require(caixa.ativo) {
                    "${caixa.marca} ${caixa.modelo} saiu do catalogo e nao pode ser vendida"
                }
            }

            item.ordemServicoId?.let { ordemId ->
                require(item.quantidade == 1) { "Ordem de servico entra na venda com quantidade 1" }
                ordens.exigirFaturavel(ordemId, pedido.clienteId)
            }
        }

        val ordensNoPedido = pedido.itens.mapNotNull { it.ordemServicoId }
        require(ordensNoPedido.size == ordensNoPedido.toSet().size) {
            "A mesma ordem de servico aparece mais de uma vez na venda"
        }
    }
}
