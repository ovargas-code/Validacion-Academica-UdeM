package co.edu.udemedellin.validacionacademica.infrastructure.rest.dto

import co.edu.udemedellin.validacionacademica.domain.model.StudentPage

data class StudentPageResponse(
    val content: List<StudentResponse>,
    val totalElements: Long,
    val totalPages: Int,
    val currentPage: Int,
    val pageSize: Int
)

fun StudentPage.toResponse() = StudentPageResponse(
    content = content.map { it.toResponse() },
    totalElements = totalElements,
    totalPages = totalPages,
    currentPage = currentPage,
    pageSize = pageSize
)
