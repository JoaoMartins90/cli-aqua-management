import caixadaagua.CaixaDaAguaDAO
import caixadaagua.CaixaDaAguaService
import caixadaagua.menuCaixa
import cliente.ClienteDAO
import cliente.ClienteService
import db.ConnectionFactory.getConnection
import financeiro.ContaDAO
import financeiro.ContaService
import funcionario.FuncionarioDAO
import funcionario.FuncionarioService
import pessoa.PessoaDAO
import pessoa.PessoaService
import pessoa.menuPessoa
import java.sql.Connection

class App(conn: Connection) {
    val caixa = CaixaDaAguaService(CaixaDaAguaDAO(conn))
    val clientes = ClienteService(ClienteDAO(conn))
    val funcionarios = FuncionarioService(FuncionarioDAO(conn))
    val contas = ContaService(ContaDAO(conn))
    val pessoa = PessoaService(conn, PessoaDAO(conn), clientes, funcionarios, contas)
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
            "1" -> menuCaixa(app.caixa)
            "2" -> menuPessoa(app.pessoa)
            else -> println("Opção inválida")
        }
    } while (op != "0")
}
