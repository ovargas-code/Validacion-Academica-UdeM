package co.edu.udemedellin.validacionacademica.bootstrap

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.boot.autoconfigure.domain.EntityScan // <-- Importante

@SpringBootApplication(scanBasePackages = ["co.edu.udemedellin.validacionacademica"])
@EnableJpaRepositories(basePackages = ["co.edu.udemedellin.validacionacademica.infrastructure.persistence.repository"])
@EntityScan(basePackages = ["co.edu.udemedellin.validacionacademica.infrastructure.persistence.entity"]) // <-- Este es el radar para las entidades.
open class ValidacionAcademicaApplication

fun main(args: Array<String>) {
    runApplication<ValidacionAcademicaApplication>(*args)
}