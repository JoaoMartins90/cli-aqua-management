package conferencia

import java.sql.SQLException

fun mostrarConferencia(service: ConferenciaService) {
    println("=== CONFERENCIA DO BANCO ===")

    try {
        val vendas = service.vendasDivergentes()
        val saldos = service.saldosDivergentes()

        if (vendas.isEmpty() && saldos.isEmpty()) {
            println("Tudo certo: total das vendas bate com os itens e saldo das contas bate com os movimentos.")
            return
        }

        vendas.forEach { v ->
            println(" >> Venda ${v.vendaId}: total gravado R\$ ${v.valorTotal}, soma dos itens R\$ ${v.somaItens}")
        }
        saldos.forEach { s ->
            println(
                " >> Conta ${s.contaId} (${s.descricao}): saldo gravado R\$ ${s.saldoGravado}, " +
                    "soma dos movimentos R\$ ${s.saldoCalculado}"
            )
        }
        println(" >> Existem dados divergentes: alguma gravação foi feita fora do sistema ou pela metade.")
    } catch (ex: SQLException) {
        println(" >> Não foi possível conferir o banco: ${ex.message}")
    }
}
