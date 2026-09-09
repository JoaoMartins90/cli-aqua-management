package caixadaagua

import enums.Cor
import enums.Formato
import enums.Material
import utils.lerBigDecimal
import utils.lerDouble
import utils.lerEnum
import utils.lerInt
import utils.lerTexto
import java.sql.SQLException

fun menuCaixa(service: CaixaDaAguaService) {

    do {
        println("0 - VOLTAR AO MENU PRINCIPAL")
        println("1 - CADASTRAR CAIXA DE AGUA")
        println("2 - REMOVER CAIXA DE AGUA")
        println("3 - ALTERAR CAIXA DE AGUA")
        println("4 - LISTAR CAIXA DE AGUA")

        val op = readln()

        try {
            when (op) {
                "0" -> {}
                "1" -> cadastrar(service)
                "2" -> remover(service)
                "3" -> alterar(service)
                "4" -> listar(service)
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

private fun cadastrar(service: CaixaDaAguaService) {
    println("=== CADASTRAR CAIXA DE AGUA ===")

    val caixa = CaixaDaAgua(
        marca = lerTexto("Marca:"),
        modelo = lerTexto("Modelo:"),
        capacidade = lerInt("Capacidade (l):", min = 1),
        altura = lerDouble("Altura (m):", min = 0.01),
        largura = lerDouble("Largura (m):", min = 0.01),
        profundidade = lerDouble("Profundidade (m):", min = 0.01),
        cor = lerEnum("Cor:", Cor.entries),
        material = lerEnum("Material:", Material.entries),
        formato = lerEnum("Formato:", Formato.entries),
        preco = lerBigDecimal("Preco:"),
        estoqueAtual = lerInt("Estoque:", min = 0)
    )
    println("Caixa cadastrada com sucesso ID: ${service.cadastrar(caixa)}")

}

private fun alterar(service: CaixaDaAguaService) {
    println("=== ALTERAR CAIXA DE AGUA ===")

    val id = escolherId(service, "alterar") ?: return

    val caixa = service.buscarPorId(id)
    if (caixa == null) {
        println("Caixa nao encontrada")
        return
    }

    val alterada = caixa.copy(
        marca = lerTexto("Marca:"),
        modelo = lerTexto("Modelo:"),
        capacidade = lerInt("Capacidade (l):", min = 1),
        altura = lerDouble("Altura (m):", min = 0.01),
        largura = lerDouble("Largura (m):", min = 0.01),
        profundidade = lerDouble("Profundidade (m):", min = 0.01),
        cor = lerEnum("Cor:", Cor.entries),
        material = lerEnum("Material:", Material.entries),
        formato = lerEnum("Formato:", Formato.entries),
        preco = lerBigDecimal("Preco:"),
        estoqueAtual = lerInt("Estoque:", min = 0)
    )

    if (service.alterar(alterada)) println("Caixa $id alterada com sucesso")
    else println("Nao foi possivel alterar a caixa $id")
}

private fun remover(service: CaixaDaAguaService) {
    println("=== REMOVER CAIXA DE AGUA ===")

    val id = escolherId(service, "remover") ?: return

    if (service.remover(id)) println("Caixa $id removida com sucesso")
    else println("Nao foi possivel remover a caixa $id")
}

private fun listar(service: CaixaDaAguaService) {
    println("=== LISTA DE CAIXAS DE AGUA ===")

    val caixas = service.listar()
    if (caixas.isEmpty()) {
        println("Nenhuma caixa cadastrada")
        return
    }

    caixas.forEach { c ->
        println("====================================")
        println(
            """
            Id: ${c.id}
            Marca: ${c.marca}
            Modelo: ${c.modelo}
            Capacidade (l): ${c.capacidade}
            Altura (m): ${c.altura}
            Largura (m): ${c.largura}
            Profundidade (m): ${c.profundidade}
            Cor: ${c.cor}
            Material: ${c.material}
            Formato: ${c.formato}
            Preco: ${c.preco}
            Estoque: ${c.estoqueAtual}
            Criado em: ${c.criadoEm}
            """.trimIndent()
        )
    }
    println("====================================")
}

private fun escolherId(service: CaixaDaAguaService, acao: String): Int? {
    val ids = service.idsExistentes()
    if (ids.isEmpty()) {
        println("Nenhuma caixa cadastrada")
        return null
    }

    println("Ids disponiveis: $ids")
    val id = lerInt("Id da caixa que deseja: $acao")

    if (id !in ids) {
        println("Id inexistente")
        return null
    }
    return id
}