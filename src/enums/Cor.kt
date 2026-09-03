package enums

enum class Cor(private val descricao: String) {

    AZUL("Azul"),
    CINZA("Cinza"),
    PRETA("Preta"),
    CASTANHO("Castanho"),
    PRATEADO("Prateado");

    override fun toString() = descricao
}