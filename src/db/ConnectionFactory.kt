package db

import java.sql.Connection
import java.sql.DriverManager

object ConnectionFactory {
    private val URL = System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5433/gerenciador"
    private val USER = System.getenv("DB_USER") ?: "postgres"
    private val PASSWORD = System.getenv("DB_PASSWORD") ?: "postgres"

    fun getConnection(): Connection =
        DriverManager.getConnection(URL, USER, PASSWORD)

}