package enums

enum class TipoMovimento(private val descricao: String) {

    ENTRADA("Entrada"),
    SAIDA("Saida");

    override fun toString() = descricao
}