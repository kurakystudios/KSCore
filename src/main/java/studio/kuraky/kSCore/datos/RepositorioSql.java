package studio.kuraky.kSCore.datos;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import studio.kuraky.kSCore.nucleo.Depurador;
import studio.kuraky.kSCore.nucleo.FuturoAsync;
import studio.kuraky.kSCore.nucleo.Registro;
import studio.kuraky.kSCore.nucleo.Tareas;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Implementación SQL genérica de {@link Repositorio} con caché
 * Caffeine y migración aditiva en el arranque.
 */
public final class RepositorioSql<T, ID> implements Repositorio<T, ID> {

    private final DescriptorTabla descriptor;
    private final ConexionSql conexion;
    private final Tareas tareas;
    private final Registro registro;
    private final Depurador depurador;
    private final Cache<Object, Optional<T>> cache;

    public RepositorioSql(DescriptorTabla descriptor, ConexionSql conexion,
                          Tareas tareas, Registro registro, Depurador depurador) {
        this.descriptor = descriptor;
        this.conexion = conexion;
        this.tareas = tareas;
        this.registro = registro;
        this.depurador = depurador;
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(Math.max(1, descriptor.cacheMinutos())))
                .maximumSize(Math.max(1, descriptor.cacheMaximo()))
                .build();
        prepararTabla();
    }

    @Override
    public DescriptorTabla descriptor() {
        return descriptor;
    }

    @Override
    public FuturoAsync<Optional<T>> obtener(ID id) {
        Optional<T> cacheado = cache.getIfPresent(id);
        if (cacheado != null) {
            return tareas.async(() -> cacheado);
        }
        return tareas.async(() -> {
            Optional<T> valor = leerPorId(id);
            cache.put(id, valor);
            return valor;
        });
    }

    @Override
    public FuturoAsync<Void> guardar(T entidad) {
        Object id = idDe(entidad);
        return tareas.async(() -> {
            escribir(entidad);
            @SuppressWarnings("unchecked")
            T persistida = (T) entidad;
            cache.put(id, Optional.of(persistida));
            return (Void) null;
        });
    }

    @Override
    public FuturoAsync<Void> eliminar(ID id) {
        return tareas.async(() -> {
            borrarPorId(id);
            cache.invalidate(id);
            return (Void) null;
        });
    }

    @Override
    public FuturoAsync<List<T>> buscar(Filtro filtro, Orden orden, int limite) {
        return tareas.async(() -> {
            String base = "SELECT * FROM " + conexion.dialecto().citar(descriptor.nombre());
            Filtro.SqlParametros where = filtro == null ? new Filtro.SqlParametros("", List.of()) : filtro.aSql(conexion.dialecto());
            String orderBy = orden == null ? "" : orden.aSql(conexion.dialecto());
            String lim = limite > 0 ? " LIMIT " + limite : "";
            String sql = base + where.sql() + orderBy + lim;
            return ejecutarSelect(sql, where.parametros());
        });
    }

    @Override
    public FuturoAsync<Long> contar() {
        String sql = conexion.dialecto().contar(descriptor);
        return tareas.async(() -> {
            long medida = medir("contar", () -> {
                try (Connection c = conexion.tomar();
                     Statement st = c.createStatement();
                     ResultSet rs = st.executeQuery(sql)) {
                    return rs.next() ? rs.getLong(1) : 0L;
                } catch (Exception e) {
                    throw new RuntimeException("Fallo contando " + descriptor.nombre(), e);
                }
            });
            return medida;
        });
    }

    @Override
    public FuturoAsync<List<T>> todos() {
        return buscar(null, null, 0);
    }

    @Override
    public void invalidarCache(ID id) {
        cache.invalidate(id);
    }

    @Override
    public void invalidarCacheTodo() {
        cache.invalidateAll();
    }

    // ---------- Núcleo ----------

    private void prepararTabla() {
        String sqlCrear = conexion.dialecto().crearTabla(descriptor);
        try (Connection c = conexion.tomar();
             Statement st = c.createStatement()) {
            st.executeUpdate(sqlCrear);
            // Migración aditiva: comparar columnas existentes con las declaradas.
            var existentes = conexion.dialecto().columnasExistentes(c, descriptor.nombre());
            for (DescriptorColumna col : descriptor.columnas()) {
                if (existentes.contains(col.nombre().toLowerCase(java.util.Locale.ROOT))) continue;
                if (col.esId()) continue;
                st.executeUpdate(conexion.dialecto().agregarColumna(descriptor, col));
                registro.info("Migración: columna " + col.nombre() + " añadida a " + descriptor.nombre());
            }
            // Crear índices declarados.
            for (DescriptorColumna col : descriptor.columnas()) {
                if (col.tieneIndice()) {
                    st.executeUpdate(conexion.dialecto().crearIndice(descriptor, col));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("No se pudo preparar la tabla " + descriptor.nombre(), e);
        }
    }

    private Optional<T> leerPorId(Object id) {
        String sql = conexion.dialecto().selectPorId(descriptor);
        return medir("obtener", () -> {
            try (Connection c = conexion.tomar();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                MapeadorFila.escribirParametro(ps, 1, descriptor.id().tipoJava(), id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return Optional.empty();
                    @SuppressWarnings("unchecked")
                    T obj = (T) MapeadorFila.aObjeto(rs, descriptor);
                    return Optional.of(obj);
                }
            } catch (Exception e) {
                throw new RuntimeException("Fallo leyendo " + descriptor.nombre() + " id=" + id, e);
            }
        });
    }

    private void escribir(T entidad) {
        String sql = conexion.dialecto().upsert(descriptor);
        medir("guardar", () -> {
            try (Connection c = conexion.tomar();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                int indice = 1;
                for (DescriptorColumna col : descriptor.columnas()) {
                    Object valor;
                    try { valor = col.leer(entidad); }
                    catch (Throwable t) { throw new RuntimeException("Fallo leyendo " + col.nombre(), t); }
                    MapeadorFila.escribirParametro(ps, indice++, col.tipoJava(), valor);
                }
                ps.executeUpdate();
                return null;
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException("Fallo guardando " + descriptor.nombre(), e);
            }
        });
    }

    private void borrarPorId(Object id) {
        String sql = conexion.dialecto().eliminarPorId(descriptor);
        medir("eliminar", () -> {
            try (Connection c = conexion.tomar();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                MapeadorFila.escribirParametro(ps, 1, descriptor.id().tipoJava(), id);
                ps.executeUpdate();
                return null;
            } catch (Exception e) {
                throw new RuntimeException("Fallo eliminando " + descriptor.nombre() + " id=" + id, e);
            }
        });
    }

    private List<T> ejecutarSelect(String sql, List<Object> parametros) {
        return medir("buscar", () -> {
            try (Connection c = conexion.tomar();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                for (int i = 0; i < parametros.size(); i++) {
                    ps.setObject(i + 1, parametros.get(i));
                }
                try (ResultSet rs = ps.executeQuery()) {
                    List<T> resultado = new ArrayList<>();
                    while (rs.next()) {
                        @SuppressWarnings("unchecked")
                        T obj = (T) MapeadorFila.aObjeto(rs, descriptor);
                        resultado.add(obj);
                    }
                    return resultado;
                }
            } catch (Exception e) {
                throw new RuntimeException("Fallo buscando " + descriptor.nombre() + " sql=" + sql, e);
            }
        });
    }

    private Object idDe(T entidad) {
        try {
            return descriptor.id().leer(entidad);
        } catch (Throwable t) {
            throw new RuntimeException("No se pudo leer el @Id de " + descriptor.nombre(), t);
        }
    }

    private <R> R medir(String operacion, Supplier<R> trabajo) {
        long inicio = depurador.activo() ? System.nanoTime() : 0L;
        try {
            return trabajo.get();
        } finally {
            if (inicio != 0L) {
                long ms = (System.nanoTime() - inicio) / 1_000_000L;
                depurador.log("Datos", () -> descriptor.nombre() + "." + operacion + " " + ms + "ms");
            }
        }
    }

    // Utilidad para tests: acceso al tamaño de la caché.
    public long tamanoCache() {
        return cache.estimatedSize();
    }
}
