package financeiro

import cliente.ClienteService
import db.emTransacao
import enums.FormaPagamento
import enums.StatusVenda
import enums.TipoMovimento
import funcionario.FuncionarioService
import movimentacao.Movimento
import movimentacao.MovimentoService
import venda.VendaService
import java.math.BigDecimal
import java.sql.Connection
import java.time.YearMonth

class FinanceiroService(
    private val conn: Connection,
    private val dao: FinanceiroDAO,
    private val contas: ContaService,
    private val movimentos: MovimentoService,
    private val vendas: VendaService,
    private val clientes: ClienteService,
    private val funcionarios: FuncionarioService
) {
    fun receber(vendaId: Int, valor: BigDecimal, forma: FormaPagamento): Int =
        conn.emTransacao {
            val venda = vendas.buscarPorId(vendaId)
                ?: throw IllegalArgumentException("Nao existe venda com o id $vendaId")
            require(venda.status == StatusVenda.EFETIVADA) {
                "A venda $vendaId esta ${venda.status}: nao recebe pagamento"
            }

            val devedor = dao.saldoDevedorDaVenda(vendaId)
            require(devedor > BigDecimal.ZERO) { "A venda $vendaId ja esta quitada" }
            require(valor <= devedor) {
                "Valor de R\$ $valor passa do saldo devedor da venda $vendaId (R\$ $devedor)"
            }

            movimentos.registrar(
                Movimento(
                    contaId = contas.daLoja().id!!,
                    tipo = TipoMovimento.ENTRADA,
                    valor = valor,
                    formaPagamento = forma,
                    vendaId = vendaId,
                    pessoaId = clientes.buscarPorId(venda.clienteId)?.pessoaId,
                    descricao = "Recebimento da venda $vendaId"
                )
            )
        }

    fun pagarSalario(
        funcionarioId: Int,
        valor: BigDecimal,
        forma: FormaPagamento,
        competencia: YearMonth
    ): Int {
        val funcionario = funcionarios.buscarPorId(funcionarioId)
        require(funcionario != null) { "Nao existe funcionario com o id $funcionarioId" }

        return movimentos.registrar(
            Movimento(
                contaId = contas.daLoja().id!!,
                tipo = TipoMovimento.SAIDA,
                valor = valor,
                formaPagamento = forma,
                pessoaId = funcionario.pessoaId,
                descricao = "Salario %02d/%d".format(competencia.monthValue, competencia.year)
            )
        )
    }

    fun estornar(movimentoId: Int): Int =
        movimentos.estornar(movimentoId, "Estorno do movimento $movimentoId")

    fun contasAReceber(): List<ContaAReceber> =
        dao.listarContasAReceber()

    fun contaDaLoja(): Conta =
        contas.daLoja()

    fun extratoDaLoja(): List<Movimento> =
        movimentos.listarPorConta(contas.daLoja().id!!)

    fun movimentosEstornaveis(): List<Movimento> {
        val extrato = extratoDaLoja()
        val estornados = extrato.mapNotNull { it.estornoDeId }.toSet()
        return extrato.filter { it.estornoDeId == null && it.id !in estornados }
    }

    fun salarioDe(funcionarioId: Int): BigDecimal =
        funcionarios.buscarPorId(funcionarioId)?.salario
            ?: throw IllegalArgumentException("Nao existe funcionario com o id $funcionarioId")
}
