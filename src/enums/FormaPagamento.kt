package enums

enum class FormaPagamento(private val descricao: String) {

    DINHEIRO("Dinheiro"),
    PIX("Pix"),
    CREDITO("Credito"),
    DEBITO("Debito"),
    BOLETO("Boleto"),
    TRANSFERENCIA("Transferencia");

    override fun toString() = descricao
}