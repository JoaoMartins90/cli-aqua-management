package db

import java.sql.Connection
import java.sql.SQLException

fun <T> Connection.emTransacao(bloco: () -> T): T {
    if (!autoCommit) return bloco()

    autoCommit = false
    try {
        val resultado = bloco()
        commit()
        return resultado
    } catch (ex: Throwable) {
        try {
            rollback()
        } catch (erroRollback: SQLException) {
            ex.addSuppressed(erroRollback)
        }
        throw ex
    } finally {
        autoCommit = true
    }
}
