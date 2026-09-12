package db

import java.sql.ResultSet
import java.sql.SQLException

inline fun <reified T : Enum<T>> ResultSet.enumDe(coluna: String): T {
    val bruto = getString(coluna)
    return enumValues<T>().firstOrNull { it.name == bruto }
        ?: throw SQLException(
            "Coluna $coluna tem '$bruto', que nao e valor de ${T::class.simpleName}. " +
                "Aceitos: ${enumValues<T>().joinToString { it.name }}"
        )
}

fun ResultSet.intOuNulo(coluna: String): Int? {
    val valor = getInt(coluna)
    return if (wasNull()) null else valor
}
