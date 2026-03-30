package co.edu.udemedellin.validacionacademica.controller

import co.edu.udemedellin.validacionacademica.bootstrap.ValidacionAcademicaApplication
import co.edu.udemedellin.validacionacademica.domain.ports.PdfGeneratorPort
import co.edu.udemedellin.validacionacademica.infrastructure.persistence.entity.ValidationRequestEntity
import co.edu.udemedellin.validacionacademica.infrastructure.persistence.repository.ValidationRequestJpaRepository
import co.edu.udemedellin.validacionacademica.domain.model.ValidationType
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional

@SpringBootTest(classes = [ValidacionAcademicaApplication::class])
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class VerificationControllerIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var validationRequestJpaRepository: ValidationRequestJpaRepository

    @MockBean
    lateinit var pdfGeneratorPort: PdfGeneratorPort

    private val testVerificationCode = "UDEM-VERIF001"

    private fun insertValidationRequest() {
        validationRequestJpaRepository.save(
            ValidationRequestEntity(
                requesterName = "Empresa Test",
                requesterEmail = "test@example.com",
                studentDocument = "10350003", // Maria Torres — GRADUADO (data.sql)
                validationType = ValidationType.DEGREE,
                verificationCode = testVerificationCode
            )
        )
    }

    @Test
    fun `GET verificacion con codigo valido retorna 200 con datos del certificado`() {
        insertValidationRequest()

        mockMvc.perform(get("/api/v1/verificaciones/$testVerificationCode"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.valid").value(true))
            .andExpect(jsonPath("$.studentName").value("Maria Torres"))
            .andExpect(jsonPath("$.program").value("Derecho"))
    }

    @Test
    fun `GET verificacion con codigo inexistente retorna 404`() {
        mockMvc.perform(get("/api/v1/verificaciones/UDEM-NOEXISTE"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `GET verificacion con formato de codigo invalido retorna 400`() {
        // El código contiene caracteres no permitidos por @Pattern
        mockMvc.perform(get("/api/v1/verificaciones/codigo invalido!"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `GET pdf con codigo valido retorna 200 con content-type PDF`() {
        insertValidationRequest()
        given(pdfGeneratorPort.generateCertificate(anyString(), anyString(), anyString(), anyString()))
            .willReturn(byteArrayOf(1, 2, 3))

        mockMvc.perform(get("/api/v1/verificaciones/$testVerificationCode/pdf"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
    }
}
