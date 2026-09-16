package studio.kuraky.kSCore.configuracion;

import studio.kuraky.kSCore.nucleo.Registro;
import studio.kuraky.kSCore.nucleo.Tareas;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Base común para todos los archivos de configuración. Cada instancia
 * gestiona un único archivo en disco: lectura sincrónica en el arranque
 * (breve por diseño) y escritura asíncrona atómica en cualquier momento.
 */
public abstract class ArchivoBase {

    private final ReentrantLock cerrojo = new ReentrantLock();

    private Path ruta;
    private String rutaRelativa;
    private Tareas tareas;
    private Registro registro;
    private ClassLoader cargador;

    void configurar(Path ruta, String rutaRelativa, Tareas tareas, Registro registro, ClassLoader cargador) {
        this.ruta = ruta;
        this.rutaRelativa = rutaRelativa;
        this.tareas = tareas;
        this.registro = registro;
        this.cargador = cargador;
    }

    public final Path ruta() {
        return ruta;
    }

    public final String rutaRelativa() {
        return rutaRelativa;
    }

    public final boolean existe() {
        return ruta != null && Files.exists(ruta);
    }

    /**
     * Lee el archivo del disco y vuelca los valores sobre esta instancia.
     * Si el archivo no existe, se crea con los valores por defecto. Si
     * faltan claves, se auto-migra escribiendo en segundo plano.
     */
    public final void cargar() {
        prepararArchivo();
        Map<String, Object> bruto;
        cerrojo.lock();
        try {
            bruto = leerDisco();
        } catch (IOException e) {
            registrarError("No se pudo leer " + rutaRelativa, e);
            return;
        } finally {
            cerrojo.unlock();
        }
        boolean faltantes = Mapeador.aObjeto(this, bruto);
        try {
            alCargado(bruto);
        } catch (Throwable t) {
            registrarError("Fallo en alCargado(" + rutaRelativa + ")", t);
        }
        if (faltantes) {
            if (tareas != null) guardar();
            else guardarBloqueante();
        }
    }

    /** Guarda el estado actual en disco de forma asíncrona y atómica. */
    public final CompletableFuture<Void> guardar() {
        if (tareas == null) throw new IllegalStateException("Archivo " + rutaRelativa + " no configurado.");
        return CompletableFuture.runAsync(this::escribirAtomicamente, tareas.ejecutorVirtual());
    }

    /** Recarga el archivo y notifica a los métodos anotados con {@link AlRecargar}. */
    public final CompletableFuture<Void> recargar() {
        if (tareas == null) throw new IllegalStateException("Archivo " + rutaRelativa + " no configurado.");
        return CompletableFuture.runAsync(() -> {
            cargar();
            notificarRecarga();
        }, tareas.ejecutorVirtual());
    }

    /** Escritura sincrónica reservada al ciclo de vida (auto-migración inmediata). */
    final void guardarBloqueante() {
        escribirAtomicamente();
    }

    /** Recarga sincrónica útil en tests y en el arranque de un módulo. */
    final void recargarBloqueante() {
        cargar();
        notificarRecarga();
    }

    private void escribirAtomicamente() {
        cerrojo.lock();
        try {
            Map<String, Object> mapa = Mapeador.aMapa(this);
            alGuardar(mapa);
            Map<String, List<String>> comentarios = Mapeador.comentarios(this);
            escribirDisco(mapa, comentarios);
        } catch (IOException e) {
            registrarError("No se pudo guardar " + rutaRelativa, e);
        } catch (Throwable t) {
            registrarError("Fallo inesperado guardando " + rutaRelativa, t);
        } finally {
            cerrojo.unlock();
        }
    }

    private void prepararArchivo() {
        try {
            Path padre = ruta.getParent();
            if (padre != null) Files.createDirectories(padre);
            if (Files.exists(ruta)) return;
            Archivo meta = getClass().getAnnotation(Archivo.class);
            if (meta != null && meta.copiarRecurso() && cargador != null) {
                InputStream recurso = cargador.getResourceAsStream(rutaRelativa);
                if (recurso != null) {
                    try (recurso) {
                        Files.copy(recurso, ruta, StandardCopyOption.REPLACE_EXISTING);
                        return;
                    }
                }
            }
            guardarBloqueante();
        } catch (IOException e) {
            registrarError("No se pudo preparar " + rutaRelativa, e);
        }
    }

    private void notificarRecarga() {
        for (Method metodo : getClass().getDeclaredMethods()) {
            if (!metodo.isAnnotationPresent(AlRecargar.class)) continue;
            if (metodo.getParameterCount() != 0) continue;
            if (Modifier.isStatic(metodo.getModifiers())) continue;
            metodo.setAccessible(true);
            try {
                metodo.invoke(this);
            } catch (Throwable t) {
                registrarError("Fallo en @AlRecargar " + getClass().getSimpleName() + "#" + metodo.getName(), t);
            }
        }
    }

    private void registrarError(String mensaje, Throwable causa) {
        if (registro != null) {
            registro.error(mensaje, causa);
            return;
        }
        System.err.println(mensaje);
        if (causa != null) causa.printStackTrace();
    }

    /** Hook opcional para que las subclases lean el mapa crudo (mensajes, etc.). */
    protected void alCargado(Map<String, Object> bruto) {
    }

    /** Hook opcional para que las subclases inyecten entradas dinámicas antes de guardar. */
    protected void alGuardar(Map<String, Object> destino) {
    }

    /**
     * Lee el archivo devolviendo un mapa canónico. Debe convertir las
     * estructuras anidadas propias del formato en {@code Map<String,
     * Object>}.
     */
    protected abstract Map<String, Object> leerDisco() throws IOException;

    /**
     * Escribe el mapa en disco de forma atómica. Los comentarios se
     * aplican por clave jerárquica cuando el formato lo permite.
     */
    protected abstract void escribirDisco(Map<String, Object> valores,
                                          Map<String, List<String>> comentarios) throws IOException;
}
