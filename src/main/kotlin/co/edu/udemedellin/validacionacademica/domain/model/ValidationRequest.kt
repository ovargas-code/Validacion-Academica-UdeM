package co.edu.udemedellin.validacionacademica.domain.model

import com.fasterxml.jackson.annotation.JsonIgnore
import java.time.LocalDateTime

data class ValidationRequest(
    @JsonIgnore // <-- Swagger ignorará este campo
    val id: Long? = null,

    val requesterName: String,
    val requesterEmail: String,
    val studentDocument: String,
    val validationType: ValidationType,

    @JsonIgnore // <-- Swagger ignorará este campo
    val verificationCode: String = "",

    @JsonIgnore // <-- Swagger ignorará este campo
    val createdAt: LocalDateTime = LocalDateTime.now()
)