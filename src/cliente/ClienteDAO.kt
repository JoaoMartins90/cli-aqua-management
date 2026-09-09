package cliente

import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Timestamp

private const val COLUNAS = "id, pessoa_id, limite_credito, criado_em"

class ClienteDAO(private val conn: Connection) {

    fun insert(cliente: Cliente): Int {

        val sql = """
            INSERT INTO cliente(
            pessoa_id, limite_credito, criado_em)
            VALUES(?, ?, ?)
            RETURNING id
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.setInt(1, cliente.pessoaId)
            stmt.setBigDecimal(2, cliente.limiteCredito)
            stmt.setTimestamp(3, Timestamp.valueOf(cliente.criadoEm))

            stmt.executeQuery().use { rs ->
                if (rs.next()) return rs.getInt("id")
            }
        }
        throw SQLException("INSERT nao retornou id")
    }

    fun listar(): List<Cliente> {
        val clientes = mutableListOf<Cliente>()

        val sql = """
            SELECT $COLUNAS
            FROM cliente
            ORDER BY id
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    clientes.add(mapear(rs))
                }
            }
        }
        return clientes
    }

    fun remover(id: Int): Boolean {
        val sql = """
            DELETE FROM cliente
            WHERE id = ?
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.setInt(1, id)
            return stmt.executeUpdate() > 0
        }
    }

    fun alterar(cliente: Cliente): Boolean {
        val id = cliente.id ?: return false

        val sql = """
            UPDATE cliente SET
            limite_credito = ?
            WHERE id = ?
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.setBigDecimal(1, cliente.limiteCredito)
            stmt.setInt(2, id)

            return stmt.executeUpdate() > 0
        }
    }

    fun buscarPorId(id: Int): Cliente? {
        val sql = """
            SELECT $COLUNAS
            FROM cliente
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

    fun buscarPorPessoaId(pessoaId: Int): Cliente? {
        val sql = """
            SELECT $COLUNAS
            FROM cliente
            WHERE pessoa_id = ?
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.setInt(1, pessoaId)
            stmt.executeQuery().use { rs ->
                if (rs.next()) return mapear(rs)
            }
        }
        return null
    }

    fun listarIdsClientes(): List<Int> {
        val ids = mutableListOf<Int>()

        val sql = """
            SELECT id
            FROM cliente
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

    private fun mapear(rs: ResultSet) = Cliente(
        id = rs.getInt("id"),
        pessoaId = rs.getInt("pessoa_id"),
        limiteCredito = rs.getBigDecimal("limite_credito"),
        criadoEm = rs.getTimestamp("criado_em").toLocalDateTime()
    )
}
