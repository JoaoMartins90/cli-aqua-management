package servico

import cliente.ClienteService
import enums.StatusOrdemServico
import funcionario.FuncionarioService
import java.math.BigDecimal
import java.time.LocalDateTime

private val TRANSICOES = mapOf(
    StatusOrdemServico.AGENDADO to setOf(StatusOrdemServico.EM_EXECUCAO, StatusOrdemServico.CANCELADO),
    StatusOrdemServico.EM_EXECUCAO to setOf(StatusOrdemServico.CONCLUIDO, StatusOrdemServico.CANCELADO)
)

class OrdemServicoService(
    private val dao: OrdemServicoDAO,
    private val clientes: ClienteService,
    private val funcionarios: FuncionarioService
) {
    fun abrir(ordem: OrdemServico): Int {
        require(ordem.status == StatusOrdemServico.AGENDADO && ordem.dataConclusao == null) {
            "Ordem nova nasce agendada e sem data de conclusao"
        }
        require(clientes.buscarPorId(ordem.clienteId) != null) {
            "Nao existe cliente com o id ${ordem.clienteId}"
        }
        ordem.funcionarioId?.let { exigirFuncionarioAtivo(it) }
        validar(ordem)
        return dao.insert(ordem)
    }

    fun designarResponsavel(ordemId: Int, funcionarioId: Int): Boolean {
        val ordem = exigirOrdem(ordemId)
        require(ordem.emAberto) { "A ordem $ordemId esta ${ordem.status}: nao muda mais de responsavel" }
        exigirFuncionarioAtivo(funcionarioId)
        return dao.alterar(ordem.copy(funcionarioId = funcionarioId))
    }

    fun reagendar(ordemId: Int, novaData: LocalDateTime): Boolean {
        val ordem = exigirOrdem(ordemId)
        require(ordem.emAberto) { "A ordem $ordemId esta ${ordem.status}: nao pode ser reagendada" }
        return dao.alterar(ordem.copy(dataAgendada = novaData))
    }

    fun iniciar(ordemId: Int, funcionarioId: Int? = null): Boolean {
        val ordem = exigirOrdem(ordemId)
        exigirTransicao(ordem, StatusOrdemServico.EM_EXECUCAO)

        val responsavel = funcionarioId ?: ordem.funcionarioId
        require(responsavel != null) { "A ordem $ordemId precisa de um responsavel para ser iniciada" }
        exigirFuncionarioAtivo(responsavel)

        return dao.alterar(ordem.copy(status = StatusOrdemServico.EM_EXECUCAO, funcionarioId = responsavel))
    }

    fun concluir(ordemId: Int, dataConclusao: LocalDateTime = LocalDateTime.now()): Boolean {
        val ordem = exigirOrdem(ordemId)
        exigirTransicao(ordem, StatusOrdemServico.CONCLUIDO)
        require(!dataConclusao.isBefore(ordem.dataAgendada)) {
            "Conclusao ($dataConclusao) nao pode ser antes do agendamento (${ordem.dataAgendada}). " +
                "Se o servico foi adiantado, reagende a ordem primeiro"
        }
        require(!dataConclusao.isAfter(LocalDateTime.now())) { "Conclusao nao pode ser no futuro" }

        return dao.alterar(ordem.copy(status = StatusOrdemServico.CONCLUIDO, dataConclusao = dataConclusao))
    }

    fun cancelar(ordemId: Int): Boolean {
        val ordem = exigirOrdem(ordemId)
        exigirTransicao(ordem, StatusOrdemServico.CANCELADO)
        return dao.alterar(ordem.copy(status = StatusOrdemServico.CANCELADO))
    }

    fun exigirFaturavel(ordemId: Int, clienteId: Int): OrdemServico {
        val ordem = exigirOrdem(ordemId)
        require(ordem.status == StatusOrdemServico.CONCLUIDO) {
            "A ordem $ordemId esta ${ordem.status}: so ordem concluida entra numa venda"
        }
        require(ordem.clienteId == clienteId) { "A ordem $ordemId e de outro cliente" }
        require(dao.estaAFaturar(ordemId)) { "A ordem $ordemId ja foi cobrada em outra venda" }
        return ordem
    }

    fun agenda(): List<OrdemServicoAberta> =
        dao.listarAgenda()

    fun listar(): List<OrdemServico> =
        dao.listar()

    fun buscarPorId(id: Int): OrdemServico? =
        dao.buscarPorId(id)

    fun aFaturarDoCliente(clienteId: Int): List<OrdemServico> =
        dao.listarAFaturarDoCliente(clienteId)

    fun estaAFaturar(ordemId: Int): Boolean =
        dao.estaAFaturar(ordemId)

    private fun exigirOrdem(ordemId: Int): OrdemServico =
        dao.buscarPorId(ordemId)
            ?: throw IllegalArgumentException("Nao existe ordem de servico com o id $ordemId")

    private fun exigirTransicao(ordem: OrdemServico, destino: StatusOrdemServico) {
        require(destino in TRANSICOES[ordem.status].orEmpty()) {
            "A ordem ${ordem.id} esta ${ordem.status}: nao pode passar para $destino"
        }
    }

    private fun exigirFuncionarioAtivo(funcionarioId: Int) {
        val funcionario = funcionarios.buscarPorId(funcionarioId)
        require(funcionario != null) { "Nao existe funcionario com o id $funcionarioId" }
        require(funcionario.ativo) { "Funcionario demitido nao pode ser responsavel por ordem de servico" }
    }

    private fun validar(o: OrdemServico) {
        require(o.preco >= BigDecimal.ZERO) { "Preco nao pode ser negativo" }
        require(o.preco.stripTrailingZeros().scale() <= 2) { "Preco nao pode ter mais de 2 casas decimais" }
        require((o.observacao?.length ?: 0) <= 255) { "Observacao pode ter no maximo 255 caracteres" }
    }
}
