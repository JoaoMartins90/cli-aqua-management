package caixadaagua

import enums.Cor
import enums.Formato
import enums.Material
import java.sql.Connection
import java.sql.SQLException

class CaixaDaAguaDAO(private val conn: Connection) {

    fun insert(caixaDaAgua: CaixaDaAgua): Int? {

        val sql = """
            INSERT INTO caixa_da_agua(
            marca, modelo, altura, largura, profundidade,
            cor, material, formato, preco)
            VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
        """.trimIndent()

        try {
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, caixaDaAgua.marca)
                stmt.setString(2, caixaDaAgua.modelo)
                stmt.setDouble(3, caixaDaAgua.altura)
                stmt.setDouble(4, caixaDaAgua.largura)
                stmt.setDouble(5, caixaDaAgua.profundidade)
                stmt.setString(6, caixaDaAgua.cor.name)
                stmt.setString(7, caixaDaAgua.material.name)
                stmt.setString(8, caixaDaAgua.formato.name)
                stmt.setBigDecimal(9, caixaDaAgua.preco)

                val rs = stmt.executeQuery()
                if (rs.next()) return rs.getInt("id")

            }

        } catch (ex: SQLException) {
            println("Erro ao salvar: ${ex.message}")
        }

        return null
    }

    fun listar(): List<CaixaDaAgua> {
        val caixas = mutableListOf<CaixaDaAgua>()

        val sql = """
            SELECT id, marca, modelo, altura, largura, profundidade,
            cor, material, formato, preco
            FROM caixa_da_agua
            ORDER BY id
        """.trimIndent()

        try {
            conn.prepareStatement(sql).use { stmt ->
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        caixas += CaixaDaAgua(
                            id = rs.getInt("id"),
                            marca = rs.getString("marca"),
                            modelo = rs.getString("modelo"),
                            altura = rs.getDouble("altura"),
                            largura = rs.getDouble("largura"),
                            profundidade = rs.getDouble("profundidade"),
                            cor = Cor.valueOf(rs.getString("cor")),
                            material = Material.valueOf(rs.getString("material")),
                            formato = Formato.valueOf(rs.getString("formato")),
                            preco = rs.getBigDecimal("preco")

                        )
                    }
                }
            }
        } catch (ex: SQLException) {
            println("Erro ao listar: ${ex.message}")
        }
        return caixas
    }

    fun remover(id: Int) {
        val sql = """
            DELETE FROM caixa_da_agua
            WHERE id = ?;
        """.trimIndent()

        try {
            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, id)
                stmt.executeUpdate()
            }
        } catch (ex: SQLException) {
            println("Erro ao remover: ${ex.message}")
        }
    }

    fun listarIdsCaixas(): List<Int> {
        val ids = mutableListOf<Int>()

        val sql = """
            SELECT id
            FROM caixa_da_agua
            ORDER BY id
        """.trimIndent()

        try {
            conn.prepareStatement(sql).use { stmt ->
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        ids.add(rs.getInt("id"))
                    }
                }
            }
        } catch (ex: SQLException) {
            println("Erro ao listar os Ids: ${ex.message}")
        }

        return ids
    }
}