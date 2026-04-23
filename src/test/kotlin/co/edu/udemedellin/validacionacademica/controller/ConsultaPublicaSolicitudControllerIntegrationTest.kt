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
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@SpringBootTest(classes = [ValidacionAcademicaApplication::class])
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@TestPropertySource(properties = ["app.storage.upload-dir=./target/test-uploads"])
class ConsultaPublicaSolicitudControllerIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    // ── Helper para crear solicitud y obtener su número ───────────────────────

    private fun crearSolicitudYObtenerNumero(): String {
        val datos = mapOf(
            "nombreEmpresa"           to "Empresa Test S.A.S.",
            "nitEmpresa"              to "890123456-1",
            "nombreContacto"          to "Juan Pérez",
            "cargoContacto"           to "Gerente RRHH",
            "correoContacto"          to "rrhh@empresa.com",
            "tipoDocumentoEstudiante" to "CC",
            "documentoEstudiante"     to "10350001",
            "nombreEstudiante"        to "Ana Gomez",
            "tipoValidacion"          to "DEGREE",
            "aceptaTerminos"          to true
        )
        val datosPart = MockPart("datos", objectMapper.writeValueAsString(datos).toByteArray()).also {
            it.headers.contentType = MediaType.APPLICATION_JSON
        }
        val carta = MockMultipartFile("carta", "carta.pdf", "application/pdf", "contenido-pdf".toByteArray())

        val result = mockMvc.perform(
            multipart("/api/v1/solicitudes-empresa").part(datosPart).file(carta)
        ).andExpect(status().isCreated).andReturn()

        return objectMapper.readTree(result.response.contentAsString)["numeroSolicitud"].asText()
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    fun `GET estado devuelve 200 con campos publicos para solicitud PENDIENTE`() {
        val numero = crearSolicitudYObtenerNumero()

        mockMvc.perform(get("/api/v1/public/solicitudes-empresa/$numero/estado"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.numeroSolicitud").value(numero))
            .andExpect(jsonPath("$.estado").value("PENDIENTE"))
            .andExpect(jsonPath("$.nombreEmpresa").value("Empresa Test S.A.S."))
            .andExpect(jsonPath("$.nombreEstudiante").value("Ana Gomez"))
            .andExpect(jsonPath("$.motivoRechazo").doesNotExist())
            .andExpect(jsonPath("$.fechaRevision").doesNotExist())
    }

    @Test
    fun `GET estado no expone campos sensibles en respuesta`() {
        val numero = crearSolicitudYObtenerNumero()

        mockMvc.perform(get("/api/v1/public/solicitudes-empresa/$numero/estado"))
            .andExpect(status().isOk)
            // Campos que NO deben aparecer
            .andExpect(jsonPath("$.nitEmpresa").doesNotExist())
            .andExpect(jsonPath("$.correoContacto").doesNotExist())
            .andExpect(jsonPath("$.nombreContacto").doesNotExist())
            .andExpect(jsonPath("$.documentoEstudiante").doesNotExist())
            .andExpect(jsonPath("$.adminResponsable").doesNotExist())
            .andExpect(jsonPath("$.comentarioAdmin").doesNotExist())
            .andExpect(jsonPath("$.rutaCarta").doesNotExist())
            .andExpect(jsonPath("$.aceptaTerminos").doesNotExist())
            .andExpect(jsonPath("$.tipoValidacion").doesNotExist())
    }

    @Test
    @WithMockUser(username = "admin", roles = ["ADMIN"])
    fun `GET estado en RECHAZADA expone motivoRechazo`() {
        val numero = crearSolicitudYObtenerNumero()

        mockMvc.perform(
            post("/api/v1/admin/solicitudes-empresa/$numero/rechazar")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"comentarioAdmin":"Carta ilegible"}""")
        ).andExpect(status().isOk)

        mockMvc.perform(get("/api/v1/public/solicitudes-empresa/$numero/estado"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.estado").value("RECHAZADA"))
            .andExpect(jsonPath("$.motivoRechazo").value("Carta ilegible"))
            .andExpect(jsonPath("$.adminResponsable").doesNotExist())
    }

    @Test
    @WithMockUser(username = "admin", roles = ["ADMIN"])
    fun `GET estado en APROBADA no expone motivoRechazo`() {
        val numero = crearSolicitudYObtenerNumero()

        mockMvc.perform(
            post("/api/v1/admin/solicitudes-empresa/$numero/aprobar")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"comentarioAdmin":"Todo correcto"}""")
        ).andExpect(status().isOk)

        mockMvc.perform(get("/api/v1/public/solicitudes-empresa/$numero/estado"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.estado").value("APROBADA"))
            .andExpect(jsonPath("$.motivoRechazo").doesNotExist())
    }

    // ── 404 ───────────────────────────────────────────────────────────────────

    @Test
    fun `GET estado devuelve 404 para numero inexistente`() {
        mockMvc.perform(get("/api/v1/public/solicitudes-empresa/SOL-19990101-000001/estado"))
            .andExpect(status().isNotFound)
    }

    // ── 400 — formato inválido ────────────────────────────────────────────────

    @Test
    fun `GET estado devuelve 400 para numero con formato invalido`() {
        mockMvc.perform(get("/api/v1/public/solicitudes-empresa/numero-malo/estado"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `GET estado devuelve 400 para numero sin guiones`() {
        mockMvc.perform(get("/api/v1/public/solicitudes-empresa/SOL20260423000001/estado"))
            .andExpect(status().isBadRequest)
    }

    // ── Sin autenticación (debe funcionar sin JWT) ────────────────────────────

    @Test
    fun `GET estado es accesible sin autenticacion`() {
        val numero = crearSolicitudYObtenerNumero()

        // No hay @WithMockUser — request totalmente anónimo
        mockMvc.perform(get("/api/v1/public/solicitudes-empresa/$numero/estado"))
            .andExpect(status().isOk)
    }
}
