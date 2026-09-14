package financeiro

import enums.FormaPagamento
import enums.TipoMovimento
import pessoa.PessoaService
import pessoa.escolherFuncionarioAtivo
import servico.OrdemServicoService
import utils.escolher
import utils.lerBigDecimal
import utils.lerEnum
import utils.lerMesAno
import utils.lerOpcao
import utils.lerSimNao
import java.math.BigDecimal
import java.sql.SQLException
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val FORMATO_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
private val CENTAVO = BigDecimal("0.01")

fun menuFinanceiro(financeiro: FinanceiroService, ordens: OrdemServicoService, pessoas: PessoaService) {

    do {
        println("0 - VOLTAR AO MENU PRINCIPAL")
        println("1 - RECEBER PAGAMENTO")
        println("2 - CONTAS A RECEBER")
        println("3 - SERVICOS A FATURAR")
        println("4 - EXTRATO E SALDO")
        println("5 - PAGAR SALARIO")
        println("6 - ESTORNAR MOVIMENTO")

        val op = lerOpcao()

        try {
            when (op) {
                "0" -> {}
                "1" -> receber(financeiro)
                "2" -> contasAReceber(financeiro)
                "3" -> servicosAFaturar(ordens, pessoas)
                "4" -> extrato(financeiro, pessoas)
                "5" -> pagarSalario(financeiro, pessoas)
                "6" -> estornar(financeiro)
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

private fun receber(financeiro: FinanceiroService) {
    println("=== RECEBER PAGAMENTO ===")

    val abertas = financeiro.contasAReceber()
    if (abertas.isEmpty()) {
        println("Nenhuma venda em aberto")
        return
    }

    val aReceber = escolher("Venda:", abertas) {
        "Venda ${it.vendaId} - ${it.cliente} - ${it.dataVenda.format(FORMATO_DATA)} - deve R\$ ${it.saldoDevedor}"
    }

    val valor = lerBigDecimal(
        "Valor recebido (Enter para ${aReceber.saldoDevedor}):",
        CENTAVO,
        aReceber.saldoDevedor
    )
    val forma = lerEnum("Forma de pagamento:", FormaPagamento.entries)

    financeiro.receber(aReceber.vendaId, valor, forma)

    val recebido = valor.setScale(2)
    val restante = aReceber.saldoDevedor - recebido
    if (restante > BigDecimal.ZERO)
        println("Recebido R\$ $recebido. A venda ${aReceber.vendaId} ainda deve R\$ $restante")
    else println("Recebido R\$ $recebido. Venda ${aReceber.vendaId} quitada")
}

private fun contasAReceber(financeiro: FinanceiroService) {
    println("=== CONTAS A RECEBER ===")

    val abertas = financeiro.contasAReceber()
    if (abertas.isEmpty()) {
        println("Nenhuma venda em aberto")
        return
    }

    abertas.forEach { c ->
        println(
            "Venda ${c.vendaId} | ${c.dataVenda.format(FORMATO_DATA)} | ${c.cliente} (${c.telefone}) | " +
                "total R\$ ${c.valorTotal} | pago R\$ ${c.pago} | deve R\$ ${c.saldoDevedor}"
        )
    }
    println("TOTAL A RECEBER: R\$ ${abertas.sumOf { it.saldoDevedor }}")
}

private fun servicosAFaturar(ordens: OrdemServicoService, pessoas: PessoaService) {
    println("=== SERVICOS A FATURAR ===")

    val lista = ordens.aFaturar()
    if (lista.isEmpty()) {
        println("Nenhum serviço concluído esperando cobrança")
        return
    }

    lista.forEach { o ->
        println(
            "Ordem ${o.id} | ${o.tipoServico} | ${pessoas.nomeDoCliente(o.clienteId)} | " +
                "concluída em ${o.dataConclusao?.format(FORMATO_DATA)} | R\$ ${o.preco}"
        )
    }
    println("TOTAL A FATURAR: R\$ ${lista.sumOf { it.preco }}")
    println("Para cobrar, lance a ordem numa venda em OPERACAO > VENDAS.")
}

private fun extrato(financeiro: FinanceiroService, pessoas: PessoaService) {
    val conta = financeiro.contaDaLoja()
    println("=== EXTRATO - ${conta.descricao.uppercase()} ===")

    val movimentos = financeiro.extratoDaLoja()
    if (movimentos.isEmpty()) println("Nenhum movimento")

    movimentos.forEach { m ->
        val sinal = if (m.tipo == TipoMovimento.ENTRADA) "+" else "-"
        val contraparte = m.pessoaId?.let { " | ${pessoas.nomeDe(it)}" } ?: ""
        val estorno = m.estornoDeId?.let { " (estorno do $it)" } ?: ""
        println(
            "${m.dataMovimentacao.format(FORMATO_DATA_HORA)} | #${m.id} | $sinal R\$ ${m.valor} | " +
                "${m.formaPagamento} | ${m.descricao}$contraparte$estorno"
        )
    }
    println("SALDO: R\$ ${conta.saldo}")
}

private fun pagarSalario(financeiro: FinanceiroService, pessoas: PessoaService) {
    println("=== PAGAR SALARIO ===")

    val funcionarioId = escolherFuncionarioAtivo(pessoas, "Funcionário:") ?: return
    val salario = financeiro.salarioDe(funcionarioId)

    val valor = lerBigDecimal("Valor (Enter para $salario):", CENTAVO, salario)
    val competencia = lerMesAno("Mês de referência (mm/aaaa, Enter para o mês atual):", YearMonth.now())
    val forma = lerEnum("Forma de pagamento:", FormaPagamento.entries)

    val saldo = financeiro.contaDaLoja().saldo
    println("Saldo da loja: R\$ $saldo. Depois do pagamento: R\$ ${saldo - valor}")
    if (!lerSimNao("Confirmar pagamento de R\$ $valor a ${pessoas.nomeDoFuncionario(funcionarioId)}?")) {
        println("Nada foi gravado.")
        return
    }

    financeiro.pagarSalario(funcionarioId, valor, forma, competencia)
    println("Salário pago")
}

private fun estornar(financeiro: FinanceiroService) {
    println("=== ESTORNAR MOVIMENTO ===")

    val candidatos = financeiro.movimentosEstornaveis()
    if (candidatos.isEmpty()) {
        println("Nenhum movimento para estornar")
        return
    }

    val movimento = escolher("Movimento:", candidatos) {
        "#${it.id} - ${it.dataMovimentacao.format(FORMATO_DATA)} - ${it.tipo} R\$ ${it.valor} - ${it.descricao}"
    }

    println("O estorno grava um movimento contrário. O original continua no extrato.")
    if (!lerSimNao("Confirmar o estorno do movimento ${movimento.id}?")) {
        println("Nada foi alterado.")
        return
    }

    val id = financeiro.estornar(movimento.id!!)
    println("Estorno gravado como movimento $id")
}
