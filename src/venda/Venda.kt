package venda

import enums.CondicaoPagamento
import enums.StatusVenda
import java.math.BigDecimal
import java.time.LocalDateTime

data class Venda(
    val id: Int? = null,
    val clienteId: Int,
    val funcionarioId: Int,
    val condicaoPagamento: CondicaoPagamento,
    val valorTotal: BigDecimal,
    val status: StatusVenda = StatusVenda.EFETIVADA,
    val observacao: String? = null,
    val dataVenda: LocalDateTime = LocalDateTime.now()
)
