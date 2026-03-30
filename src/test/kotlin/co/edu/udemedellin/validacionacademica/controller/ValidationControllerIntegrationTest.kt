package co.edu.udemedellin.validacionacademica.controller

import co.edu.udemedellin.validacionacademica.bootstrap.ValidacionAcademicaApplication
import co.edu.udemedellin.validacionacademica.domain.ports.MailPort
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional

@SpringBootTest(classes = [ValidacionAcademicaApplication::class])
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ValidationControllerIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockBean
    lateinit var mailPort: MailPort

    @Test
    fun `POST initiate con estudiante GRADUATED y tipo DEGREE envia OTP y retorna token`() {
        // 10350003 es Maria Torres — GRADUATED (data.sql)
        val body = """
            {
              "requesterName": "Empresa ABC",
              "requesterEmail": "empresa@example.com",
              "studentDocument": "10350003",
              "validationType": "DEGREE"
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/validations/initiate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("VALID"))
            .andExpect(jsonPath("$.token").isNotEmpty)
            .andExpect(jsonPath("$.maskedEmail").value("e***@example.com"))
    }

    @Test
    fun `POST initiate con estudiante no registrado retorna NOT_FOUND sin token`() {
        val body = """
            {
              "requesterName": "Empresa ABC",
              "requesterEmail": "empresa@example.com",
              "studentDocument": "00000000",
              "validationType": "DEGREE"
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/validations/initiate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("NOT_FOUND"))
            .andExpect(jsonPath("$.token").doesNotExist())
    }

    @Test
    fun `POST initiate con estudiante ACTIVE y tipo DEGREE retorna REQUIRES_REVIEW`() {
        // 10350001 Ana Gomez — ACTIVE
        val body = """
            {
              "requesterName": "Empresa ABC",
              "requesterEmail": "empresa@example.com",
              "studentDocument": "10350001",
              "validationType": "DEGREE"
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/validations/initiate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("REQUIRES_REVIEW"))
            .andExpect(jsonPath("$.token").doesNotExist())
    }

    @Test
    fun `POST confirm con token invalido retorna 400`() {
        val body = """{"token": "token-que-no-existe", "code": "123456"}"""

        mockMvc.perform(
            post("/api/validations/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("TOKEN_NOT_FOUND"))
    }

    @Test
    fun `POST initiate con body invalido retorna 400`() {
        val body = """{"requesterName": "", "requesterEmail": "no-es-email", "studentDocument": ""}"""

        mockMvc.perform(
            post("/api/validations/initiate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        ).andExpect(status().isBadRequest)
    }
}
