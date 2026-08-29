package caixadaagua

import enums.Cor
import enums.Formato
import enums.Material
import utils.lerBigDecimal
import utils.lerDouble
import utils.lerEnum
import utils.lerTexto
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