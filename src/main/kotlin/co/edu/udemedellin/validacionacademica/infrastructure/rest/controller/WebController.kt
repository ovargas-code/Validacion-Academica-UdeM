package co.edu.udemedellin.validacionacademica.infrastructure.rest.controller

import co.edu.udemedellin.validacionacademica.application.usecase.CreateValidationUseCase
import co.edu.udemedellin.validacionacademica.application.usecase.GetStudentByDocumentUseCase
import co.edu.udemedellin.validacionacademica.domain.model.ValidationRequest
import co.edu.udemedellin.validacionacademica.domain.model.ValidationType
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
class WebController(
    private val createValidationUseCase: CreateValidationUseCase,
    private val getStudentByDocumentUseCase: GetStudentByDocumentUseCase
) {

    @GetMapping("/")
    fun index(): String = "index"

    @PostMapping("/verificar")
    fun verify(
        @RequestParam document: String,
        @RequestParam email: String,
        model: Model
    ): String {
        val studentFound = getStudentByDocumentUseCase.execute(document)
        val nameToRegister = studentFound?.fullName ?: "Portal Web"

        val request = ValidationRequest(
            requesterName = nameToRegister,
            requesterEmail = email,
            studentDocument = document,
            validationType = ValidationType.DEGREE,
            verificationCode = ""
        )

        val response = createValidationUseCase.execute(request)

        model.addAttribute("status", response.result.status)
        model.addAttribute("message", response.result.message)
        model.addAttribute("code", response.request.verificationCode)
        model.addAttribute("letter", response.letter)
        model.addAttribute("mailAttempted", response.mailResult.attempted)
        model.addAttribute("mailSent", response.mailResult.sent)
        model.addAttribute("mailMessage", response.mailResult.message)
        model.addAttribute("mailDestination", response.mailResult.destination ?: email)

        return "result"
    }
}
