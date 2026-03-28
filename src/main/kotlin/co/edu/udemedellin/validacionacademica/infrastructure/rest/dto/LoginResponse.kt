package co.edu.udemedellin.validacionacademica.infrastructure.rest.dto

data class LoginResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long
)
