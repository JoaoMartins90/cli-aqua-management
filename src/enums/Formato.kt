package enums

enum class Formato(private val descricao: String) {

    CILINDRICA("Cilindrica"),
    TRONCO_CONICA("Tronco-Conica"),
    QUADRADA("Quadrada"),
    RETANGULAR("Retangular");

    override fun toString() = descricao
}