package funcionario

import enums.Setor
import java.sql.Connection
import java.sql.Date
import java.sql.ResultSet
import java.sql.SQLException
import java.time.LocalDate

private const val COLUNAS = "id, pessoa_id, setor, salario, data_admissao, data_demissao"

class FuncionarioDAO(private val conn: Connection) {

    fun insert(funcionario: Funcionario): Int {

        val sql = """
            INSERT INTO funcionario(
            pessoa_id, setor, salario, data_admissao, data_demissao)
            VALUES(?, ?, ?, ?, ?)
            RETURNING id
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.setInt(1, funcionario.pessoaId)
            stmt.setString(2, funcionario.setor.name)
            stmt.setBigDecimal(3, funcionario.salario)
            stmt.setDate(4, Date.valueOf(funcionario.dataAdmissao))
            stmt.setDate(5, funcionario.dataDemissao?.let { Date.valueOf(it) })

            stmt.executeQuery().use { rs ->
                if (rs.next()) return rs.getInt("id")
            }
        }
        throw SQLException("INSERT nao retornou id")
    }

    fun listar(): List<Funcionario> {
        val sql = """
            SELECT $COLUNAS
            FROM funcionario
            ORDER BY id
        """.trimIndent()

        return consultar(sql)
    }

    fun listarAtivos(): List<Funcionario> {
        val sql = """
            SELECT $COLUNAS
            FROM funcionario
            WHERE data_demissao IS NULL
            ORDER BY id
        """.trimIndent()

        return consultar(sql)
    }

    fun alterar(funcionario: Funcionario): Boolean {
        val id = funcionario.id ?: return false

        val sql = """
            UPDATE funcionario SET
            setor = ?, salario = ?, data_admissao = ?, data_demissao = ?
            WHERE id = ?
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, funcionario.setor.name)
            stmt.setBigDecimal(2, funcionario.salario)
            stmt.setDate(3, Date.valueOf(funcionario.dataAdmissao))
            stmt.setDate(4, funcionario.dataDemissao?.let { Date.valueOf(it) })
            stmt.setInt(5, id)

            return stmt.executeUpdate() > 0
        }
    }

    fun demitir(id: Int, dataDemissao: LocalDate): Boolean {
        val sql = """
            UPDATE funcionario SET
            data_demissao = ?
            WHERE id = ? AND data_demissao IS NULL
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.setDate(1, Date.valueOf(dataDemissao))
            stmt.setInt(2, id)

            return stmt.executeUpdate() > 0
        }
    }

    fun buscarPorId(id: Int): Funcionario? {
        val sql = """
            SELECT $COLUNAS
            FROM funcionario
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

    fun buscarPorPessoaId(pessoaId: Int): Funcionario? {
        val sql = """
            SELECT $COLUNAS
            FROM funcionario
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

    fun listarIdsFuncionarios(): List<Int> {
        val ids = mutableListOf<Int>()

        val sql = """
            SELECT id
            FROM funcionario
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

    private fun consultar(sql: String): List<Funcionario> {
        val funcionarios = mutableListOf<Funcionario>()

        conn.prepareStatement(sql).use { stmt ->
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    funcionarios.add(mapear(rs))
                }
            }
        }
        return funcionarios
    }

    private fun mapear(rs: ResultSet) = Funcionario(
        id = rs.getInt("id"),
        pessoaId = rs.getInt("pessoa_id"),
        setor = Setor.valueOf(rs.getString("setor")),
        salario = rs.getBigDecimal("salario"),
        dataAdmissao = rs.getDate("data_admissao").toLocalDate(),
        dataDemissao = rs.getDate("data_demissao")?.toLocalDate()
    )
}
