package cliente

import java.math.BigDecimal
import java.time.LocalDateTime

data class Cliente(
    val id: Int? = null,
    val pessoaId: Int,
    val limiteCredito: BigDecimal = BigDecimal.ZERO,
    val criadoEm: LocalDateTime = LocalDateTime.now()
)
