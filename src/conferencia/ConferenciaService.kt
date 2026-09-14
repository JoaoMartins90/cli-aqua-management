package conferencia

class ConferenciaService(
    private val dao: ConferenciaDAO
) {
    fun vendasDivergentes(): List<DivergenciaVenda> =
        dao.listarVendasDivergentes()

    fun saldosDivergentes(): List<DivergenciaSaldo> =
        dao.listarSaldosDivergentes()
}
