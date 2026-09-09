package cliente

import java.math.BigDecimal

class ClienteService(
    private val dao: ClienteDAO
) {
    fun cadastrar(cliente: Cliente): Int {
        validar(cliente)
        return dao.insert(cliente)
    }

    fun alterar(cliente: Cliente): Boolean {
        require(cliente.id != null) { "Nao existe cliente com esse id" }
        validar(cliente)
        return dao.alterar(cliente)
    }

    fun remover(id: Int): Boolean =
        dao.remover(id)

    fun listar(): List<Cliente> =
        dao.listar()

    fun buscarPorId(id: Int): Cliente? =
        dao.buscarPorId(id)

    fun buscarPorPessoaId(pessoaId: Int): Cliente? =
        dao.buscarPorPessoaId(pessoaId)

    fun idsExistentes(): List<Int> =
        dao.listarIdsClientes()

    private fun validar(c: Cliente) {
        require(c.pessoaId > 0) { "Cliente precisa apontar para uma pessoa" }
        require(c.limiteCredito >= BigDecimal.ZERO) { "Limite de credito nao pode ser negativo" }
    }
}
