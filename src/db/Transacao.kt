package db

import java.sql.Connection

fun <T> Connection.emTransacao(bloco: () -> T): T {
    val anterior = autoCommit
    autoCommit = false
    try {
        val resultado = bloco()
        commit()
        return resultado
    } catch (ex: Exception) {
        rollback()
        throw ex
    } finally {
        autoCommit = anterior
    }
}