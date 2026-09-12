package movimentacao

import enums.FormaPagamento
import enums.TipoMovimento
import java.math.BigDecimal
import java.time.LocalDateTime

data class Movimento(
    val id: Int? = null,
    val contaId: Int,
    val tipo: TipoMovimento,
    val valor: BigDecimal,
    val formaPagamento: FormaPagamento,
    val vendaId: Int? = null,
    val pessoaId: Int? = null,
    val estornoDeId: Int? = null,
    val descricao: String,
    val dataMovimentacao: LocalDateTime = LocalDateTime.now()
) {
    val efeitoNoSaldo: BigDecimal
        get() = if (tipo == TipoMovimento.ENTRADA) valor else valor.negate()
}
