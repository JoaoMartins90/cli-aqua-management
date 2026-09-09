package pessoa

import enums.Setor
import utils.lerBigDecimal
import utils.lerData
import utils.lerDigitos
import utils.lerEnum
import utils.lerInt
import utils.lerTexto
import java.math.BigDecimal
import java.sql.SQLException
import java.time.LocalDate

private const val CLIENTE = "CLIENTE"
private const val FUNCIONARIO = "FUNCIONARIO"

fun menuPessoa(service: PessoaService) {

    do {
        println("0 - VOLTAR AO MENU PRINCIPAL")
        println("1 - CADASTRAR PESSOA / ADICIONAR PAPEL")
        println("2 - LISTAR PESSOAS")
        println("3 - ALTERAR PESSOA")
        println("4 - REMOVER PESSOA")

        val op = readln()

        try {
            when (op) {
                "0" -> {}
                "1" -> cadastrar(service)
                "2" -> listar(service)
                "3" -> alterar(service)
                "4" -> remover(service)
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

private fun cadastrar(service: PessoaService) {
    println("=== CADASTRAR PESSOA / ADICIONAR PAPEL ===")

    val cpfCnpj = lerDigitos("CPF ou CNPJ:", listOf(11, 14))
    val existente = service.buscarPorCpfCnpj(cpfCnpj)

    val pessoaId = if (existente == null) {
        println("Documento novo. Vamos cadastrar a pessoa.")
        service.cadastrar(
            Pessoa(
                nome = lerTexto("Nome:", minimo = 3),
                cpfCnpj = cpfCnpj,
                telefone = lerDigitos("Telefone com DDD:", listOf(10, 11))
            )
        )
    } else {
        println("Documento já cadastrado: ${existente.nome}")
        existente.id!!
    }

    adicionarPapel(service, pessoaId)
}

private fun adicionarPapel(service: PessoaService, pessoaId: Int) {
    val papeis = service.papeis(pessoaId)
    println("Papéis atuais: ${descreverPapeis(papeis)}")

    val disponiveis = listOf(CLIENTE, FUNCIONARIO) - papeis.toSet()
    if (disponiveis.isEmpty()) {
        println("Essa pessoa já é cliente e funcionário. Nada a adicionar.")
        return
    }

    when (lerEnum("Papel a adicionar:", disponiveis + "NENHUM")) {
        CLIENTE -> {
            val id = service.tornarCliente(pessoaId, lerBigDecimal("Limite de crédito:"))
            println("Cliente $id criado, com conta aberta")
        }
        FUNCIONARIO -> {
            val id = service.tornarFuncionario(
                pessoaId,
                lerEnum("Setor:", Setor.entries),
                lerBigDecimal("Salário:", BigDecimal("0.01")),
                lerData("Data de admissão (dd/mm/aaaa, Enter para hoje):", LocalDate.now())
            )
            println("Funcionário $id criado")
        }
        else -> println("Nenhum papel adicionado")
    }
}

private fun listar(service: PessoaService) {
    println("=== LISTA DE PESSOAS ===")

    val pessoas = service.listar()
    if (pessoas.isEmpty()) {
        println("Nenhuma pessoa cadastrada")
        return
    }

    pessoas.forEach { p ->
        println("====================================")
        println(
            """
            Id: ${p.id}
            Nome: ${p.nome}
            CPF/CNPJ: ${p.cpfCnpj}
            Telefone: ${p.telefone}
            Papéis: ${descreverPapeis(service.papeis(p.id!!))}
            Conta: ${if (service.temConta(p.id)) "sim" else "não"}
            Criado em: ${p.criadoEm}
            """.trimIndent()
        )
    }
    println("====================================")
}

private fun alterar(service: PessoaService) {
    println("=== ALTERAR PESSOA ===")

    val id = escolherId(service, "alterar") ?: return

    val pessoa = service.buscarPorId(id)
    if (pessoa == null) {
        println("Pessoa não encontrada")
        return
    }

    val alterada = pessoa.copy(
        nome = lerTexto("Nome:", minimo = 3),
        cpfCnpj = lerDigitos("CPF ou CNPJ:", listOf(11, 14)),
        telefone = lerDigitos("Telefone com DDD:", listOf(10, 11))
    )

    if (service.alterar(alterada)) println("Pessoa $id alterada com sucesso")
    else println("Não foi possível alterar a pessoa $id")
}

private fun remover(service: PessoaService) {
    println("=== REMOVER PESSOA ===")

    val id = escolherId(service, "remover") ?: return

    if (service.remover(id)) println("Pessoa $id removida com sucesso")
    else println("Não foi possível remover a pessoa $id")
}

private fun escolherId(service: PessoaService, acao: String): Int? {
    val ids = service.idsExistentes()
    if (ids.isEmpty()) {
        println("Nenhuma pessoa cadastrada")
        return null
    }

    println("Ids disponíveis: $ids")
    val id = lerInt("Id da pessoa que deseja $acao:")

    if (id !in ids) {
        println("Id inexistente")
        return null
    }
    return id
}

private fun descreverPapeis(papeis: List<String>) =
    if (papeis.isEmpty()) "nenhum" else papeis.joinToString(", ")
