package movimentacao

import db.emTransacao
import financeiro.ContaService
import utils.temAteDuasCasas
import java.math.BigDecimal
import java.sql.Connection
import java.time.LocalDateTime

class MovimentoService(
    private val conn: Connection,
    private val dao: MovimentoDAO,
    private val contas: ContaService
) {
    fun registrar(movimento: Movimento): Int =
        conn.emTransacao {
            validar(movimento)
            val id = dao.insert(movimento)
            contas.movimentarSaldo(movimento.contaId, movimento.efeitoNoSaldo)
            id
        }

    fun estornar(movimentoId: Int, descricao: String): Int =
        conn.emTransacao {
            val original = dao.buscarPorId(movimentoId)
                ?: throw IllegalArgumentException("Nao existe movimento com o id $movimentoId")
            require(original.estornoDeId == null) {
                "O movimento $movimentoId ja e um estorno"
            }
            require(dao.buscarEstornoDe(movimentoId) == null) {
                "O movimento $movimentoId ja foi estornado"
            }

            registrar(
                original.copy(
                    id = null,
                    tipo = original.tipo.inverso,
                    estornoDeId = original.id,
                    descricao = descricao,
                    dataMovimentacao = LocalDateTime.now()
                )
            )
        }

    fun listar(): List<Movimento> =
        dao.listar()

    fun listarPorVenda(vendaId: Int): List<Movimento> =
        dao.listarPorVenda(vendaId)

    fun listarPorConta(contaId: Int): List<Movimento> =
        dao.listarPorConta(contaId)

    fun buscarPorId(id: Int): Movimento? =
        dao.buscarPorId(id)

    private fun validar(m: Movimento) {
        require(m.contaId > 0) { "Movimento precisa apontar para uma conta" }
        require(m.valor > BigDecimal.ZERO) { "Valor do movimento deve ser maior que zero" }
        require(m.valor.temAteDuasCasas()) { "Valor do movimento nao pode ter mais de 2 casas decimais" }
        require(m.descricao.isNotBlank()) { "Movimento precisa de uma descricao" }
    }
}
