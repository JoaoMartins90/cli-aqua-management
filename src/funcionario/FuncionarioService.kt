package funcionario

import java.math.BigDecimal
import java.time.LocalDate

class FuncionarioService(
    private val dao: FuncionarioDAO
) {
    fun cadastrar(funcionario: Funcionario): Int {
        validar(funcionario)
        return dao.insert(funcionario)
    }

    fun alterar(funcionario: Funcionario): Boolean {
        require(funcionario.id != null) { "Nao existe funcionario com esse id" }
        validar(funcionario)
        return dao.alterar(funcionario)
    }

    fun demitir(id: Int, dataDemissao: LocalDate = LocalDate.now()): Boolean {
        val funcionario = dao.buscarPorId(id)
        require(funcionario != null) { "Nao existe funcionario com o id $id" }
        require(funcionario.ativo) { "Funcionario ja foi demitido em ${funcionario.dataDemissao}" }
        require(!dataDemissao.isBefore(funcionario.dataAdmissao)) {
            "Demissao nao pode ser anterior a admissao (${funcionario.dataAdmissao})"
        }
        return dao.demitir(id, dataDemissao)
    }

    fun listar(): List<Funcionario> =
        dao.listar()

    fun listarAtivos(): List<Funcionario> =
        dao.listarAtivos()

    fun buscarPorId(id: Int): Funcionario? =
        dao.buscarPorId(id)

    fun buscarPorPessoaId(pessoaId: Int): Funcionario? =
        dao.buscarPorPessoaId(pessoaId)

    fun idsExistentes(): List<Int> =
        dao.listarIdsFuncionarios()

    private fun validar(f: Funcionario) {
        require(f.pessoaId > 0) { "Funcionario precisa apontar para uma pessoa" }
        require(f.salario > BigDecimal.ZERO) { "Salario deve ser maior que zero" }
        require(f.dataDemissao == null || !f.dataDemissao.isBefore(f.dataAdmissao)) {
            "Demissao nao pode ser anterior a admissao"
        }
    }
}
