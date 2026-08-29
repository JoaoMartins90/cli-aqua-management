package db

import java.sql.Connection
import java.sql.DriverManager

object ConnectionFactory {
    private const val URL = "jdbc:postgresql://localhost:5432/gerenciador"
    private const val USER = "postgres"
    private const val PASSWORD = "postgres"

    fun getConnection(): Connection =
        DriverManager.getConnection(URL, USER, PASSWORD)

}