package caixadaagua

import enums.Cor
import enums.Formato
import enums.Material
import java.sql.Connection
import java.sql.SQLException
import java.sql.Timestamp

class CaixaDaAguaDAO(private val conn: Connection) {

    fun insert(caixaDaAgua: CaixaDaAgua): Int {

        val sql = """
            INSERT INTO caixa_da_agua(
            marca, modelo, capacidade, altura, largura, profundidade,
            cor, material, formato, preco, estoque_atual, criado_em)
            VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
            stmt.setTimestamp(12, Timestamp.valueOf(caixaDaAgua.criadoEm))

            stmt.executeQuery().use { rs ->
                if (rs.next()) return rs.getInt("id")
            }
        }
        throw SQLException("INSERT nao retornou id")
    }

    fun listar(): List<CaixaDaAgua> {
        val caixas = mutableListOf<CaixaDaAgua>()

        val sql = """
            SELECT id, marca, modelo, capacidade, altura, largura, profundidade,
            cor, material, formato, preco, estoque_atual, criado_em
            FROM caixa_da_agua
            ORDER BY id
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    caixas.add(CaixaDaAgua(
                        id = rs.getInt("id"),
                        marca = rs.getString("marca"),
                        modelo = rs.getString("modelo"),
                        capacidade= rs.getInt("capacidade"),
                        altura = rs.getDouble("altura"),
                        largura = rs.getDouble("largura"),
                        profundidade = rs.getDouble("profundidade"),
                        cor = Cor.valueOf(rs.getString("cor")),
                        material = Material.valueOf(rs.getString("material")),
                        formato = Formato.valueOf(rs.getString("formato")),
                        preco = rs.getBigDecimal("preco"),
                        estoqueAtual = rs.getInt("estoque_atual"),
                        criadoEm = rs.getTimestamp("criado_em").toLocalDateTime()
                    ))
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
            SELECT id, marca, modelo, capacidade, altura, largura, profundidade,
            cor, material, formato, preco, estoque_atual, criado_em
            FROM caixa_da_agua
            WHERE id = ?
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.setInt(1, id)
            stmt.executeQuery().use { rs ->
                if (rs.next()) return CaixaDaAgua(
                    id = rs.getInt("id"),
                    marca = rs.getString("marca"),
                    modelo = rs.getString("modelo"),
                    capacidade = rs.getInt("capacidade_litros"),
                    altura = rs.getDouble("altura"),
                    largura = rs.getDouble("largura"),
                    profundidade = rs.getDouble("profundidade"),
                    cor = Cor.valueOf(rs.getString("cor")),
                    material = Material.valueOf(rs.getString("material")),
                    formato = Formato.valueOf(rs.getString("formato")),
                    preco = rs.getBigDecimal("preco"),
                    estoqueAtual = rs.getInt("estoque_atual"),
                    criadoEm = rs.getTimestamp("criado_em").toLocalDateTime()
                )
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
}
