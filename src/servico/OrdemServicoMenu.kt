package servico

import enums.StatusOrdemServico
import enums.TipoServico
import pessoa.PessoaService
import pessoa.escolherFuncionarioAtivo
import pessoa.identificarCliente
import utils.escolher
import utils.lerBigDecimal
import utils.lerDataHora
import utils.lerEnum
import utils.lerOpcao
import utils.lerSimNao
import utils.lerTexto
import java.sql.SQLException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

fun menuOrdemServico(ordens: OrdemServicoService, pessoas: PessoaService) {

    do {
        println("0 - VOLTAR A OPERACAO")
        println("1 - NOVA ORDEM DE SERVICO")
        println("2 - AGENDA (ORDENS EM ABERTO)")
        println("3 - LISTAR TODAS AS ORDENS")
        println("4 - DESIGNAR RESPONSAVEL")
        println("5 - REAGENDAR ORDEM")
        println("6 - INICIAR ORDEM")
        println("7 - CONCLUIR ORDEM")
        println("8 - CANCELAR ORDEM")

        val op = lerOpcao()

        try {
            when (op) {
                "0" -> {}
                "1" -> nova(ordens, pessoas)
                "2" -> agenda(ordens)
                "3" -> listar(ordens, pessoas)
                "4" -> designar(ordens, pessoas)
                "5" -> reagendar(ordens, pessoas)
                "6" -> iniciar(ordens, pessoas)
                "7" -> concluir(ordens, pessoas)
                "8" -> cancelar(ordens, pessoas)
                else -> println("Opção inválida!")
            }
        } catch (ex: IllegalArgumentException) {
            println(" >> ${ex.message}")
        } catch (ex: IllegalStateException) {
            println(" >> ${ex.message}")
        } catch (ex: SQLException) {
            println(" >> Erro de banco: ${ex.message}")
        }
    } while (op != "0")
}

private fun nova(ordens: OrdemServicoService, pessoas: PessoaService) {
    println("=== NOVA ORDEM DE SERVICO ===")

    val clienteId = identificarCliente(pessoas) ?: return
    val tipo = lerEnum("Serviço:", TipoServico.entries)
    val preco = lerBigDecimal("Preço combinado:")
    val data = lerDataHora("Data e hora agendadas (dd/mm/aaaa hh:mm):")
    val responsavel =
        if (lerSimNao("Já designar o responsável?")) escolherFuncionarioAtivo(pessoas, "Responsável:")
        else null
    val observacao = lerTexto("Observação (Enter para pular):", obrigatorio = false).ifBlank { null }

    val id = ordens.abrir(
        OrdemServico(
            tipoServico = tipo,
            clienteId = clienteId,
            funcionarioId = responsavel,
            preco = preco,
            dataAgendada = data,
            observacao = observacao
        )
    )
    println("Ordem $id agendada para ${data.format(FORMATO)}")
}

private fun agenda(ordens: OrdemServicoService) {
    println("=== AGENDA - ORDENS EM ABERTO ===")

    val linhas = ordens.agenda()
    if (linhas.isEmpty()) {
        println("Nenhuma ordem em aberto")
        return
    }

    val agora = LocalDateTime.now()
    linhas.forEach { l ->
        val atrasada =
            if (l.status == StatusOrdemServico.AGENDADO && l.dataAgendada.isBefore(agora)) " - ATRASADA"
            else ""
        println("${l.dataAgendada.format(FORMATO)} | Ordem ${l.ordemId} | ${l.servico} | ${l.status}$atrasada")
        println(
            "    Cliente: ${l.cliente} (${l.telefoneCliente}) | " +
                "Responsável: ${l.responsavel ?: "a designar"} | R\$ ${l.preco}"
        )
    }
}

private fun listar(ordens: OrdemServicoService, pessoas: PessoaService) {
    println("=== ORDENS DE SERVICO ===")

    val lista = ordens.listar()
    if (lista.isEmpty()) {
        println("Nenhuma ordem de serviço registrada")
        return
    }

    lista.forEach { o ->
        println("====================================")
        println(
            """
            Ordem: ${o.id}
            Serviço: ${o.tipoServico}
            Cliente: ${pessoas.nomeDoCliente(o.clienteId)}
            Responsável: ${o.funcionarioId?.let { pessoas.nomeDoFuncionario(it) } ?: "a designar"}
            Status: ${o.status}
            Agendada para: ${o.dataAgendada.format(FORMATO)}
            Concluída em: ${o.dataConclusao?.format(FORMATO) ?: "-"}
            Cobrança: ${descreverCobranca(ordens, o)}
            Preço: R${'$'} ${o.preco}
            Observação: ${o.observacao ?: "-"}
            """.trimIndent()
        )
    }
    println("====================================")
}

