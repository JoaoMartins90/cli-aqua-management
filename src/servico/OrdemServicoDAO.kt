package servico

import db.enumDe
import db.intOuNulo
import enums.StatusOrdemServico
import enums.TipoServico
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Timestamp
import java.sql.Types

private const val COLUNAS =
    "id, tipo_servico, cliente_id, funcionario_id, status, preco, " +
        "data_agendada, data_conclusao, observacao, criado_em"

class OrdemServicoDAO(private val conn: Connection) {

    fun insert(ordem: OrdemServico): Int {

        val sql = """
            INSERT INTO ordem_servico(
            tipo_servico, cliente_id, funcionario_id, status, preco,
            data_agendada, data_conclusao, observacao, criado_em)
            VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, ordem.tipoServico.name)
            stmt.setInt(2, ordem.clienteId)
            stmt.setObject(3, ordem.funcionarioId, Types.INTEGER)
            stmt.setString(4, ordem.status.name)
            stmt.setBigDecimal(5, ordem.preco)
            stmt.setTimestamp(6, Timestamp.valueOf(ordem.dataAgendada))
            stmt.setTimestamp(7, ordem.dataConclusao?.let { Timestamp.valueOf(it) })
            stmt.setString(8, ordem.observacao)
            stmt.setTimestamp(9, Timestamp.valueOf(ordem.criadoEm))

            stmt.executeQuery().use { rs ->
                if (rs.next()) return rs.getInt("id")
            }
        }
        throw SQLException("INSERT nao retornou id")
    }

    fun alterar(ordem: OrdemServico): Boolean {
        val id = ordem.id ?: return false

        val sql = """
            UPDATE ordem_servico SET
            funcionario_id = ?, status = ?, preco = ?,
            data_agendada = ?, data_conclusao = ?, observacao = ?
            WHERE id = ?
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.setObject(1, ordem.funcionarioId, Types.INTEGER)
            stmt.setString(2, ordem.status.name)
            stmt.setBigDecimal(3, ordem.preco)
            stmt.setTimestamp(4, Timestamp.valueOf(ordem.dataAgendada))
            stmt.setTimestamp(5, ordem.dataConclusao?.let { Timestamp.valueOf(it) })
            stmt.setString(6, ordem.observacao)
            stmt.setInt(7, id)

            return stmt.executeUpdate() > 0
        }
    }

    fun listar(): List<OrdemServico> {
        val ordens = mutableListOf<OrdemServico>()

        val sql = """
            SELECT $COLUNAS
            FROM ordem_servico
            ORDER BY id
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    ordens.add(mapear(rs))
                }
            }
        }
        return ordens
    }

    fun buscarPorId(id: Int): OrdemServico? {
        val sql = """
            SELECT $COLUNAS
            FROM ordem_servico
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

    fun listarAFaturarDoCliente(clienteId: Int): List<OrdemServico> {
        val ordens = mutableListOf<OrdemServico>()

        val sql = """
            SELECT $COLUNAS
            FROM ordem_servico
            WHERE cliente_id = ?
              AND id IN (SELECT ordem_id FROM vw_ordem_servico_a_faturar)
            ORDER BY data_conclusao
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.setInt(1, clienteId)
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    ordens.add(mapear(rs))
                }
            }
        }
        return ordens
    }

    fun estaAFaturar(id: Int): Boolean {
        val sql = """
            SELECT EXISTS(
                SELECT 1 FROM vw_ordem_servico_a_faturar WHERE ordem_id = ?
            ) AS a_faturar
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.setInt(1, id)
            stmt.executeQuery().use { rs ->
                if (rs.next()) return rs.getBoolean("a_faturar")
            }
        }
        return false
    }

    fun listarAgenda(): List<OrdemServicoAberta> {
        val linhas = mutableListOf<OrdemServicoAberta>()

        val sql = """
            SELECT ordem_id, data_agendada, status, servico, cliente,
                   telefone_cliente, responsavel, preco
            FROM vw_ordem_servico_aberta
            ORDER BY data_agendada
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    linhas.add(mapearAgenda(rs))
                }
            }
        }
        return linhas
    }

    private fun mapear(rs: ResultSet) = OrdemServico(
        id = rs.getInt("id"),
        tipoServico = rs.enumDe<TipoServico>("tipo_servico"),
        clienteId = rs.getInt("cliente_id"),
        funcionarioId = rs.intOuNulo("funcionario_id"),
        status = rs.enumDe<StatusOrdemServico>("status"),
        preco = rs.getBigDecimal("preco"),
        dataAgendada = rs.getTimestamp("data_agendada").toLocalDateTime(),
        dataConclusao = rs.getTimestamp("data_conclusao")?.toLocalDateTime(),
        observacao = rs.getString("observacao"),
        criadoEm = rs.getTimestamp("criado_em").toLocalDateTime()
    )

    private fun mapearAgenda(rs: ResultSet) = OrdemServicoAberta(
        ordemId = rs.getInt("ordem_id"),
        dataAgendada = rs.getTimestamp("data_agendada").toLocalDateTime(),
        status = rs.enumDe<StatusOrdemServico>("status"),
        servico = rs.enumDe<TipoServico>("servico"),
        cliente = rs.getString("cliente"),
        telefoneCliente = rs.getString("telefone_cliente"),
        responsavel = rs.getString("responsavel"),
        preco = rs.getBigDecimal("preco")
    )
}
