package co.edu.udemedellin.validacionacademica.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Propiedades de configuración del almacenamiento de archivos en disco.
 *
 * Configurable vía `app.storage.upload-dir` en application.yml
 * o mediante la variable de entorno `UPLOAD_DIR`.
 */
@ConfigurationProperties(prefix = "app.storage")
data class StorageProperties(
    /** Directorio base donde se almacenan los archivos subidos. Puede ser absoluto o relativo al working directory. */
    val uploadDir: String = "./uploads"
)
