package venda

import db.enumDe
import enums.CondicaoPagamento
import enums.StatusVenda
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Timestamp

private const val COLUNAS =
    "id, cliente_id, funcionario_id, data_venda, condicao_pagamento, " +
        "status, valor_total, observacao"

class VendaDAO(private val conn: Connection) {

    fun insert(venda: Venda): Int {

        val sql = """
            INSERT INTO venda(
            cliente_id, funcionario_id, data_venda, condicao_pagamento,
            status, valor_total, observacao)
            VALUES(?, ?, ?, ?, ?, ?, ?)
            RETURNING id
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.setInt(1, venda.clienteId)
            stmt.setInt(2, venda.funcionarioId)
            stmt.setTimestamp(3, Timestamp.valueOf(venda.dataVenda))
            stmt.setString(4, venda.condicaoPagamento.name)
            stmt.setString(5, venda.status.name)
            stmt.setBigDecimal(6, venda.valorTotal)
            stmt.setString(7, venda.observacao)

            stmt.executeQuery().use { rs ->
                if (rs.next()) return rs.getInt("id")
            }
        }
        throw SQLException("INSERT nao retornou id")
    }

    fun alterarStatus(id: Int, status: StatusVenda): Boolean {
        val sql = """
            UPDATE venda SET
            status = ?
            WHERE id = ?
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, status.name)
            stmt.setInt(2, id)

            return stmt.executeUpdate() > 0
        }
    }

    fun listar(): List<Venda> {
        val vendas = mutableListOf<Venda>()

        val sql = """
            SELECT $COLUNAS
            FROM venda
            ORDER BY id DESC
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    vendas.add(mapear(rs))
                }
            }
        }
        return vendas
    }

    fun buscarPorId(id: Int): Venda? {
        val sql = """
            SELECT $COLUNAS
            FROM venda
            WHERE id = ?
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.setInt(1, id)
            stmt.executeQuery().use { rs ->
                if (rs.next()) return mapear(rs)
            }
        }
        return null
    }

    fun listarIdsVendas(): List<Int> {
        val ids = mutableListOf<Int>()

        val sql = """
            SELECT id
            FROM venda
            ORDER BY id
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    ids.add(rs.getInt("id"))
                }
            }
        }
        return ids
    }

    private fun mapear(rs: ResultSet) = Venda(
        id = rs.getInt("id"),
        clienteId = rs.getInt("cliente_id"),
        funcionarioId = rs.getInt("funcionario_id"),
        condicaoPagamento = rs.enumDe<CondicaoPagamento>("condicao_pagamento"),
        valorTotal = rs.getBigDecimal("valor_total"),
        status = rs.enumDe<StatusVenda>("status"),
        observacao = rs.getString("observacao"),
        dataVenda = rs.getTimestamp("data_venda").toLocalDateTime()
    )
}
