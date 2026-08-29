package caixadaagua

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
}