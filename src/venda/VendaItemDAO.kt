package venda

import db.intOuNulo
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Types

private const val COLUNAS =
    "id, venda_id, caixa_da_agua_id, ordem_servico_id, descricao, " +
        "quantidade, preco_unitario"

class VendaItemDAO(private val conn: Connection) {

    fun insert(item: VendaItem): Int {
        val vendaId = item.vendaId
            ?: throw IllegalArgumentException("Item precisa saber de qual venda e")

        val sql = """
            INSERT INTO venda_item(
            venda_id, caixa_da_agua_id, ordem_servico_id, descricao,
            quantidade, preco_unitario)
            VALUES(?, ?, ?, ?, ?, ?)
            RETURNING id
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.setInt(1, vendaId)
            stmt.setObject(2, item.caixaDaAguaId, Types.INTEGER)
            stmt.setObject(3, item.ordemServicoId, Types.INTEGER)
            stmt.setString(4, item.descricao)
            stmt.setInt(5, item.quantidade)
            stmt.setBigDecimal(6, item.precoUnitario)

            stmt.executeQuery().use { rs ->
                if (rs.next()) return rs.getInt("id")
            }
        }
        throw SQLException("INSERT nao retornou id")
    }

    fun listarPorVenda(vendaId: Int): List<VendaItem> {
        val itens = mutableListOf<VendaItem>()

        val sql = """
            SELECT $COLUNAS
            FROM venda_item
            WHERE venda_id = ?
            ORDER BY id
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.setInt(1, vendaId)
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    itens.add(mapear(rs))
                }
            }
        }
        return itens
    }

    private fun mapear(rs: ResultSet) = VendaItem(
        id = rs.getInt("id"),
        vendaId = rs.getInt("venda_id"),
        caixaDaAguaId = rs.intOuNulo("caixa_da_agua_id"),
        ordemServicoId = rs.intOuNulo("ordem_servico_id"),
        descricao = rs.getString("descricao"),
        quantidade = rs.getInt("quantidade"),
        precoUnitario = rs.getBigDecimal("preco_unitario")
    )
}
