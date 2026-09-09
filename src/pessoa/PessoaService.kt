package pessoa

import cliente.Cliente
import cliente.ClienteService
import db.emTransacao
import enums.Setor
import financeiro.Conta
import financeiro.ContaService
import funcionario.Funcionario
import funcionario.FuncionarioService
import java.math.BigDecimal
import java.sql.Connection
import java.time.LocalDate

private val CPF_CNPJ = Regex("""\d{11}|\d{14}""")
private val TELEFONE = Regex("""\d{10,11}""")

class PessoaService(
    private val conn: Connection,
    private val dao: PessoaDAO,
    private val clientes: ClienteService,
    private val funcionarios: FuncionarioService,
    private val contas: ContaService
) {
    fun cadastrar(pessoa: Pessoa): Int {
        validar(pessoa)
        return conn.emTransacao { dao.insert(pessoa) }
    }

    fun tornarCliente(pessoaId: Int, limiteCredito: BigDecimal = BigDecimal.ZERO): Int =
        conn.emTransacao {
            val pessoa = exigirPessoa(pessoaId)
            require(clientes.buscarPorPessoaId(pessoaId) == null) {
                "${pessoa.nome} já é cliente"
            }
            abrirContaSeNaoTiver(pessoa)
            clientes.cadastrar(Cliente(pessoaId = pessoaId, limiteCredito = limiteCredito))
        }

    fun tornarFuncionario(
        pessoaId: Int,
        setor: Setor,
        salario: BigDecimal,
        dataAdmissao: LocalDate = LocalDate.now()
    ): Int =
        conn.emTransacao {
            val pessoa = exigirPessoa(pessoaId)
            require(funcionarios.buscarPorPessoaId(pessoaId) == null) {
                "${pessoa.nome} já é funcionário"
            }
            funcionarios.cadastrar(
                Funcionario(
                    pessoaId = pessoaId,
                    setor = setor,
                    salario = salario,
                    dataAdmissao = dataAdmissao
                )
            )
        }

    fun papeis(pessoaId: Int): List<String> = buildList {
        if (clientes.buscarPorPessoaId(pessoaId) != null) add("CLIENTE")
        if (funcionarios.buscarPorPessoaId(pessoaId) != null) add("FUNCIONARIO")
    }

    fun alterar(pessoa: Pessoa): Boolean {
        require(pessoa.id != null) { "Não existe pessoa com esse id" }
        validar(pessoa)
        return dao.alterar(pessoa)
    }

    fun remover(id: Int): Boolean {
        val papeis = papeis(id)
        require(papeis.isEmpty()) {
            "Pessoa não pode ser removida: ainda é ${papeis.joinToString(" e ")}"
        }
        require(contas.buscarPorPessoaId(id) == null) {
            "Pessoa não pode ser removida: ainda tem conta"
        }
        return dao.remover(id)
    }

    fun temConta(pessoaId: Int): Boolean =
        contas.buscarPorPessoaId(pessoaId) != null

    fun listar(): List<Pessoa> =
        dao.listar()

    fun buscarPorId(id: Int): Pessoa? =
        dao.buscarPorId(id)

    fun buscarPorCpfCnpj(cpfCnpj: String): Pessoa? =
        dao.buscarPorCpfCnpj(cpfCnpj)

    fun idsExistentes(): List<Int> =
        dao.listarIdsPessoa()

    private fun exigirPessoa(pessoaId: Int): Pessoa =
        dao.buscarPorId(pessoaId)
            ?: throw IllegalArgumentException("Não existe pessoa com o id $pessoaId")

    private fun abrirContaSeNaoTiver(pessoa: Pessoa) {
        if (contas.buscarPorPessoaId(pessoa.id!!) != null) return
        contas.cadastrar(Conta(pessoaId = pessoa.id, descricao = "Conta de ${pessoa.nome}"))
    }

    private fun validar(p: Pessoa) {
        require(p.nome.trim().length >= 3) { "Nome precisa ter ao menos 3 caracteres" }
        require(p.cpfCnpj.matches(CPF_CNPJ)) { "CPF precisa ter 11 dígitos e CNPJ 14, apenas números" }
        require(p.telefone.matches(TELEFONE)) { "Telefone precisa ter 10 ou 11 dígitos, apenas números" }
    }
}
