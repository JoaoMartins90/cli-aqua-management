package enums

enum class Material(val descricao: String) {

    POLIETILENO("Polietileno"),
    FIBRA_DE_VIDRO("Fibra de Vidro"),
    ACO_INOXIDAVEL("Aco Inoxidavel"),
    CIMENTO_AMIANTO("Cimento amianto");

    override fun toString() = descricao
}