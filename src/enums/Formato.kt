package enums

enum class Formato(private val descricao: String) {

    QUADRADA("Quadrada");

    override fun toString() = descricao
}