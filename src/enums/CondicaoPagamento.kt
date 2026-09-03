package enums

enum class CondicaoPagamento(private val descricao: String) {

    A_VISTA("A vista"),
    A_PRAZO("A prazo");

    override fun toString() = descricao
}