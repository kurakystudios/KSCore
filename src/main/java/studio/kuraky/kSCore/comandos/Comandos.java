package studio.kuraky.kSCore.comandos;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fachada estática del sistema de comandos. Enruta las llamadas al
 * módulo activo tras el arranque.
 */
public final class Comandos {

    private static volatile ModuloComandos modulo;
    private static final AtomicInteger refs = new AtomicInteger();

    private Comandos() {}

    static void inicializar(ModuloComandos activo) {
        Comandos.modulo = activo;
        refs.incrementAndGet();
    }

    static void desinicializar() {
        if (refs.decrementAndGet() <= 0) {
            refs.set(0);
            Comandos.modulo = null;
        }
    }

    public static <T> void registrarTipo(Class<T> tipo, Convertidor<T> convertidor) {
        Convertidores.registrar(tipo, convertidor);
    }

    /** Registra una instancia ya construida por el usuario. */
    public static void registrar(Object instancia) {
        requerir().registrar(instancia);
    }

    /** Lista los comandos registrados hasta ahora, por su nombre principal. */
    public static Collection<String> nombresRegistrados() {
        ModuloComandos m = modulo;
        return m == null ? Collections.emptyList() : m.nombresRegistrados();
    }

    /** Devuelve los descriptores de los métodos registrados (para tooling/debug). */
    public static List<DescriptorMetodo> descriptores() {
        ModuloComandos m = modulo;
        return m == null ? List.of() : m.descriptores();
    }

    private static ModuloComandos requerir() {
        ModuloComandos m = modulo;
        if (m == null) throw new IllegalStateException("Comandos: ModuloComandos aún no está iniciado.");
        return m;
    }
}