private fun designar(ordens: OrdemServicoService, pessoas: PessoaService) {
    println("=== DESIGNAR RESPONSAVEL ===")

    val ordem = escolherOrdem(ordens, pessoas, "Nenhuma ordem em aberto") { it.emAberto } ?: return
    val funcionarioId = escolherFuncionarioAtivo(pessoas, "Responsável:") ?: return

    if (ordens.designarResponsavel(ordem.id!!, funcionarioId))
        println("Ordem ${ordem.id} agora é de ${pessoas.nomeDoFuncionario(funcionarioId)}")
    else println("Não foi possível designar o responsável da ordem ${ordem.id}")
}

private fun reagendar(ordens: OrdemServicoService, pessoas: PessoaService) {
    println("=== REAGENDAR ORDEM ===")

    val ordem = escolherOrdem(ordens, pessoas, "Nenhuma ordem em aberto") { it.emAberto } ?: return
    println("Agendamento atual: ${ordem.dataAgendada.format(FORMATO)}")
    val data = lerDataHora("Nova data e hora (dd/mm/aaaa hh:mm):")

    if (ordens.reagendar(ordem.id!!, data)) println("Ordem ${ordem.id} reagendada para ${data.format(FORMATO)}")
    else println("Não foi possível reagendar a ordem ${ordem.id}")
}

private fun iniciar(ordens: OrdemServicoService, pessoas: PessoaService) {
    println("=== INICIAR ORDEM ===")

    val ordem = escolherOrdem(ordens, pessoas, "Nenhuma ordem agendada") {
        it.status == StatusOrdemServico.AGENDADO
    } ?: return

    val responsavel = ordem.funcionarioId
        ?.also { println("Responsável: ${pessoas.nomeDoFuncionario(it)}") }
        ?: escolherFuncionarioAtivo(pessoas, "A ordem não tem responsável. Quem vai executar?")
        ?: return

    if (ordens.iniciar(ordem.id!!, responsavel)) println("Ordem ${ordem.id} em execução")
    else println("Não foi possível iniciar a ordem ${ordem.id}")
}

private fun concluir(ordens: OrdemServicoService, pessoas: PessoaService) {
    println("=== CONCLUIR ORDEM ===")

    val ordem = escolherOrdem(ordens, pessoas, "Nenhuma ordem em execução") {
        it.status == StatusOrdemServico.EM_EXECUCAO
    } ?: return

    val agora = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
    val data = lerDataHora("Data e hora da conclusão (Enter para agora):", agora)

    if (ordens.concluir(ordem.id!!, data))
        println("Ordem ${ordem.id} concluída. Ela já pode ser cobrada numa venda.")
    else println("Não foi possível concluir a ordem ${ordem.id}")
}

private fun cancelar(ordens: OrdemServicoService, pessoas: PessoaService) {
    println("=== CANCELAR ORDEM ===")

    val ordem = escolherOrdem(ordens, pessoas, "Nenhuma ordem em aberto") { it.emAberto } ?: return

    if (!lerSimNao("Confirmar o cancelamento da ordem ${ordem.id}?")) {
        println("Nada foi alterado.")
        return
    }

    if (ordens.cancelar(ordem.id!!)) println("Ordem ${ordem.id} cancelada")
    else println("Não foi possível cancelar a ordem ${ordem.id}")
}

private fun escolherOrdem(
    ordens: OrdemServicoService,
    pessoas: PessoaService,
    vazio: String,
    filtro: (OrdemServico) -> Boolean
): OrdemServico? {
    val candidatas = ordens.listar().filter(filtro)
    if (candidatas.isEmpty()) {
        println(vazio)
        return null
    }
    return escolher("Ordem:", candidatas) { descrever(it, pessoas) }
}

private fun descrever(o: OrdemServico, pessoas: PessoaService) =
    "Ordem ${o.id} - ${o.tipoServico} - ${pessoas.nomeDoCliente(o.clienteId)} - " +
        "${o.dataAgendada.format(FORMATO)} - ${o.status}"

private fun descreverCobranca(ordens: OrdemServicoService, o: OrdemServico) = when {
    o.status != StatusOrdemServico.CONCLUIDO -> "-"
    ordens.estaAFaturar(o.id!!) -> "a cobrar"
    else -> "cobrada em venda"
}
