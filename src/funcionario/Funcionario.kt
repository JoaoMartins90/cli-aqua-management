package funcionario

import enums.Setor
import java.math.BigDecimal
import java.time.LocalDate

data class Funcionario(
    val id: Int? = null,
    val pessoaId: Int,
    val setor: Setor,
    val salario: BigDecimal,
    val dataAdmissao: LocalDate = LocalDate.now(),
    val dataDemissao: LocalDate? = null
) {
    val ativo: Boolean get() = dataDemissao == null
}
