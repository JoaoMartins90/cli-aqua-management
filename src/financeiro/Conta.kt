package financeiro

import java.math.BigDecimal

data class Conta(
    val id: Int? = null,
    val pessoaId: Int,
    val descricao: String,
    val saldo: BigDecimal = BigDecimal.ZERO
)
