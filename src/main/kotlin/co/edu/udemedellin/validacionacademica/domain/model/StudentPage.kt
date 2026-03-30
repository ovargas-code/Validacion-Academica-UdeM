package co.edu.udemedellin.validacionacademica.domain.model

data class StudentPage(
    val content: List<Student>,
    val totalElements: Long,
    val totalPages: Int,
    val currentPage: Int,
    val pageSize: Int
)
