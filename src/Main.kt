import caixadaagua.CaixaDaAguaDAO
import caixadaagua.CaixaDaAguaService
import caixadaagua.menuCaixa
import cliente.ClienteDAO
import cliente.ClienteService
import conferencia.ConferenciaDAO
import conferencia.ConferenciaService
import conferencia.mostrarConferencia
import db.ConnectionFactory.getConnection
import financeiro.ContaDAO
import financeiro.ContaService
import financeiro.FinanceiroDAO
import financeiro.FinanceiroService
import financeiro.menuFinanceiro
import funcionario.FuncionarioDAO
import funcionario.FuncionarioService
import movimentacao.MovimentoDAO
import movimentacao.MovimentoService
import pessoa.PessoaDAO
import pessoa.PessoaService
import pessoa.menuPessoa
import servico.OrdemServicoDAO
import servico.OrdemServicoService
import servico.menuOrdemServico
import utils.lerOpcao
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
    val ordens = OrdemServicoService(OrdemServicoDAO(conn), clientes, funcionarios)
    val vendas = VendaService(
        conn,
        VendaDAO(conn),
        VendaItemDAO(conn),
        caixa,
        ordens,
        movimentos,
        contas,
        clientes,
        funcionarios
    )
    val financeiro = FinanceiroService(
        conn,
        FinanceiroDAO(conn),
        contas,
        movimentos,
        vendas,
        clientes,
        funcionarios
    )
    val conferencia = ConferenciaService(ConferenciaDAO(conn))
}

fun main() {

    getConnection().use { conn ->
        val app = App(conn)
        mostrarConferencia(app.conferencia)
        menuPrincipal(app)
    }

}

fun menuPrincipal(app: App) {
    do {
        println("0 - SAIR")
        println("1 - CADASTROS")
        println("2 - OPERACAO")
        println("3 - FINANCEIRO")

        val op = lerOpcao()

        when (op) {
            "0" -> println("Sistema encerrado")
            "1" -> menuCadastros(app)
            "2" -> menuOperacao(app)
            "3" -> menuFinanceiro(app.financeiro, app.ordens, app.pessoa)
            else -> println("Opção inválida")
        }
    } while (op != "0")
}

fun menuCadastros(app: App) {
    do {
        println("0 - VOLTAR AO MENU PRINCIPAL")
        println("1 - PESSOAS (CLIENTE / FUNCIONARIO / CONTA)")
        println("2 - CAIXAS DE AGUA")

        val op = lerOpcao()

        when (op) {
            "0" -> {}
            "1" -> menuPessoa(app.pessoa)
            "2" -> menuCaixa(app.caixa)
            else -> println("Opção inválida")
        }
    } while (op != "0")
}

fun menuOperacao(app: App) {
    do {
        println("0 - VOLTAR AO MENU PRINCIPAL")
        println("1 - VENDAS")
        println("2 - ORDENS DE SERVICO")

        val op = lerOpcao()

        when (op) {
            "0" -> {}
            "1" -> menuVenda(app.vendas, app.pessoa, app.caixa, app.ordens)
            "2" -> menuOrdemServico(app.ordens, app.pessoa)
            else -> println("Opção inválida")
        }
    } while (op != "0")
}
