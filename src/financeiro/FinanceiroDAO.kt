package financeiro

import java.math.BigDecimal
import java.sql.Connection
import java.sql.ResultSet

class FinanceiroDAO(private val conn: Connection) {

    fun listarContasAReceber(): List<ContaAReceber> {
        val contas = mutableListOf<ContaAReceber>()

        val sql = """
            SELECT venda_id, data_venda, cliente, telefone, valor_total, pago, saldo_devedor
            FROM vw_contas_receber
            ORDER BY data_venda
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    contas.add(mapear(rs))
                }
            }
        }
        return contas
    }

    fun saldoDevedorDaVenda(vendaId: Int): BigDecimal {
        val sql = """
            SELECT COALESCE(
                (SELECT saldo_devedor FROM vw_contas_receber WHERE venda_id = ?), 0
            ) AS devedor
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.setInt(1, vendaId)
            stmt.executeQuery().use { rs ->
                if (rs.next()) return rs.getBigDecimal("devedor")
            }
        }
        return BigDecimal.ZERO
    }

    private fun mapear(rs: ResultSet) = ContaAReceber(
        vendaId = rs.getInt("venda_id"),
        dataVenda = rs.getTimestamp("data_venda").toLocalDateTime(),
        cliente = rs.getString("cliente"),
        telefone = rs.getString("telefone"),
        valorTotal = rs.getBigDecimal("valor_total"),
        pago = rs.getBigDecimal("pago").setScale(2),
        saldoDevedor = rs.getBigDecimal("saldo_devedor")
    )
}
