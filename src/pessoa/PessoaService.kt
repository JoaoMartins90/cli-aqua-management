package pessoa

import db.emTransacao
import java.sql.Connection

private val CPF_CNPJ = Regex("""\d{11}|\d{14}""")
private val TELEFONE = Regex("""\d{10,11}""")

class PessoaService(
    private val conn: Connection,
    private val dao: PessoaDAO
) {
    fun cadastrar(pessoa: Pessoa): Int {
        validar(pessoa)
        return conn.emTransacao { dao.insert(pessoa) }
    }

    fun alterar(pessoa: Pessoa): Boolean {
        require(pessoa.id != null) { "Não existe pessoa com esse id" }
        validar(pessoa)
        return dao.alterar(pessoa)
    }

    fun remover(id: Int): Boolean =
        dao.remover(id)

    fun listar(): List<Pessoa> =
        dao.listar()

    fun buscarPorId(id: Int): Pessoa? =
        dao.buscarPorId(id)

    fun buscarPorCpfCnpj(cpfCnpj: String): Pessoa? =
        dao.buscarPorCpfCnpj(cpfCnpj)

    fun idsExistentes(): List<Int> =
        dao.listarIdsPessoa()

    private fun validar(p: Pessoa) {
        require(p.nome.trim().length >= 3) { "Nome precisa ter ao menos 3 caracteres" }
        require(p.cpfCnpj.matches(CPF_CNPJ)) { "CPF precisa ter 11 dígitos e CNPJ 14, apenas números" }
        require(p.telefone.matches(TELEFONE)) { "Telefone precisa ter 10 ou 11 dígitos, apenas números" }
    }
}
