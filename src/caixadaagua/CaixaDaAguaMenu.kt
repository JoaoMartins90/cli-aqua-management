package caixadaagua

import enums.Cor
import enums.Formato
import enums.Material
import utils.lerBigDecimal
import utils.lerDouble
import utils.lerEnum
import utils.lerInt
import utils.lerTexto
import java.sql.Connection

fun menu(conn: Connection) {

    val dao = CaixaDaAguaDAO(conn)

    do {
        println("0 - VOLTAR AO MENU PRINCIPAL")
        println("1 - CADASTRAR CAIXA DE AGUA")
        println("2 - REMOVER CAIXA DE AGUA")
        println("3 - ALTERAR CAIXA DE AGUA")
        println("4 - LISTAR CAIXA DE AGUA")

        val op = readln()

        when(op){
            "0" -> {
                println("")
            }
            "1" -> cadastrarCaixa(dao)
            "2" -> removerCaixa(dao)
            "3" -> alterarCaixa(dao)
            "4" -> listarCaixas(dao)
            else -> println("Opção inválida!")

        }
    } while(op != "0")
}

private fun cadastrarCaixa(dao: CaixaDaAguaDAO) {
    println("=== CADASTRAR CAIXA DE AGUA ===")

    val caixa = CaixaDaAgua(
        marca = lerTexto("Marca:"),
        modelo = lerTexto("Modelo:"),
        altura = lerDouble("Altura (m):", min = 0.01),
        largura = lerDouble("Largura (m):", min = 0.01),
        profundidade = lerDouble("Profundidade (m):", min = 0.01),
        cor = lerEnum("Cor:", Cor.entries),
        material = lerEnum("Material:", Material.entries),
        formato = lerEnum("Formato:", Formato.entries),
        preco = lerBigDecimal("Preco:")
    )

    val novoId = dao.insert(caixa)
    if (novoId != null)
        println("Caixa d'água cadastrada com sucesso! ID: $novoId")
    else
        println("Nao foi possivel cadastrar a caixa")

}

private fun alterarCaixa(dao: CaixaDaAguaDAO) {
    println("=== ALTERAR CAIXA DE AGUA ===")


    println("Digite o Id da caixa que deseja alterar:")
    println("Ids: ${dao.listarIdsCaixas()}")
    val id = readln().toIntOrNull()
    if (id == null) {
        println("Id invalido")
        return
    }

    val caixa = dao.buscarPorId(id)
    if (caixa == null) {
        println("Caixa não encontrada")
        return
    }

    val alterada = caixa.copy(
        marca = lerTexto("Marca:"),
        modelo = lerTexto("Modelo:"),
        altura = lerDouble("Altura (m):", min = 0.01),
        largura = lerDouble("Largura (m):", min = 0.01),
        profundidade = lerDouble("Profundidade (m):", min = 0.01),
        cor = lerEnum("Cor:", Cor.entries),
        material = lerEnum("Material:", Material.entries),
        formato = lerEnum("Formato:", Formato.entries),
        preco = lerBigDecimal("Preco:")
    )

    if (dao.alterar(alterada))
        println("Caixa ${alterada.id} alterada com sucesso!")
    else
        println("Nao foi possivel atualizar a caixa")
}

private fun listarCaixas(dao: CaixaDaAguaDAO) {
    println("=== LISTA DE CAIXAS DE AGUA ===")

    val caixas: List<CaixaDaAgua> = dao.listar()

    caixas.forEach { c ->
        println("====================================")
        println("""
            Id: ${c.id}
            Marca: ${c.marca}
            Modelo: ${c.modelo}
            Altura: ${c.altura}
            Largura: ${c.largura}
            Profundidade: ${c.profundidade}
            Cor: ${c.cor.name}
            Material: ${c.material.name}
            Formato: ${c.formato.name}}
            Preco: ${c.preco}
        """.trimIndent()
        )
        println("====================================")
    }
}

private fun removerCaixa(dao: CaixaDaAguaDAO) {
    println("=== REMOVER CAIXA DE AGUA ===")

    val ids: List<Int> = dao.listarIdsCaixas()
    println("Lista de Ids: $ids")

    println("Escolha o Id da caixa que deseja remover:")

    val idRemovido = readln().toInt()

    while (true) {
        if (idRemovido in ids) {
            dao.remover(idRemovido)
            println("Caixa $idRemovido removido com sucesso!")
            return
        } else {
        println("Digite um Id existente")
        break
        }
    }
}