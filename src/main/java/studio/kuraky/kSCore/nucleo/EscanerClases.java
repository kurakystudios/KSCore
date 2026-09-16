package studio.kuraky.kSCore.nucleo;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

public final class EscanerClases {

    private static final String SUFIJO_CLASE = ".class";
    private static final String SUFIJO_LIBS = ".libs.";

    private final JavaPlugin plugin;
    private final Registro registro;
    private final String paqueteAnfitrion;
    private final String paqueteCore;
    private final Map<Class<? extends Annotation>, Set<Class<?>>> indice = new ConcurrentHashMap<>();

    private volatile Set<Class<?>> clases;

    public EscanerClases(JavaPlugin plugin, Registro registro) {
        this(plugin, plugin.getClass().getPackageName(), registro);
    }

    public EscanerClases(JavaPlugin plugin, String paqueteAnfitrion, Registro registro) {
        this.plugin = plugin;
        this.registro = registro;
        this.paqueteAnfitrion = paqueteAnfitrion;
        this.paqueteCore = deducirPaqueteCore();
    }

    /**
     * Paquete raíz del propio KsCore. En modo plugin propio coincide con
     * el paquete del anfitrión ({@code studio.kuraky.kSCore}); en modo
     * embebido queda relocado bajo {@code <anfitrion>.libs.core}. Se
     * escanea en cualquier caso para que las clases anotadas del core
     * (ComandoCore, ConfigNucleo, ConfigMensajes, RegistroEfecto…) queden
     * registradas también cuando corren embebidas.
     */
    private static String deducirPaqueteCore() {
        String propio = Nucleo.class.getPackage().getName();          // ...core.nucleo
        int i = propio.lastIndexOf('.');
        return i > 0 ? propio.substring(0, i) : propio;
    }

    public Set<Class<?>> todas() {
        Set<Class<?>> local = clases;
        if (local == null) {
            synchronized (this) {
                local = clases;
                if (local == null) {
                    local = escanear();
                    clases = local;
                }
            }
        }
        return local;
    }

    public Set<Class<?>> conAnotacion(Class<? extends Annotation> anotacion) {
        return indice.computeIfAbsent(anotacion, a -> {
            Set<Class<?>> resultado = new HashSet<>();
            for (Class<?> clase : todas()) {
                if (clase.isAnnotationPresent(a)) {
                    resultado.add(clase);
                }
            }
            return Set.copyOf(resultado);
        });
    }

    private Set<Class<?>> escanear() {
        Set<Class<?>> resultado = new HashSet<>();
        URL url = plugin.getClass().getProtectionDomain().getCodeSource().getLocation();
        if (url == null) {
            registro.aviso("EscanerClases: no se pudo determinar el origen del código.");
            return Set.of();
        }
        try {
            Path ruta = Paths.get(url.toURI());
            if (Files.isDirectory(ruta)) {
                escanearDirectorio(ruta, ruta, resultado);
            } else {
                escanearJar(ruta, resultado);
            }
        } catch (URISyntaxException | IOException e) {
            registro.aviso("EscanerClases: fallo al leer clases (" + e.getMessage() + ")");
        }
        return Set.copyOf(resultado);
    }

    private void escanearDirectorio(Path raiz, Path dir, Set<Class<?>> resultado) throws IOException {
        try (Stream<Path> stream = Files.walk(dir)) {
            stream.filter(p -> p.toString().endsWith(SUFIJO_CLASE)).forEach(p -> {
                String relativo = raiz.relativize(p).toString()
                        .replace('/', '.').replace('\\', '.');
                String nombre = relativo.substring(0, relativo.length() - SUFIJO_CLASE.length());
                cargar(nombre, resultado);
            });
        }
    }

    private void escanearJar(Path jar, Set<Class<?>> resultado) throws IOException {
        try (JarFile jf = new JarFile(jar.toFile())) {
            var entradas = jf.entries();
            while (entradas.hasMoreElements()) {
                JarEntry entrada = entradas.nextElement();
                String nombre = entrada.getName();
                if (!nombre.endsWith(SUFIJO_CLASE)) continue;
                String claseNombre = nombre.replace('/', '.')
                        .substring(0, nombre.length() - SUFIJO_CLASE.length());
                cargar(claseNombre, resultado);
            }
        }
    }

    private void cargar(String nombre, Set<Class<?>> resultado) {
        if (nombre.equals("module-info") || nombre.endsWith(".package-info")) return;
        if (!esAceptado(nombre)) return;
        try {
            Class<?> clase = Class.forName(nombre, false, plugin.getClass().getClassLoader());
            resultado.add(clase);
        } catch (NoClassDefFoundError | ClassNotFoundException t) {
            // Dependencia opcional ausente (Vault, PAPI, MongoDB…) — no es un
            // error para el plugin: la clase simplemente no forma parte del
            // escaneo. Se registra a nivel debug para no ensuciar el arranque.
            registro.debug("EscanerClases: se salta " + nombre + " (" + t.getClass().getSimpleName() + ")");
        } catch (Throwable t) {
            registro.aviso("EscanerClases: no se pudo cargar " + nombre + " (" + t.getClass().getSimpleName() + ": " + t.getMessage() + ")");
        }
    }

    private boolean esAceptado(String nombre) {
        // El paquete del propio core se acepta siempre (aunque quede bajo
        // <anfitrion>.libs.core tras la relocación), excluyendo sus propias
        // sub-libs sombreadas (caffeine, hikari, night-config…).
        if (nombre.startsWith(paqueteCore + ".")) {
            return !nombre.startsWith(paqueteCore + SUFIJO_LIBS);
        }
        // Clases del anfitrión: aceptar todas menos las que caen en su
        // subpaquete de librerías sombreadas.
        if (nombre.startsWith(paqueteAnfitrion + ".")) {
            return !nombre.startsWith(paqueteAnfitrion + SUFIJO_LIBS);
        }
        return false;
    }
}
