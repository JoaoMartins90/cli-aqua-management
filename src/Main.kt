import caixadaagua.CaixaDaAguaDAO
import caixadaagua.CaixaDaAguaService
import caixadaagua.menu
import db.ConnectionFactory.getConnection
import pessoa.PessoaDAO
import pessoa.PessoaService
import java.sql.Connection

class App(conn: Connection) {
    val caixa = CaixaDaAguaService(CaixaDaAguaDAO(conn))
    val pessoa = PessoaService(conn, PessoaDAO(conn))
}

fun main() {

    getConnection().use { conn ->
        menuPrincipal(App(conn))

    }

}

fun menuPrincipal(app: App) {
    do {
        println("0 - SAIR")
        println("1 - GERENCIAR CAIXA DE AGUA")
        println("2 - GERENCIAR FUNCIONARIOS")

        val op = readln()

        when (op) {
            "0" -> println("Sistema encerrado")
            "1" -> menu(app.caixa)
            "2" -> println("Gerenciar Funcionarios")
            else -> println("Opção inválida")
        }
    } while (op != "0")
}
