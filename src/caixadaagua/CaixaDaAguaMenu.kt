package caixadaagua

import enums.Cor
import enums.Formato
import enums.Material
import java.sql.Connection

fun menu(conn: Connection) {

    val dao = CaixaDaAguaDAO(conn)

    do {
        println("0 - SAIR")
        println("1 - CADASTRAR CAIXA DE AGUA")
        println("2 - REMOVER CAIXA DE AGUA")
        println("3 - ALTERAR CAIXA DE AGUA")
        println("4 - LISTAR CAIXA DE AGUA")

        val op = readln()

        when(op){
            "0" -> {
                println("Sistema encerrado")
            }
            "1" -> cadastrarNovaCaixa(dao)
            "2" -> print(2)
            "3" -> print(3)
            "4" -> print(4)
            else -> println("Opção inválida!")

        }
    } while(op != "0")
}

fun cadastrarNovaCaixa(dao: CaixaDaAguaDAO) {
    println("=== CADASTRAR CAIXA D'ÁGUA ===")

    println("Marca:")
    val marca = readln()

    println("Modelo:")
    val modelo = readln()

    println("Altura (m):")
    val altura = readln().toDouble()

    println("Largura (m):")
    val largura = readln().toDouble()

    println("Profundidade (m):")
    val profundidade = readln().toDouble()

    println("Cor (${Cor.entries.joinToString()}):")
    val cor = Cor.valueOf(readln().uppercase())

    println("Material (${Material.entries.joinToString()}):")
    val material = Material.valueOf(readln().uppercase())

    println("Formato (${Formato.entries.joinToString()}):")
    val formato = Formato.valueOf(readln().uppercase())

    println("Preço:")
    val preco = readln().toBigDecimal()

    val caixa = CaixaDaAgua(
        id = 0,
        marca = marca,
        modelo = modelo,
        altura = altura,
        largura = largura,
        profundidade = profundidade,
        cor = cor,
        material = material,
        formato = formato,
        preco = preco
    )

    val novoId = dao.insert(caixa)
    println("Caixa d'água cadastrada com sucesso! ID: $novoId")
}