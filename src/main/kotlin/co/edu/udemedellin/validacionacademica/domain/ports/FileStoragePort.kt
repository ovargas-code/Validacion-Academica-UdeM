package co.edu.udemedellin.validacionacademica.domain.ports

import java.io.InputStream

/**
 * Puerto de salida para el almacenamiento de archivos binarios.
 *
 * Las implementaciones son responsables de crear la estructura de directorios necesaria
 * y de garantizar que los nombres de archivo no provoquen colisiones.
 */
interface FileStoragePort {

    /**
     * Almacena el contenido de [inputStream] bajo la ruta [subPath]/[filename]
     * relativa al directorio base configurado.
     *
     * @param inputStream Flujo de bytes del archivo a guardar.
     * @param subPath     Ruta relativa al directorio base, sin barra inicial ni final
     *                    (ej: "solicitudes-empresa/2026/04").
     * @param filename    Nombre del archivo incluyendo extensión (ej: "uuid.pdf").
     * @return            Ruta relativa completa que puede usarse para recuperar el archivo
     *                    con [loadAsBytes] (ej: "solicitudes-empresa/2026/04/uuid.pdf").
     * @throws java.io.IOException si ocurre un error al escribir el archivo.
     */
    fun store(inputStream: InputStream, subPath: String, filename: String): String

    /**
     * Carga el archivo ubicado en [relativePath] (tal como lo retornó [store]) y
     * retorna su contenido como arreglo de bytes.
     *
     * @throws java.nio.file.NoSuchFileException si no existe ningún archivo en esa ruta.
     * @throws java.io.IOException si ocurre un error al leer el archivo.
     */
    fun loadAsBytes(relativePath: String): ByteArray
}
