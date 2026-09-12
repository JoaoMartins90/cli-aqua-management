package financeiro

import java.math.BigDecimal
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException

private const val COLUNAS = "id, pessoa_id, descricao, saldo"

class ContaDAO(private val conn: Connection) {

    fun insert(conta: Conta): Int {

        val sql = """
            INSERT INTO conta(
            pessoa_id, descricao, saldo)
            VALUES(?, ?, ?)
            RETURNING id
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.setInt(1, conta.pessoaId)
            stmt.setString(2, conta.descricao)
            stmt.setBigDecimal(3, conta.saldo)

            stmt.executeQuery().use { rs ->
                if (rs.next()) return rs.getInt("id")
            }
        }
        throw SQLException("INSERT nao retornou id")
    }

    fun listar(): List<Conta> {
        val contas = mutableListOf<Conta>()

        val sql = """
            SELECT $COLUNAS
            FROM conta
            ORDER BY id
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

    fun remover(id: Int): Boolean {
        val sql = """
            DELETE FROM conta
            WHERE id = ?
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.setInt(1, id)
            return stmt.executeUpdate() > 0
        }
    }

    fun alterar(conta: Conta): Boolean {
        val id = conta.id ?: return false

        val sql = """
            UPDATE conta SET
            descricao = ?, saldo = ?
            WHERE id = ?
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, conta.descricao)
            stmt.setBigDecimal(2, conta.saldo)
            stmt.setInt(3, id)

            return stmt.executeUpdate() > 0
        }
    }

    fun atualizarSaldo(id: Int, delta: BigDecimal): Boolean {
        val sql = """
            UPDATE conta SET
            saldo = saldo + ?
            WHERE id = ?
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.setBigDecimal(1, delta)
            stmt.setInt(2, id)

            return stmt.executeUpdate() > 0
        }
    }

    fun buscarPorId(id: Int): Conta? {
        val sql = """
            SELECT $COLUNAS
            FROM conta
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

    fun buscarPorPessoaId(pessoaId: Int): Conta? {
        val sql = """
            SELECT $COLUNAS
            FROM conta
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

    fun buscarPorCpfCnpj(cpfCnpj: String): Conta? {
        val sql = """
            SELECT c.id, c.pessoa_id, c.descricao, c.saldo
            FROM conta c
            JOIN pessoa p ON p.id = c.pessoa_id
            WHERE p.cpf_cnpj = ?
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, cpfCnpj)
            stmt.executeQuery().use { rs ->
                if (rs.next()) return mapear(rs)
            }
        }
        return null
    }

    fun listarIdsContas(): List<Int> {
        val ids = mutableListOf<Int>()

        val sql = """
            SELECT id
            FROM conta
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

    private fun mapear(rs: ResultSet) = Conta(
        id = rs.getInt("id"),
        pessoaId = rs.getInt("pessoa_id"),
        descricao = rs.getString("descricao"),
        saldo = rs.getBigDecimal("saldo")
    )
}
