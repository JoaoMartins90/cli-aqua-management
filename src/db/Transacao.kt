package db

import java.sql.Connection

fun <T> Connection.emTransacao(bloco: () -> T): T {
    if (!autoCommit) return bloco()

    autoCommit = false
    try {
        val resultado = bloco()
        commit()
        return resultado
    } catch (ex: Exception) {
        rollback()
        throw ex
    } finally {
        autoCommit = true
    }
}
