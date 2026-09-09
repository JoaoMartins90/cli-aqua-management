package utils

import java.math.BigDecimal
import kotlin.system.exitProcess

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

fun lerTexto(rotulo: String, obrigatorio: Boolean = true): String =
    lerAte(rotulo, "Esse campo nao pode ser vazio") { texto ->
        texto.ifBlank { if (obrigatorio) null else "" }
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

fun <T> lerEnum(rotulo: String, valores: List<T>): T {
    println(rotulo)
    valores.forEachIndexed { i, v -> println(" ${i + 1} - $v") }
    return valores[lerInt("Escolha:", 1, valores.size) - 1]
}