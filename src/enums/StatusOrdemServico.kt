package enums

enum class StatusOrdemServico(private val descricao: String) {

    AGENDADO("Agendado"),
    EM_EXECUCAO("Em execução"),
    CONCLUIDO("Concluido"),
    CANCELADO("Cancelado");

    override fun toString() = descricao
}