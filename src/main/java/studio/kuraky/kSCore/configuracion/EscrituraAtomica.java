package studio.kuraky.kSCore.configuracion;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.function.Consumer;

/**
 * Utilidad para escribir un archivo de forma atómica: se serializa en
 * un temporal en el mismo directorio y luego se mueve al destino con
 * {@link StandardCopyOption#ATOMIC_MOVE} (con fallback a
 * {@link StandardCopyOption#REPLACE_EXISTING} cuando el sistema de
 * archivos no lo soporta).
 */
final class EscrituraAtomica {

    private EscrituraAtomica() {}

    static void escribir(Path destino, Consumer<OutputStream> escritor) throws IOException {
        Path padre = destino.getParent();
        if (padre != null) Files.createDirectories(padre);
        Path temporal = Files.createTempFile(padre, destino.getFileName() + "-", ".tmp");
        try {
            try (OutputStream out = Files.newOutputStream(temporal,
                    StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                escritor.accept(out);
            }
            reemplazar(temporal, destino);
        } finally {
            Files.deleteIfExists(temporal);
        }
    }

    static void escribirTexto(Path destino, String contenido) throws IOException {
        escribir(destino, out -> {
            try {
                out.write(contenido.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static void reemplazar(Path temporal, Path destino) throws IOException {
        try {
            Files.move(temporal, destino,
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temporal, destino, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
