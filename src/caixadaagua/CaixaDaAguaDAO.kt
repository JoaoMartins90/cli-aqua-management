package caixadaagua

import db.enumDe
import enums.Cor
import enums.Formato
import enums.Material
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Timestamp

private const val COLUNAS =
    "id, marca, modelo, capacidade, altura, largura, profundidade, " +
        "cor, material, formato, preco, estoque_atual, ativo, criado_em"

class CaixaDaAguaDAO(private val conn: Connection) {

    fun insert(caixaDaAgua: CaixaDaAgua): Int {

        val sql = """
            INSERT INTO caixa_da_agua(
            marca, modelo, capacidade, altura, largura, profundidade,
            cor, material, formato, preco, estoque_atual, ativo, criado_em)
            VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, caixaDaAgua.marca)
            stmt.setString(2, caixaDaAgua.modelo)
            stmt.setInt(3, caixaDaAgua.capacidade)
            stmt.setDouble(4, caixaDaAgua.altura)
            stmt.setDouble(5, caixaDaAgua.largura)
            stmt.setDouble(6, caixaDaAgua.profundidade)
            stmt.setString(7, caixaDaAgua.cor.name)
            stmt.setString(8, caixaDaAgua.material.name)
            stmt.setString(9, caixaDaAgua.formato.name)
            stmt.setBigDecimal(10, caixaDaAgua.preco)
            stmt.setInt(11, caixaDaAgua.estoqueAtual)
            stmt.setBoolean(12, caixaDaAgua.ativo)
            stmt.setTimestamp(13, Timestamp.valueOf(caixaDaAgua.criadoEm))

            stmt.executeQuery().use { rs ->
                if (rs.next()) return rs.getInt("id")
            }
        }
        throw SQLException("INSERT nao retornou id")
    }

    fun listar(): List<CaixaDaAgua> {
        val caixas = mutableListOf<CaixaDaAgua>()

        val sql = """
            SELECT $COLUNAS
            FROM caixa_da_agua
            ORDER BY id
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    caixas.add(mapear(rs))
                }
            }
        }
        return caixas
    }

    fun listarAtivas(): List<CaixaDaAgua> {
        val caixas = mutableListOf<CaixaDaAgua>()

        val sql = """
            SELECT $COLUNAS
            FROM caixa_da_agua
            WHERE ativo
            ORDER BY marca, modelo, capacidade
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    caixas.add(mapear(rs))
                }
            }
        }
        return caixas
    }

    fun remover(id: Int): Boolean {
        val sql = """
            DELETE FROM caixa_da_agua
            WHERE id = ?
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.setInt(1, id)
            return stmt.executeUpdate() > 0
        }
    }

    fun alterarAtivo(id: Int, ativo: Boolean): Boolean {
        val sql = """
            UPDATE caixa_da_agua SET
            ativo = ?
            WHERE id = ?
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.setBoolean(1, ativo)
            stmt.setInt(2, id)

            return stmt.executeUpdate() > 0
        }
    }

    fun temVenda(id: Int): Boolean {
        val sql = """
            SELECT EXISTS(
                SELECT 1 FROM venda_item WHERE caixa_da_agua_id = ?
            ) AS tem
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.setInt(1, id)
            stmt.executeQuery().use { rs ->
                if (rs.next()) return rs.getBoolean("tem")
            }
        }
        return false
    }

    fun baixarEstoque(id: Int, quantidade: Int): Boolean {
        val sql = """
            UPDATE caixa_da_agua SET
            estoque_atual = estoque_atual - ?
            WHERE id = ? AND estoque_atual >= ?
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.setInt(1, quantidade)
            stmt.setInt(2, id)
            stmt.setInt(3, quantidade)

            return stmt.executeUpdate() > 0
        }
    }

    fun devolverEstoque(id: Int, quantidade: Int): Boolean {
        val sql = """
            UPDATE caixa_da_agua SET
            estoque_atual = estoque_atual + ?
            WHERE id = ?
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.setInt(1, quantidade)
            stmt.setInt(2, id)

            return stmt.executeUpdate() > 0
        }
    }

    fun alterar(caixa: CaixaDaAgua): Boolean {
        val id = caixa.id ?: return false

        val sql = """
            UPDATE caixa_da_agua SET
            marca = ?, modelo = ?, capacidade = ?, altura = ?, largura = ?, profundidade = ?,
            cor = ?, material = ?, formato = ?, preco = ?, estoque_atual = ?
            WHERE id = ?
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, caixa.marca)
            stmt.setString(2, caixa.modelo)
            stmt.setInt(3, caixa.capacidade)
            stmt.setDouble(4, caixa.altura)
            stmt.setDouble(5, caixa.largura)
            stmt.setDouble(6, caixa.profundidade)
            stmt.setString(7, caixa.cor.name)
            stmt.setString(8, caixa.material.name)
            stmt.setString(9, caixa.formato.name)
            stmt.setBigDecimal(10, caixa.preco)
            stmt.setInt(11, caixa.estoqueAtual)
            stmt.setInt(12, id)

            return stmt.executeUpdate() > 0
        }
    }

    fun buscarPorId(id: Int): CaixaDaAgua? {
        val sql = """
            SELECT $COLUNAS
            FROM caixa_da_agua
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

    fun buscarPorModelo(marca: String, modelo: String, capacidade: Int): CaixaDaAgua? {
        val sql = """
            SELECT $COLUNAS
            FROM caixa_da_agua
            WHERE marca = ? AND modelo = ? AND capacidade = ?
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, marca)
            stmt.setString(2, modelo)
            stmt.setInt(3, capacidade)
            stmt.executeQuery().use { rs ->
                if (rs.next()) return mapear(rs)
            }
        }
        return null
    }

    fun listarIdsCaixas(): List<Int> {
        val ids = mutableListOf<Int>()

        val sql = """
            SELECT id
            FROM caixa_da_agua
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

    private fun mapear(rs: ResultSet) = CaixaDaAgua(
        id = rs.getInt("id"),
        marca = rs.getString("marca"),
        modelo = rs.getString("modelo"),
        capacidade = rs.getInt("capacidade"),
        altura = rs.getDouble("altura"),
        largura = rs.getDouble("largura"),
        profundidade = rs.getDouble("profundidade"),
        cor = rs.enumDe<Cor>("cor"),
        material = rs.enumDe<Material>("material"),
        formato = rs.enumDe<Formato>("formato"),
        preco = rs.getBigDecimal("preco"),
        estoqueAtual = rs.getInt("estoque_atual"),
        ativo = rs.getBoolean("ativo"),
        criadoEm = rs.getTimestamp("criado_em").toLocalDateTime()
    )
}
