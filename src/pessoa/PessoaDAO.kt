package pessoa

import java.sql.Connection
import java.sql.SQLException
import java.sql.Timestamp

class PessoaDAO(private val conn: Connection) {

    fun insert(pessoa: Pessoa): Int {

        val sql = """
            INSERT INTO pessoa(
            nome, cpf_cnpj, telefone, criado_em)
            VALUES(?, ?, ?, ?)
            RETURNING id
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt->
            stmt.setString(1, pessoa.nome)
            stmt.setString(2, pessoa.cpfCnpj)
            stmt.setString(3, pessoa.telefone)
            stmt.setTimestamp(4, Timestamp.valueOf(pessoa.criadoEm))

            stmt.executeQuery().use { rs ->
                if (rs.next()) return rs.getInt("id")
            }
        }
        throw SQLException("INSERT não retornou id")
    }

    fun listar(): List<Pessoa> {
        val pessoas = mutableListOf<Pessoa>()

        val sql = """
            SELECT id, nome, cpf_cnpj, telefone, criado_em
            FROM pessoa
            ORDER BY id
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt->
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    pessoas.add(Pessoa(
                        id = rs.getInt("id"),
                        nome = rs.getString("nome"),
                        cpfCnpj = rs.getString("cpf_cnpj"),
                        telefone = rs.getString("telefone"),
                        criadoEm = rs.getTimestamp("criadoEm").toLocalDateTime()
                    ))
                }
            }
        }
        return pessoas
    }

    fun remover(id: Int): Boolean {
        val sql = """
            DELETE FROM pessoa
            WHERE id = ?
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt->
            stmt.setInt(1, id)
            return stmt.executeUpdate() > 0
        }
    }

    fun alterar(pessoa: Pessoa): Boolean {
        val id = pessoa.id ?: return false

        val sql = """
            UPDATE pessoa SET
            nome = ?, cpf_cnpj = ?, telefone = ?
            WHERE id = ?
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt->
            stmt.setString(1, pessoa.nome)
            stmt.setString(2, pessoa.cpfCnpj)
            stmt.setString(3, pessoa.telefone)
            stmt.setInt(4, id)

            return stmt.executeUpdate() > 0
        }
    }

    fun buscarPorId(id: Int): Pessoa? {
        val sql = """
            SELECT id, nome, cpf_cnpj, telefone, criado_em
            FROM pessoa
            WHERE id = ?
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt->
            stmt.setInt(1, id)
            stmt.executeQuery().use { rs ->
                if (rs.next()) return Pessoa(
                    id = rs.getInt("id"),
                    nome = rs.getString("nome"),
                    cpfCnpj = rs.getString("cpf_cnpj"),
                    telefone = rs.getString("telefone"),
                    criadoEm = rs.getTimestamp("criado_em").toLocalDateTime()
                )
            }
        }
        return null
    }

    fun listarIdsPessoa(): List<Int> {
        val ids = mutableListOf<Int>()

        val sql = """
            SELECT id
            FROM pessoa
            ORDER BY id
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt->
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    ids.add(rs.getInt("id"))
                }
            }
        }
        return ids
    }
}