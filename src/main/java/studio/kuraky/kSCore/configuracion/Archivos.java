package studio.kuraky.kSCore.configuracion;

import studio.kuraky.kSCore.nucleo.Registro;
import studio.kuraky.kSCore.nucleo.Tareas;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fachada estática del sistema de configuración. Cada instancia de
 * {@link ArchivoBase} se registra aquí una única vez y se recupera
 * después con {@link #obtener(Class)}.
 */
public final class Archivos {

    private static final Map<Class<? extends ArchivoBase>, ArchivoBase> registro = new ConcurrentHashMap<>();
    private static final AtomicInteger refs = new AtomicInteger();

    private static volatile Path carpeta;
    private static volatile Tareas tareas;
    private static volatile Registro registroCore;
    private static volatile ClassLoader cargador;

    private Archivos() {}

    static void inicializar(Path carpetaDatos, Tareas tareasNucleo,
                            Registro registroNucleo, ClassLoader cargadorRecursos) {
        // La carpeta/tareas/registro/cargador se sobrescriben en cada arranque
        // porque los @Archivo del plugin actual se registran inmediatamente
        // después y deben resolverse contra su propia carpeta de datos.
        Archivos.carpeta = carpetaDatos;
        Archivos.tareas = tareasNucleo;
        Archivos.registroCore = registroNucleo;
        Archivos.cargador = cargadorRecursos;
        refs.incrementAndGet();
    }

    static void desinicializar() {
        if (refs.decrementAndGet() <= 0) {
            refs.set(0);
            registro.clear();
            carpeta = null;
            tareas = null;
            registroCore = null;
            cargador = null;
        }
    }

    /**
     * Registra una instancia recién construida. Rellena la configuración
     * interna (ruta absoluta, tareas, registro) y la deja lista para
     * cargar/guardar.
     */
    public static <T extends ArchivoBase> T registrar(T archivo) {
        verificarInicializado();
        Archivo meta = archivo.getClass().getAnnotation(Archivo.class);
        if (meta == null) {
            throw new IllegalArgumentException("La clase " + archivo.getClass().getName()
                    + " no tiene @Archivo.");
        }
        String rutaRelativa = meta.ruta();
        Path rutaAbs = carpeta.resolve(rutaRelativa);
        archivo.configurar(rutaAbs, rutaRelativa, tareas, registroCore, cargador);
        registro.put(archivo.getClass(), archivo);
        return archivo;
    }

    @SuppressWarnings("unchecked")
    public static <T extends ArchivoBase> T obtener(Class<T> clase) {
        ArchivoBase instancia = registro.get(clase);
        if (instancia == null) {
            throw new IllegalStateException("Archivo no registrado: " + clase.getName());
        }
        return (T) instancia;
    }

    public static boolean estaRegistrado(Class<? extends ArchivoBase> clase) {
        return registro.containsKey(clase);
    }

    public static Collection<ArchivoBase> registrados() {
        return List.copyOf(registro.values());
    }

    /** Recarga en paralelo todos los archivos registrados. */
    public static CompletableFuture<Void> recargarTodo() {
        verificarInicializado();
        CompletableFuture<?>[] futuros = registro.values().stream()
                .map(ArchivoBase::recargar)
                .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(futuros);
    }

    private static void verificarInicializado() {
        if (carpeta == null) {
            throw new IllegalStateException("Archivos aún no inicializado: falta arrancar ModuloConfiguracion.");
        }
    }
}
