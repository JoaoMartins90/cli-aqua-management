package pessoa

import java.time.LocalDateTime

data class Pessoa(
    val id: Int? = null,
    val nome: String,
    val cpfCnpj: String,
    val telefone: String,
    val criadoEm: LocalDateTime = LocalDateTime.now()
)
