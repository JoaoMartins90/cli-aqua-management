package enums

enum class StatusVenda(private val descricao: String) {

    EFETIVADA("Efetivada"),
    CANCELADA("Cancelada");

    override fun toString() = descricao
}