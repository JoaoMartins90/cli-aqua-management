package conferencia

import java.math.BigDecimal

data class DivergenciaSaldo(
    val contaId: Int,
    val descricao: String,
    val saldoGravado: BigDecimal,
    val saldoCalculado: BigDecimal
)
