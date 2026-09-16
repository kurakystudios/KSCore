package studio.kuraky.kSCore.datos;

import studio.kuraky.kSCore.nucleo.FuturoAsync;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fachada estática del sistema de datos. Solo válida tras el arranque
 * de {@link ModuloDatos}.
 */
public final class Datos {

    private static volatile ModuloDatos modulo;
    private static final AtomicInteger refs = new AtomicInteger();

    private Datos() {}

    static void inicializar(ModuloDatos activo) {
        Datos.modulo = activo;
        refs.incrementAndGet();
    }

    static void desinicializar() {
        if (refs.decrementAndGet() <= 0) {
            refs.set(0);
            Datos.modulo = null;
        }
    }

    public static <T, ID> Repositorio<T, ID> repositorio(Class<T> clase) {
        return requerir().repositorio(clase);
    }

    public static FuturoAsync<List<Fila>> consultaCruda(String sql, Object... parametros) {
        return requerir().consultaCruda(sql, parametros);
    }

    public static ConexionSql conexion() {
        return requerir().conexion();
    }

    public static DialectoSql dialecto() {
        return requerir().conexion().dialecto();
    }

    private static ModuloDatos requerir() {
        ModuloDatos m = modulo;
        if (m == null) throw new IllegalStateException("Datos: ModuloDatos aún no está iniciado.");
        return m;
    }
}
