package movimentacao

import db.enumDe
import db.intOuNulo
import enums.FormaPagamento
import enums.TipoMovimento
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Timestamp
import java.sql.Types

private const val COLUNAS =
    "id, conta_id, tipo, valor, forma_pagamento, venda_id, pessoa_id, " +
        "estorno_de_id, descricao, data_movimentacao"

class MovimentoDAO(private val conn: Connection) {

    fun insert(movimento: Movimento): Int {

        val sql = """
            INSERT INTO movimento(
            conta_id, tipo, valor, forma_pagamento, venda_id, pessoa_id,
            estorno_de_id, descricao, data_movimentacao)
            VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.setInt(1, movimento.contaId)
            stmt.setString(2, movimento.tipo.name)
            stmt.setBigDecimal(3, movimento.valor)
            stmt.setString(4, movimento.formaPagamento.name)
            stmt.setObject(5, movimento.vendaId, Types.INTEGER)
            stmt.setObject(6, movimento.pessoaId, Types.INTEGER)
            stmt.setObject(7, movimento.estornoDeId, Types.INTEGER)
            stmt.setString(8, movimento.descricao)
            stmt.setTimestamp(9, Timestamp.valueOf(movimento.dataMovimentacao))

            stmt.executeQuery().use { rs ->
                if (rs.next()) return rs.getInt("id")
            }
        }
        throw SQLException("INSERT nao retornou id")
    }

    fun listar(): List<Movimento> {
        val movimentos = mutableListOf<Movimento>()

        val sql = """
            SELECT $COLUNAS
            FROM movimento
            ORDER BY id
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    movimentos.add(mapear(rs))
                }
            }
        }
        return movimentos
    }

    fun listarPorVenda(vendaId: Int): List<Movimento> {
        val movimentos = mutableListOf<Movimento>()

        val sql = """
            SELECT $COLUNAS
            FROM movimento
            WHERE venda_id = ?
            ORDER BY id
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.setInt(1, vendaId)
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    movimentos.add(mapear(rs))
                }
            }
        }
        return movimentos
    }

    fun buscarPorId(id: Int): Movimento? {
        val sql = """
            SELECT $COLUNAS
            FROM movimento
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

    private fun mapear(rs: ResultSet) = Movimento(
        id = rs.getInt("id"),
        contaId = rs.getInt("conta_id"),
        tipo = rs.enumDe<TipoMovimento>("tipo"),
        valor = rs.getBigDecimal("valor"),
        formaPagamento = rs.enumDe<FormaPagamento>("forma_pagamento"),
        vendaId = rs.intOuNulo("venda_id"),
        pessoaId = rs.intOuNulo("pessoa_id"),
        estornoDeId = rs.intOuNulo("estorno_de_id"),
        descricao = rs.getString("descricao"),
        dataMovimentacao = rs.getTimestamp("data_movimentacao").toLocalDateTime()
    )
}
