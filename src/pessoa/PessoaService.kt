package pessoa

class PessoaService(
    private val dao: PessoaDAO
) {
    fun cadastrar(pessoa: Pessoa): Int {
        validar(pessoa)
        return dao.insert(pessoa)
    }

    fun alterar(pessoa: Pessoa): Boolean {
        require(pessoa.id != null) { "Não existe pessoa com esse id" }
        validar(pessoa)
        return dao.alterar(pessoa)
    }

    fun remover(id: Int): Boolean =
        dao.remover(id)

    fun listar(pessoa: Pessoa): List<Pessoa> =
        dao.listar()

    fun buscarPorId(id: Int): Pessoa? =
        dao.buscarPorId(id)

    fun idsExistentes(): List<Int> =
        dao.listarIdsPessoa()

    private fun validar(p: Pessoa) {
        require(p.nome.length > 3) { "Nome precisa ter ao menos 3 caracteres" }
        require(p.cpfCnpj.isNotBlank()) { "CPF/CNPJ não pode ser vazio" }
        require(p.telefone.isNotBlank()) {}
        require(p.nome)
    }
}