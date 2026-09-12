package venda

import java.math.BigDecimal

data class VendaItem(
    val id: Int? = null,
    val vendaId: Int? = null,
    val caixaDaAguaId: Int? = null,
    val ordemServicoId: Int? = null,
    val descricao: String,
    val quantidade: Int,
    val precoUnitario: BigDecimal
) {
    val subtotal: BigDecimal
        get() = precoUnitario.multiply(quantidade.toBigDecimal())
}
