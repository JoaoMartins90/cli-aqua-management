package enums

enum class FormaPagamento(val descricao: String) {

    DINHEIRO("Dinheiro"),
    PIX("Pix"),
    CREDITO("Credito"),
    DEBITO("Debito"),
    BOLETO("Boleto"),
    TRANSFERENCIA("Transferencia");

    override fun toString() = descricao
}