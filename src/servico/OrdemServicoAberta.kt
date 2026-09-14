package servico

import enums.StatusOrdemServico
import enums.TipoServico
import java.math.BigDecimal
import java.time.LocalDateTime

data class OrdemServicoAberta(
    val ordemId: Int,
    val dataAgendada: LocalDateTime,
    val status: StatusOrdemServico,
    val servico: TipoServico,
    val cliente: String,
    val telefoneCliente: String,
    val responsavel: String?,
    val preco: BigDecimal
)
