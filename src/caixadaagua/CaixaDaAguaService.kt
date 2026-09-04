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

    fun remover(id: Int): Boolean =
        dao.remover(id)

    fun listar(): List<CaixaDaAgua> =
        dao.listar()

    fun buscarPorId(id: Int): CaixaDaAgua? =
        dao.buscarPorId(id)

    fun idsExistentes(): List<Int> =
        dao.listarIdsCaixas()

    private fun validar(c: CaixaDaAgua) {
        require(c.marca.isNotBlank()) { "Marca nao pode ser vazia" }
        require(c.modelo.isNotBlank()) { "Modelo nao pode ser vazio" }
        require(c.capacidadeLitros > 0) { "Capacidade deve ser maior que zero" }
        require(c.altura > 0 && c.largura > 0 && c.profundidade > 0) {
            "Altura, largura e profundidade devem ser maiores que zero"
        }
        require(c.preco > BigDecimal.ZERO) { "Preco deve ser maior que zero" }
        require(c.estoqueAtual >= 0) { "Estoque nao pode ser negativo" }

    }
}