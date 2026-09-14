package utils

import java.math.BigDecimal

fun BigDecimal.temAteDuasCasas(): Boolean =
    stripTrailingZeros().scale() <= 2
