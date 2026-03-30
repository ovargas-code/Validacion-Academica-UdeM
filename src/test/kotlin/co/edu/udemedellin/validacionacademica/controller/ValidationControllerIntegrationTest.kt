package co.edu.udemedellin.validacionacademica.controller

import co.edu.udemedellin.validacionacademica.bootstrap.ValidacionAcademicaApplication
import co.edu.udemedellin.validacionacademica.domain.ports.MailPort
import co.edu.udemedellin.validacionacademica.domain.ports.PdfGeneratorPort
import co.edu.udemedellin.validacionacademica.infrastructure.persistence.repository.EmailVerificationJpaRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.ArgumentMatchers.anyString
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

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var emailVerificationJpaRepository: EmailVerificationJpaRepository

    @MockBean
    lateinit var mailPort: MailPort

    @MockBean
    lateinit var pdfGeneratorPort: PdfGeneratorPort

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

    @Test
    fun `POST confirm con OTP correcto retorna 200 con PDF`() {
        given(pdfGeneratorPort.generateCertificate(anyString(), anyString(), anyString(), anyString()))
            .willReturn(byteArrayOf(1, 2, 3))

        // Paso 1: iniciar validación (10350003 Maria Torres — GRADUADO)
        val initiateBody = """
            {
              "requesterName": "Empresa Test",
              "requesterEmail": "test@example.com",
              "studentDocument": "10350003",
              "validationType": "DEGREE"
            }
        """.trimIndent()

        val initiateResult = mockMvc.perform(
            post("/api/validations/initiate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(initiateBody)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("VALID"))
            .andReturn()

        val token = objectMapper.readTree(initiateResult.response.contentAsString)
            .get("token").asText()

        // Paso 2: recuperar el OTP desde la base de datos
        val verification = emailVerificationJpaRepository.findByToken(token)!!

        // Paso 3: confirmar con el OTP real
        val confirmBody = """{"token": "$token", "code": "${verification.code}"}"""

        mockMvc.perform(
            post("/api/validations/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(confirmBody)
        )
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", MediaType.APPLICATION_PDF_VALUE))
    }

    @Test
    fun `POST confirm con OTP incorrecto retorna 422`() {
        val initiateBody = """
            {
              "requesterName": "Empresa Test",
              "requesterEmail": "test@example.com",
              "studentDocument": "10350003",
              "validationType": "DEGREE"
            }
        """.trimIndent()

        val initiateResult = mockMvc.perform(
            post("/api/validations/initiate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(initiateBody)
        ).andReturn()

        val token = objectMapper.readTree(initiateResult.response.contentAsString)
            .get("token").asText()

        val confirmBody = """{"token": "$token", "code": "000000"}"""

        mockMvc.perform(
            post("/api/validations/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(confirmBody)
        )
            .andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.code").value("INVALID_CODE"))
    }

    @Test
    fun `POST confirm con token ya utilizado retorna 410`() {
        given(pdfGeneratorPort.generateCertificate(anyString(), anyString(), anyString(), anyString()))
            .willReturn(byteArrayOf(1, 2, 3))

        val initiateBody = """
            {
              "requesterName": "Empresa Test",
              "requesterEmail": "test@example.com",
              "studentDocument": "10350003",
              "validationType": "DEGREE"
            }
        """.trimIndent()

        val initiateResult = mockMvc.perform(
            post("/api/validations/initiate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(initiateBody)
        ).andReturn()

        val token = objectMapper.readTree(initiateResult.response.contentAsString)
            .get("token").asText()
        val code = emailVerificationJpaRepository.findByToken(token)!!.code

        val confirmBody = """{"token": "$token", "code": "$code"}"""

        // Primer uso — exitoso
        mockMvc.perform(
            post("/api/validations/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(confirmBody)
        ).andExpect(status().isOk)

        // Segundo uso — token ya utilizado
        mockMvc.perform(
            post("/api/validations/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(confirmBody)
        )
            .andExpect(status().isGone)
            .andExpect(jsonPath("$.code").value("TOKEN_ALREADY_USED"))
    }
}
