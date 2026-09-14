package conferencia

import java.math.BigDecimal

data class DivergenciaVenda(
    val vendaId: Int,
    val valorTotal: BigDecimal,
    val somaItens: BigDecimal
)
