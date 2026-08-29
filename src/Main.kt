import caixadaagua.menu
import db.ConnectionFactory.getConnection
import java.sql.Connection

fun main() {

    getConnection().use { conn ->
        menuPrincipal(conn)

    }

}

fun menuPrincipal(conn: Connection) {
    do {
        println("0 - SAIR")
        println("1 - GERENCIAR CAIXA DE AGUA")

        val op = readln()

        when (op) {
            "0" -> println("Sistema encerrado")
            "1" -> menu(conn)
            "2" -> println("teste")
            else -> println("Opção inválida")
        }
    } while (op != "0")

}