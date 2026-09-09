package financeiro

class ContaService(
    private val dao: ContaDAO
) {
    fun cadastrar(conta: Conta): Int {
        validar(conta)
        return dao.insert(conta)
    }

    fun alterar(conta: Conta): Boolean {
        require(conta.id != null) { "Nao existe conta com esse id" }
        validar(conta)
        return dao.alterar(conta)
    }

    fun remover(id: Int): Boolean =
        dao.remover(id)

    fun listar(): List<Conta> =
        dao.listar()

    fun buscarPorId(id: Int): Conta? =
        dao.buscarPorId(id)

    fun buscarPorPessoaId(pessoaId: Int): Conta? =
        dao.buscarPorPessoaId(pessoaId)

    fun idsExistentes(): List<Int> =
        dao.listarIdsContas()

    private fun validar(c: Conta) {
        require(c.pessoaId > 0) { "Conta precisa apontar para uma pessoa" }
        require(c.descricao.isNotBlank()) { "Descricao nao pode ser vazia" }
    }
}
