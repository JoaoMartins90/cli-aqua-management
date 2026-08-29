package caixadaagua

import enums.Cor
import enums.Formato
import enums.Material
import java.math.BigDecimal

data class CaixaDaAgua(
    val id: Int? = null,
    val marca: String,
    val modelo: String,
    val altura: Double,
    val largura: Double,
    val profundidade: Double,
    val cor: Cor,
    val material: Material,
    val formato: Formato,
    val preco: BigDecimal
)
