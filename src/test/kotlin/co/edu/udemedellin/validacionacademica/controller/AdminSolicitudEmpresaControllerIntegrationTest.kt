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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

/**
 * Tests de integración para [AdminSolicitudEmpresaController].
 *
 * El estudiante "10350001" (Ana Gomez) existe en data.sql y se usa en los tests de aprobación.
 * Con @Transactional la BD se revierte al finalizar cada test.
 */
@SpringBootTest(classes = [ValidacionAcademicaApplication::class])
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@TestPropertySource(properties = ["app.storage.upload-dir=./target/test-uploads"])
class AdminSolicitudEmpresaControllerIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun crearSolicitudYObtenerNumero(documentoEstudiante: String = "10350001"): String {
        val datos = mapOf(
            "nombreEmpresa"           to "Empresa Test S.A.S.",
            "nitEmpresa"              to "890123456-1",
            "nombreContacto"          to "Juan Pérez",
            "cargoContacto"           to "Gerente RRHH",
            "correoContacto"          to "rrhh@empresa.com",
            "tipoDocumentoEstudiante" to "CC",
            "documentoEstudiante"     to documentoEstudiante,
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

    private fun bodyConComentario(comentario: String?) =
        if (comentario != null) """{"comentarioAdmin":"$comentario"}"""
        else "{}"

    // ── Transición a EN_REVISION ──────────────────────────────────────────────

    @Test
    @WithMockUser(username = "admin", roles = ["ADMIN"])
    fun `marcar-en-revision desde PENDIENTE devuelve 200 con estado EN_REVISION`() {
        val numero = crearSolicitudYObtenerNumero()

        mockMvc.perform(
            post("/api/v1/admin/solicitudes-empresa/$numero/marcar-en-revision")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.estado").value("EN_REVISION"))
            .andExpect(jsonPath("$.adminResponsable").value("admin"))
            .andExpect(jsonPath("$.fechaRevision").isNotEmpty)
    }

    @Test
    @WithMockUser(username = "admin", roles = ["ADMIN"])
    fun `marcar-en-revision desde EN_REVISION devuelve 409`() {
        val numero = crearSolicitudYObtenerNumero()
        // Primera transición
        mockMvc.perform(
            post("/api/v1/admin/solicitudes-empresa/$numero/marcar-en-revision")
                .contentType(MediaType.APPLICATION_JSON).content("{}")
        ).andExpect(status().isOk)

        // Segunda transición — inválida
        mockMvc.perform(
            post("/api/v1/admin/solicitudes-empresa/$numero/marcar-en-revision")
                .contentType(MediaType.APPLICATION_JSON).content("{}")
        ).andExpect(status().isConflict)
    }

    // ── Transición a APROBADA ─────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "admin", roles = ["ADMIN"])
    fun `aprobar desde PENDIENTE devuelve 200 con estado APROBADA`() {
        val numero = crearSolicitudYObtenerNumero()  // documentoEstudiante = 10350001 existe en data.sql

        mockMvc.perform(
            post("/api/v1/admin/solicitudes-empresa/$numero/aprobar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bodyConComentario("Documentación verificada"))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.estado").value("APROBADA"))
            .andExpect(jsonPath("$.adminResponsable").value("admin"))
            .andExpect(jsonPath("$.comentarioAdmin").value("Documentación verificada"))
    }

    @Test
    @WithMockUser(username = "admin", roles = ["ADMIN"])
    fun `aprobar desde EN_REVISION devuelve 200 con estado APROBADA`() {
        val numero = crearSolicitudYObtenerNumero()
        mockMvc.perform(
            post("/api/v1/admin/solicitudes-empresa/$numero/marcar-en-revision")
                .contentType(MediaType.APPLICATION_JSON).content("{}")
        ).andExpect(status().isOk)

        mockMvc.perform(
            post("/api/v1/admin/solicitudes-empresa/$numero/aprobar")
                .contentType(MediaType.APPLICATION_JSON).content("{}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.estado").value("APROBADA"))
    }

    @Test
    @WithMockUser(username = "admin", roles = ["ADMIN"])
    fun `aprobar solicitud con estudiante inexistente devuelve 409`() {
        val numero = crearSolicitudYObtenerNumero(documentoEstudiante = "99999999")  // estudiante no existe

        mockMvc.perform(
            post("/api/v1/admin/solicitudes-empresa/$numero/aprobar")
                .contentType(MediaType.APPLICATION_JSON).content("{}")
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("estudiante referenciado no existe")))
    }

    @Test
    @WithMockUser(username = "admin", roles = ["ADMIN"])
    fun `aprobar solicitud ya APROBADA devuelve 409`() {
        val numero = crearSolicitudYObtenerNumero()
        mockMvc.perform(
            post("/api/v1/admin/solicitudes-empresa/$numero/aprobar")
                .contentType(MediaType.APPLICATION_JSON).content("{}")
        ).andExpect(status().isOk)

        mockMvc.perform(
            post("/api/v1/admin/solicitudes-empresa/$numero/aprobar")
                .contentType(MediaType.APPLICATION_JSON).content("{}")
        ).andExpect(status().isConflict)
    }

    // ── Transición a RECHAZADA ────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "admin", roles = ["ADMIN"])
    fun `rechazar desde PENDIENTE con comentario devuelve 200 con estado RECHAZADA`() {
        val numero = crearSolicitudYObtenerNumero()

        mockMvc.perform(
            post("/api/v1/admin/solicitudes-empresa/$numero/rechazar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bodyConComentario("Carta de autorización ilegible"))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.estado").value("RECHAZADA"))
            .andExpect(jsonPath("$.comentarioAdmin").value("Carta de autorización ilegible"))
            .andExpect(jsonPath("$.adminResponsable").value("admin"))
    }

    @Test
    @WithMockUser(username = "admin", roles = ["ADMIN"])
    fun `rechazar sin comentarioAdmin devuelve 400`() {
        val numero = crearSolicitudYObtenerNumero()

        mockMvc.perform(
            post("/api/v1/admin/solicitudes-empresa/$numero/rechazar")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        ).andExpect(status().isBadRequest)
    }

    @Test
    @WithMockUser(username = "admin", roles = ["ADMIN"])
    fun `rechazar con comentarioAdmin en blanco devuelve 400`() {
        val numero = crearSolicitudYObtenerNumero()

        mockMvc.perform(
            post("/api/v1/admin/solicitudes-empresa/$numero/rechazar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bodyConComentario("   "))
        ).andExpect(status().isBadRequest)
    }

    @Test
    @WithMockUser(username = "admin", roles = ["ADMIN"])
    fun `rechazar solicitud ya RECHAZADA devuelve 409`() {
        val numero = crearSolicitudYObtenerNumero()
        mockMvc.perform(
            post("/api/v1/admin/solicitudes-empresa/$numero/rechazar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bodyConComentario("Motivo inicial"))
        ).andExpect(status().isOk)

        mockMvc.perform(
            post("/api/v1/admin/solicitudes-empresa/$numero/rechazar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bodyConComentario("Otro motivo"))
        ).andExpect(status().isConflict)
    }

    // ── Autenticación ─────────────────────────────────────────────────────────

    @Test
    fun `marcar-en-revision sin JWT devuelve 401`() {
        mockMvc.perform(
            post("/api/v1/admin/solicitudes-empresa/SOL-20260422-000001/marcar-en-revision")
                .contentType(MediaType.APPLICATION_JSON).content("{}")
        ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `aprobar sin JWT devuelve 401`() {
        mockMvc.perform(
            post("/api/v1/admin/solicitudes-empresa/SOL-20260422-000001/aprobar")
                .contentType(MediaType.APPLICATION_JSON).content("{}")
        ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `rechazar sin JWT devuelve 401`() {
        mockMvc.perform(
            post("/api/v1/admin/solicitudes-empresa/SOL-20260422-000001/rechazar")
                .contentType(MediaType.APPLICATION_JSON).content("{}")
        ).andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(roles = ["USER"])
    fun `marcar-en-revision con rol USER devuelve 403`() {
        mockMvc.perform(
            post("/api/v1/admin/solicitudes-empresa/SOL-20260422-000001/marcar-en-revision")
                .contentType(MediaType.APPLICATION_JSON).content("{}")
        ).andExpect(status().isForbidden)
    }
}
