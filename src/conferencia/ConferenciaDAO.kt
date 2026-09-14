package conferencia

import java.sql.Connection
import java.sql.ResultSet

class ConferenciaDAO(private val conn: Connection) {

    fun listarVendasDivergentes(): List<DivergenciaVenda> {
        val divergencias = mutableListOf<DivergenciaVenda>()

        val sql = """
            SELECT venda_id, valor_total, soma_itens
            FROM vw_conferencia_venda
            ORDER BY venda_id
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    divergencias.add(mapearVenda(rs))
                }
            }
        }
        return divergencias
    }

    fun listarSaldosDivergentes(): List<DivergenciaSaldo> {
        val divergencias = mutableListOf<DivergenciaSaldo>()

        val sql = """
            SELECT conta_id, descricao, saldo_gravado, saldo_calculado
            FROM vw_conferencia_saldo
            ORDER BY conta_id
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    divergencias.add(mapearSaldo(rs))
                }
            }
        }
        return divergencias
    }

    private fun mapearVenda(rs: ResultSet) = DivergenciaVenda(
        vendaId = rs.getInt("venda_id"),
        valorTotal = rs.getBigDecimal("valor_total"),
        somaItens = rs.getBigDecimal("soma_itens")
    )

    private fun mapearSaldo(rs: ResultSet) = DivergenciaSaldo(
        contaId = rs.getInt("conta_id"),
        descricao = rs.getString("descricao"),
        saldoGravado = rs.getBigDecimal("saldo_gravado"),
        saldoCalculado = rs.getBigDecimal("saldo_calculado")
    )
}
