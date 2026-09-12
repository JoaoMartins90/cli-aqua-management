package utils

import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.system.exitProcess

private val FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy")

private fun lerLinha(rotulo: String): String {
    print("$rotulo ")
    return readLine() ?: run {
        println("\nEntrada encerrada.")
        exitProcess(0)
    }
}

private fun <T> lerAte(rotulo: String, erro: String, converter: (String) -> T?): T {
    while(true) {
        converter(lerLinha(rotulo).trim())?.let { return it }
        println(" >> $erro")
    }
}

private fun normalizar(texto: String) =
    if (',' in texto) texto.replace(',', '.') else texto

fun lerTexto(rotulo: String, obrigatorio: Boolean = true, minimo: Int = 1): String =
    lerAte(rotulo, "Esse campo precisa de pelo menos $minimo caractere(s)") { texto ->
        if (texto.isBlank()) (if (obrigatorio) null else "")
        else texto.takeIf { it.length >= minimo }
    }

fun lerDigitos(rotulo: String, tamanhos: List<Int>): String =
    lerAte(rotulo, "Digite apenas números, com ${tamanhos.joinToString(" ou ")} dígitos.") { texto ->
        texto.filter { it.isDigit() }.takeIf { it.length in tamanhos }
    }

fun lerInt(rotulo: String, min: Int = Int.MIN_VALUE, max: Int = Int.MAX_VALUE): Int =
    lerAte(rotulo, "Digite um número inteiro entre $min e $max.") { texto ->
        texto.toIntOrNull()?.takeIf { it in min..max }
    }

fun lerDouble(rotulo: String, min: Double = 0.0, max: Double = Double.MAX_VALUE): Double =
    lerAte(rotulo, "Digite um número válido (ex.: 1,20).") { texto ->
        normalizar(texto).toDoubleOrNull()?.takeIf { it in min..max }
    }

fun lerBigDecimal(rotulo: String, min: BigDecimal = BigDecimal.ZERO, padrao: BigDecimal? = null): BigDecimal =
    lerAte(rotulo, "Digite um valor válido (ex.: 199,90).") { texto ->
        if (texto.isBlank()) padrao
        else normalizar(texto).toBigDecimalOrNull()?.takeIf { it >= min }
    }

fun lerData(rotulo: String, padrao: LocalDate? = null): LocalDate =
    lerAte(rotulo, "Digite uma data válida (ex.: 31/12/2025).") { texto ->
        if (texto.isBlank()) padrao
        else runCatching { LocalDate.parse(texto, FORMATO_DATA) }.getOrNull()
    }

fun lerSimNao(rotulo: String): Boolean =
    lerAte("$rotulo (s/n)", "Responda s ou n.") { texto ->
        when (texto.lowercase()) {
            "s", "sim" -> true
            "n", "nao", "não" -> false
            else -> null
        }
    }

fun <T> escolher(rotulo: String, opcoes: List<T>, descricao: (T) -> String): T {
    println(rotulo)
    opcoes.forEachIndexed { i, opcao -> println(" ${i + 1} - ${descricao(opcao)}") }
    return opcoes[lerInt("Escolha:", 1, opcoes.size) - 1]
}

fun <T> lerEnum(rotulo: String, valores: List<T>): T =
    escolher(rotulo, valores) { it.toString() }
