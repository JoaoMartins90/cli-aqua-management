package venda

import enums.CondicaoPagamento
import enums.FormaPagamento
import java.math.BigDecimal

data class Pedido(
    val clienteId: Int,
    val funcionarioId: Int,
    val condicaoPagamento: CondicaoPagamento,
    val formaPagamento: FormaPagamento? = null,
    val observacao: String? = null,
    val itens: List<VendaItem> = emptyList()
) {
    val valorTotal: BigDecimal
        get() = itens.fold(BigDecimal.ZERO) { soma, item -> soma + item.subtotal }
}
