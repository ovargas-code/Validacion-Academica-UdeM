package co.edu.udemedellin.validacionacademica.infrastructure.storage

import co.edu.udemedellin.validacionacademica.domain.ports.FileStoragePort
import co.edu.udemedellin.validacionacademica.infrastructure.config.StorageProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

/**
 * Implementación de [FileStoragePort] que persiste archivos en el sistema de archivos local.
 *
 * El directorio base se configura mediante `app.storage.upload-dir` (default: `./uploads`).
 * Los directorios intermedios se crean automáticamente si no existen.
 *
 * Seguridad: se normaliza y valida cada ruta resultante para prevenir path-traversal.
 */
@Component
class LocalFileSystemStorageAdapter(
    private val properties: StorageProperties
) : FileStoragePort {

    private val log = LoggerFactory.getLogger(LocalFileSystemStorageAdapter::class.java)

    override fun store(inputStream: InputStream, subPath: String, filename: String): String {
        val baseDir = resolveBaseDir()
        val targetDir = resolveAndValidate(baseDir, subPath)
        val targetFile = resolveAndValidate(targetDir, filename)

        Files.createDirectories(targetDir)
        inputStream.use { stream ->
            Files.copy(stream, targetFile, StandardCopyOption.REPLACE_EXISTING)
        }

        val relativePath = "$subPath/$filename"
        log.info("Archivo almacenado: {}", relativePath)
        return relativePath
    }

    override fun loadAsBytes(relativePath: String): ByteArray {
        val baseDir = resolveBaseDir()
        val file = resolveAndValidate(baseDir, relativePath)
        return Files.readAllBytes(file)
    }

    // ── Utilidades privadas ───────────────────────────────────────────────────

    private fun resolveBaseDir(): Path = Paths.get(properties.uploadDir).toAbsolutePath().normalize()

    /**
     * Resuelve [child] relativo a [parent] y verifica que el resultado no escape
     * del directorio padre (prevención de path traversal).
     *
     * @throws IOException si la ruta resultante queda fuera de [parent].
     */
    private fun resolveAndValidate(parent: Path, child: String): Path {
        val resolved = try {
            parent.resolve(child).normalize()
        } catch (e: InvalidPathException) {
            throw IOException("Ruta de archivo inválida: $child", e)
        }
        if (!resolved.startsWith(parent)) {
            throw IOException("Ruta de archivo fuera del directorio permitido: $child")
        }
        return resolved
    }
}
