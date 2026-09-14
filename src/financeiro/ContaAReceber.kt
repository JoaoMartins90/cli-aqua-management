package financeiro

import java.math.BigDecimal
import java.time.LocalDateTime

data class ContaAReceber(
    val vendaId: Int,
    val dataVenda: LocalDateTime,
    val cliente: String,
    val telefone: String,
    val valorTotal: BigDecimal,
    val pago: BigDecimal,
    val saldoDevedor: BigDecimal
)
