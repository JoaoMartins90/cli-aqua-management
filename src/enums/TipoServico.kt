package enums

enum class TipoServico(private val descricao: String) {

    INSTALACAO("Instalação"),
    LIMPEZA("Limpeza"),
    MANUTENCAO("Manutenção"),
    TROCA("Troca");

    override fun toString() = descricao
}