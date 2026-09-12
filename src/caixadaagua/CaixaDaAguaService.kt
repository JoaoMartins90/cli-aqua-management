package caixadaagua

import java.math.BigDecimal

class CaixaDaAguaService(
    private val dao: CaixaDaAguaDAO
) {
    fun cadastrar(caixa: CaixaDaAgua): Int {
        validar(caixa)
        return dao.insert(caixa)
    }

    fun alterar(caixa: CaixaDaAgua): Boolean {
        require(caixa.id != null) { "Nao existe caixa com esse id" }
        validar(caixa)
        return dao.alterar(caixa)
    }

    fun remover(id: Int): Resultado {
        require(dao.buscarPorId(id) != null) { "Nao existe caixa com o id $id" }

        if (dao.temVenda(id)) {
            dao.alterarAtivo(id, false)
            return Resultado.DESATIVADA
        }
        return if (dao.remover(id)) Resultado.REMOVIDA else Resultado.NADA_FEITO
    }

    fun reativar(id: Int): Boolean {
        val caixa = dao.buscarPorId(id)
        require(caixa != null) { "Nao existe caixa com o id $id" }
        require(!caixa.ativo) { "A caixa $id ja esta ativa" }
        return dao.alterarAtivo(id, true)
    }

    fun baixarEstoque(caixaId: Int, quantidade: Int) {
        require(quantidade > 0) { "Quantidade precisa ser maior que zero" }

        if (dao.baixarEstoque(caixaId, quantidade)) return

        val caixa = dao.buscarPorId(caixaId)
            ?: throw IllegalArgumentException("Nao existe caixa com o id $caixaId")

        throw IllegalStateException(
            "Estoque insuficiente de ${caixa.marca} ${caixa.modelo}: restam " +
                "${caixa.estoqueAtual} depois dos itens ja lancados nesta venda, " +
                "foram pedidas $quantidade"
        )
    }

    fun devolverEstoque(caixaId: Int, quantidade: Int) {
        require(quantidade > 0) { "Quantidade precisa ser maior que zero" }
        val devolveu = dao.devolverEstoque(caixaId, quantidade)
        require(devolveu) { "Nao existe caixa com o id $caixaId" }
    }

    fun listar(): List<CaixaDaAgua> =
        dao.listar()

    fun listarAtivas(): List<CaixaDaAgua> =
        dao.listarAtivas()

    fun buscarPorId(id: Int): CaixaDaAgua? =
        dao.buscarPorId(id)

    fun buscarPorModelo(marca: String, modelo: String, capacidade: Int): CaixaDaAgua? =
        dao.buscarPorModelo(marca, modelo, capacidade)

    fun idsExistentes(): List<Int> =
        dao.listarIdsCaixas()

    private fun validar(c: CaixaDaAgua) {
        require(c.marca.isNotBlank()) { "Marca nao pode ser vazia" }
        require(c.modelo.isNotBlank()) { "Modelo nao pode ser vazio" }
        require(c.capacidade > 0) { "Capacidade deve ser maior que zero" }
        require(c.altura > 0 && c.largura > 0 && c.profundidade > 0) {
            "Altura, largura e profundidade devem ser maiores que zero"
        }
        require(c.preco > BigDecimal.ZERO) { "Preco deve ser maior que zero" }
        require(c.estoqueAtual >= 0) { "Estoque nao pode ser negativo" }

    }

    enum class Resultado { REMOVIDA, DESATIVADA, NADA_FEITO }
}
