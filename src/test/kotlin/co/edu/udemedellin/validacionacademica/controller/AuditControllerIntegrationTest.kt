package co.edu.udemedellin.validacionacademica.controller

import co.edu.udemedellin.validacionacademica.bootstrap.ValidacionAcademicaApplication
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional

@SpringBootTest(classes = [ValidacionAcademicaApplication::class])
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuditControllerIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `GET audit retorna lista vacia cuando no hay eventos`() {
        mockMvc.perform(get("/api/v1/admin/audit"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
    }

    @Test
    @WithMockUser(username = "admin", roles = ["ADMIN"])
    fun `POST estudiante genera evento de auditoria`() {
        val studentJson = """
            {
              "document": "88880001",
              "fullName": "Test Auditoria",
              "program": "Derecho",
              "academicLevel": "PREGRADO",
              "status": "ACTIVO"
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/v1/students")
                .contentType(MediaType.APPLICATION_JSON)
                .content(studentJson)
        ).andExpect(status().isCreated)

        mockMvc.perform(get("/api/v1/admin/audit"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].action").value("CREATE_STUDENT"))
            .andExpect(jsonPath("$[0].performedBy").value("admin"))
            .andExpect(jsonPath("$[0].targetDocument").value("88880001"))
    }

    @Test
    @WithMockUser(username = "admin", roles = ["ADMIN"])
    fun `DELETE estudiante genera evento de auditoria`() {
        // 10350001 existe en data.sql
        mockMvc.perform(delete("/api/v1/students/10350001"))
            .andExpect(status().isNoContent)

        mockMvc.perform(get("/api/v1/admin/audit?action=DELETE_STUDENT"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].action").value("DELETE_STUDENT"))
            .andExpect(jsonPath("$[0].targetDocument").value("10350001"))
    }

    @Test
    fun `GET audit sin autenticacion devuelve 401`() {
        mockMvc.perform(get("/api/v1/admin/audit"))
            .andExpect(status().isUnauthorized)
    }
}
