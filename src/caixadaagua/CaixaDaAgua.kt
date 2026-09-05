package caixadaagua

import enums.Cor
import enums.Formato
import enums.Material
import java.math.BigDecimal
import java.time.LocalDateTime

data class CaixaDaAgua(
    val id: Int? = null,
    val marca: String,
    val modelo: String,
    val capacidade: Int,
    val altura: Double,
    val largura: Double,
    val profundidade: Double,
    val cor: Cor,
    val material: Material,
    val formato: Formato,
    val preco: BigDecimal,
    val estoqueAtual: Int,
    val criadoEm: LocalDateTime = LocalDateTime.now()
)
