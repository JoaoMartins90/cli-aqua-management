package servico

import enums.StatusOrdemServico
import enums.TipoServico
import java.math.BigDecimal
import java.time.LocalDateTime

data class OrdemServico(
    val id: Int? = null,
    val tipoServico: TipoServico,
    val clienteId: Int,
    val funcionarioId: Int? = null,
    val status: StatusOrdemServico = StatusOrdemServico.AGENDADO,
    val preco: BigDecimal,
    val dataAgendada: LocalDateTime,
    val dataConclusao: LocalDateTime? = null,
    val observacao: String? = null,
    val criadoEm: LocalDateTime = LocalDateTime.now()
) {
    val emAberto: Boolean
        get() = status == StatusOrdemServico.AGENDADO || status == StatusOrdemServico.EM_EXECUCAO
}
