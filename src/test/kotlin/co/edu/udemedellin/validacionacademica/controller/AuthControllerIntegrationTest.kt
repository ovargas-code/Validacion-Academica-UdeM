package co.edu.udemedellin.validacionacademica.controller

import co.edu.udemedellin.validacionacademica.bootstrap.ValidacionAcademicaApplication
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional

@SpringBootTest(classes = [ValidacionAcademicaApplication::class])
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `POST login con credenciales validas retorna 200 y JWT`() {
        val body = """{"username": "admin", "password": "udem2026"}"""

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").isNotEmpty)
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.expiresIn").isNumber)
    }

    @Test
    fun `POST login con password incorrecta retorna 401`() {
        val body = """{"username": "admin", "password": "wrong-password"}"""

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("Credenciales incorrectas"))
    }

    @Test
    fun `POST login con usuario inexistente retorna 401`() {
        val body = """{"username": "noexiste", "password": "cualquier"}"""

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `POST login con campos vacios retorna 400`() {
        val body = """{"username": "", "password": ""}"""

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `POST login con password demasiado larga retorna 400`() {
        val longPassword = "a".repeat(129)
        val body = """{"username": "admin", "password": "$longPassword"}"""

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        ).andExpect(status().isBadRequest)
    }
}
