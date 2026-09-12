package enums

enum class TipoMovimento(private val descricao: String) {

    ENTRADA("Entrada"),
    SAIDA("Saida");

    val inverso: TipoMovimento
        get() = if (this == ENTRADA) SAIDA else ENTRADA

    override fun toString() = descricao
}
