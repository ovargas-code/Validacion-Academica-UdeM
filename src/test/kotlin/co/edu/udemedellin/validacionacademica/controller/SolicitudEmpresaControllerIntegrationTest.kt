package co.edu.udemedellin.validacionacademica.controller

import co.edu.udemedellin.validacionacademica.bootstrap.ValidacionAcademicaApplication
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.mock.web.MockPart
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.transaction.annotation.Transactional

/**
 * Tests de integración para [SolicitudEmpresaController].
 *
 * Usa H2 en memoria (perfil "test") y escribe archivos en ./target/test-uploads.
 * Con @Transactional, la BD se revierte al finalizar cada test; los archivos de disco
 * permanecen en ./target/test-uploads (comportamiento esperado: misma deuda conocida
 * de atomicidad disco/BD documentada en el controller).
 *
 * Contrato multipart verificado:
 *   - Parte "datos": Blob JSON (application/json)
 *   - Parte "carta": archivo PDF (application/pdf)
 */
@SpringBootTest(classes = [ValidacionAcademicaApplication::class])
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@TestPropertySource(properties = ["app.storage.upload-dir=./target/test-uploads"])
class SolicitudEmpresaControllerIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun dadosValidos(aceptaTerminos: Boolean = true) = mapOf(
        "nombreEmpresa"           to "Empresa Test S.A.S.",
        "nitEmpresa"              to "890123456-1",
        "nombreContacto"          to "Juan Pérez",
        "cargoContacto"           to "Gerente RRHH",
        "correoContacto"          to "rrhh@empresa.com",
        "tipoDocumentoEstudiante" to "CC",
        "documentoEstudiante"     to "10350001",
        "nombreEstudiante"        to "Ana Gomez",
        "tipoValidacion"          to "DEGREE",
        "aceptaTerminos"          to aceptaTerminos
    )

    private fun datosPart(dados: Map<String, Any>): MockPart {
        val json = objectMapper.writeValueAsString(dados)
        return MockPart("datos", json.toByteArray()).also {
            it.headers.contentType = MediaType.APPLICATION_JSON
        }
    }

    private fun cartaPdf(nombre: String = "carta.pdf", mediaType: String = "application/pdf") =
        MockMultipartFile("carta", nombre, mediaType, "contenido-pdf-simulado".toByteArray())

    // ── Tests POST /api/v1/solicitudes-empresa ────────────────────────────────

    @Test
    fun `POST solicitud valida devuelve 201 con numero de radicado`() {
        mockMvc.perform(
            multipart("/api/v1/solicitudes-empresa")
                .part(datosPart(dadosValidos()))
                .file(cartaPdf())
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.numeroSolicitud").isString)
            .andExpect(jsonPath("$.numeroSolicitud").value(org.hamcrest.Matchers.matchesPattern("SOL-\\d{8}-\\d{6}")))
            .andExpect(jsonPath("$.estado").value("PENDIENTE"))
            .andExpect(jsonPath("$.nombreEmpresa").value("Empresa Test S.A.S."))
            .andExpect(jsonPath("$.rutaCarta").doesNotExist()) // campo interno omitido
    }

    @Test
    fun `POST solicitudes consecutivas el mismo dia generan numeros distintos`() {
        val res1 = mockMvc.perform(
            multipart("/api/v1/solicitudes-empresa")
                .part(datosPart(dadosValidos()))
                .file(cartaPdf())
        ).andExpect(status().isCreated).andReturn()

        val res2 = mockMvc.perform(
            multipart("/api/v1/solicitudes-empresa")
                .part(datosPart(dadosValidos()))
                .file(cartaPdf())
        ).andExpect(status().isCreated).andReturn()

        val numero1 = objectMapper.readTree(res1.response.contentAsString)["numeroSolicitud"].asText()
        val numero2 = objectMapper.readTree(res2.response.contentAsString)["numeroSolicitud"].asText()
        assert(numero1 != numero2) { "Los números de radicado deben ser únicos: $numero1 == $numero2" }
    }

    @Test
    fun `POST con aceptaTerminos false devuelve 400`() {
        mockMvc.perform(
            multipart("/api/v1/solicitudes-empresa")
                .part(datosPart(dadosValidos(aceptaTerminos = false)))
                .file(cartaPdf())
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `POST con NIT invalido devuelve 400`() {
        val dadosNitInvalido = dadosValidos().toMutableMap().apply { put("nitEmpresa", "ABC-INVALIDO") }
        mockMvc.perform(
            multipart("/api/v1/solicitudes-empresa")
                .part(datosPart(dadosNitInvalido))
                .file(cartaPdf())
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `POST con correo invalido devuelve 400`() {
        val dadosCorreoInvalido = dadosValidos().toMutableMap().apply { put("correoContacto", "no-es-un-correo") }
        mockMvc.perform(
            multipart("/api/v1/solicitudes-empresa")
                .part(datosPart(dadosCorreoInvalido))
                .file(cartaPdf())
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `POST con carta que no es PDF devuelve 400`() {
        val cartaWord = cartaPdf(nombre = "carta.docx", mediaType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
        mockMvc.perform(
            multipart("/api/v1/solicitudes-empresa")
                .part(datosPart(dadosValidos()))
                .file(cartaWord)
        )
            .andExpect(status().isBadRequest)
    }

    // ── Tests GET /api/v1/solicitudes-empresa/{numeroSolicitud} ───────────────

    @Test
    fun `GET solicitud existente devuelve 200 con los datos`() {
        // Creamos la solicitud primero dentro de la misma transacción de test
        val createResult = mockMvc.perform(
            multipart("/api/v1/solicitudes-empresa")
                .part(datosPart(dadosValidos()))
                .file(cartaPdf())
        ).andExpect(status().isCreated).andReturn()

        val numero = objectMapper.readTree(createResult.response.contentAsString)["numeroSolicitud"].asText()

        mockMvc.perform(get("/api/v1/solicitudes-empresa/$numero"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.numeroSolicitud").value(numero))
            .andExpect(jsonPath("$.estado").value("PENDIENTE"))
            .andExpect(jsonPath("$.nombreEmpresa").value("Empresa Test S.A.S."))
    }

    @Test
    fun `GET solicitud inexistente devuelve 404`() {
        mockMvc.perform(get("/api/v1/solicitudes-empresa/SOL-19990101-000001"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `GET con formato de numero invalido devuelve 400`() {
        mockMvc.perform(get("/api/v1/solicitudes-empresa/numero-malo"))
            .andExpect(status().isBadRequest)
    }

    // ── Tests GET /api/v1/solicitudes-empresa/recursos/plantilla-carta ────────

    @Test
    fun `GET plantilla-carta devuelve 200 con content-type docx`() {
        mockMvc.perform(get("/api/v1/solicitudes-empresa/recursos/plantilla-carta"))
            .andExpect(status().isOk)
            .andExpect(
                content().contentType(
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                )
            )
    }
}
