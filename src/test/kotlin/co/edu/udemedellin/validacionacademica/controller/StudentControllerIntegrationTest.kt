package co.edu.udemedellin.validacionacademica.controller

import co.edu.udemedellin.validacionacademica.bootstrap.ValidacionAcademicaApplication
import com.fasterxml.jackson.databind.ObjectMapper
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
class StudentControllerIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    private val validStudentJson = """
        {
          "document": "99990001",
          "fullName": "Test Usuario",
          "program": "Ingeniería de Sistemas",
          "academicLevel": "PREGRADO",
          "status": "ACTIVO"
        }
    """.trimIndent()

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `POST estudiante valido devuelve 201`() {
        mockMvc.perform(
            post("/api/v1/students")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validStudentJson)
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.document").value("99990001"))
            .andExpect(jsonPath("$.fullName").value("Test Usuario"))
            .andExpect(jsonPath("$.id").isNotEmpty)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `POST estudiante con documento duplicado devuelve 409`() {
        mockMvc.perform(
            post("/api/v1/students")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validStudentJson)
        ).andExpect(status().isCreated)

        mockMvc.perform(
            post("/api/v1/students")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validStudentJson)
        ).andExpect(status().isConflict)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `POST estudiante con body invalido devuelve 400`() {
        val invalidJson = """{"document": "", "fullName": "", "program": ""}"""
        mockMvc.perform(
            post("/api/v1/students")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson)
        ).andExpect(status().isBadRequest)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `GET estudiante existente devuelve 200`() {
        // El data.sql inserta 10350001
        mockMvc.perform(get("/api/v1/students/10350001"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.document").value("10350001"))
            .andExpect(jsonPath("$.fullName").value("Ana Gomez"))
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `GET estudiante inexistente devuelve 404`() {
        mockMvc.perform(get("/api/v1/students/99999999"))
            .andExpect(status().isNotFound)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `GET lista paginada devuelve 200 con estructura de pagina`() {
        mockMvc.perform(get("/api/v1/students?page=0&size=10"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").isArray)
            .andExpect(jsonPath("$.totalElements").isNumber)
            .andExpect(jsonPath("$.totalPages").isNumber)
            .andExpect(jsonPath("$.currentPage").value(0))
            .andExpect(jsonPath("$.pageSize").value(10))
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `GET lista usa defaults cuando no se pasan parametros`() {
        mockMvc.perform(get("/api/v1/students"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.pageSize").value(20))
            .andExpect(jsonPath("$.currentPage").value(0))
    }

    @Test
    fun `GET sin autenticacion devuelve 401`() {
        mockMvc.perform(get("/api/v1/students"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `PUT estudiante existente devuelve 200 con datos actualizados`() {
        val updateJson = """
            {
              "document": "10350001",
              "fullName": "Ana Gomez Actualizada",
              "program": "Medicina",
              "academicLevel": "PREGRADO",
              "status": "GRADUADO",
              "degreeTitle": "Médica",
              "graduationDate": "2025-12-01"
            }
        """.trimIndent()

        mockMvc.perform(
            put("/api/v1/students/10350001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.fullName").value("Ana Gomez Actualizada"))
            .andExpect(jsonPath("$.status").value("GRADUADO"))
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `PUT estudiante inexistente devuelve 404`() {
        val updateJson = """
            {
              "document": "99999999",
              "fullName": "Nadie",
              "program": "Ninguno",
              "academicLevel": "PREGRADO",
              "status": "ACTIVO"
            }
        """.trimIndent()

        mockMvc.perform(
            put("/api/v1/students/99999999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson)
        ).andExpect(status().isNotFound)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `DELETE estudiante existente devuelve 204`() {
        mockMvc.perform(delete("/api/v1/students/10350002"))
            .andExpect(status().isNoContent)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `DELETE estudiante inexistente devuelve 404`() {
        mockMvc.perform(delete("/api/v1/students/99999999"))
            .andExpect(status().isNotFound)
    }
}
