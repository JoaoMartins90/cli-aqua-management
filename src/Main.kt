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
import movimentacao.MovimentoDAO
import movimentacao.MovimentoService
import pessoa.PessoaDAO
import pessoa.PessoaService
import pessoa.menuPessoa
import venda.VendaDAO
import venda.VendaItemDAO
import venda.VendaService
import venda.menuVenda
import java.sql.Connection

class App(conn: Connection) {
    val caixa = CaixaDaAguaService(CaixaDaAguaDAO(conn))
    val clientes = ClienteService(ClienteDAO(conn))
    val funcionarios = FuncionarioService(FuncionarioDAO(conn))
    val contas = ContaService(ContaDAO(conn))
    val movimentos = MovimentoService(conn, MovimentoDAO(conn), contas)
    val pessoa = PessoaService(conn, PessoaDAO(conn), clientes, funcionarios, contas)
    val vendas = VendaService(
        conn,
        VendaDAO(conn),
        VendaItemDAO(conn),
        caixa,
        movimentos,
        contas,
        clientes,
        funcionarios
    )
}

fun main() {

    getConnection().use { conn ->
        menuPrincipal(App(conn))

    }

}

fun menuPrincipal(app: App) {
    do {
        println("0 - SAIR")
        println("1 - CADASTROS")
        println("2 - OPERACAO")

        val op = readln()

        when (op) {
            "0" -> println("Sistema encerrado")
            "1" -> menuCadastros(app)
            "2" -> menuVenda(app.vendas, app.pessoa, app.caixa)
            else -> println("Opção inválida")
        }
    } while (op != "0")
}

fun menuCadastros(app: App) {
    do {
        println("0 - VOLTAR AO MENU PRINCIPAL")
        println("1 - PESSOAS (CLIENTE / FUNCIONARIO / CONTA)")
        println("2 - CAIXAS DE AGUA")

        val op = readln()

        when (op) {
            "0" -> {}
            "1" -> menuPessoa(app.pessoa)
            "2" -> menuCaixa(app.caixa)
            else -> println("Opção inválida")
        }
    } while (op != "0")
}
