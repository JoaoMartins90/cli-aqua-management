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

fun lerBigDecimal(rotulo: String, min: BigDecimal = BigDecimal.ZERO): BigDecimal =
    lerAte(rotulo, "Digite um valor válido (ex.: 199,90).") { texto ->
        normalizar(texto).toBigDecimalOrNull()?.takeIf { it >= min }
    }

fun lerData(rotulo: String, padrao: LocalDate? = null): LocalDate =
    lerAte(rotulo, "Digite uma data válida (ex.: 31/12/2025).") { texto ->
        if (texto.isBlank()) padrao
        else runCatching { LocalDate.parse(texto, FORMATO_DATA) }.getOrNull()
    }

fun <T> lerEnum(rotulo: String, valores: List<T>): T {
    println(rotulo)
    valores.forEachIndexed { i, v -> println(" ${i + 1} - $v") }
    return valores[lerInt("Escolha:", 1, valores.size) - 1]
}
