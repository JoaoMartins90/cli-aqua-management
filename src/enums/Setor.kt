package enums

enum class Setor(private val descricao: String) {

    VENDAS("Vendas"),
    INSTALACAO("Instalacao"),
    ADMINISTRATIVO("Administrativo");

    override fun toString() = descricao
}