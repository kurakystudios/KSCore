package studio.kuraky.kSCore.datos;

import studio.kuraky.kSCore.configuracion.Archivos;
import studio.kuraky.kSCore.nucleo.ConfigNucleo;
import studio.kuraky.kSCore.nucleo.Depurador;
import studio.kuraky.kSCore.nucleo.FuturoAsync;
import studio.kuraky.kSCore.nucleo.Modulo;
import studio.kuraky.kSCore.nucleo.Nucleo;
import studio.kuraky.kSCore.nucleo.Registro;
import studio.kuraky.kSCore.nucleo.Tareas;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ModuloDatos implements Modulo {

    private final Map<Class<?>, Repositorio<?, ?>> repositorios = new ConcurrentHashMap<>();

    private Nucleo nucleo;
    private Registro registro;
    private Depurador depurador;
    private Tareas tareas;
    private ConexionSql conexion;

    @Override
    public void iniciar(Nucleo nucleo) {
        this.nucleo = nucleo;
        this.registro = nucleo.registro();
        this.depurador = nucleo.depurador();
        this.tareas = nucleo.tareas();

        ConfigNucleo cfg = Archivos.obtener(ConfigNucleo.class);
        ConfigNucleo.BaseDatos bd = cfg.baseDatos;
        try {
            this.conexion = abrirConexion(nucleo, bd);
            Datos.inicializar(this);
            registro.info("Base de datos '" + bd.tipo + "' conectada.");
        } catch (Throwable t) {
            registro.error("No se pudo abrir la base de datos '" + bd.tipo + "'", t);
        }
    }

    @Override
    public void detener() {
        Datos.desinicializar();
        for (Repositorio<?, ?> r : repositorios.values()) r.invalidarCacheTodo();
        repositorios.clear();
        if (conexion != null) {
            try { conexion.close(); } catch (Throwable t) { registro.error("Fallo al cerrar el pool", t); }
            conexion = null;
        }
        this.nucleo = null;
    }

    @SuppressWarnings("unchecked")
    public <T, ID> Repositorio<T, ID> repositorio(Class<T> clase) {
        if (conexion == null) throw new IllegalStateException("Base de datos no disponible.");
        return (Repositorio<T, ID>) repositorios.computeIfAbsent(clase, c -> {
            DescriptorTabla desc = DescriptorTabla.de(c);
            return new RepositorioSql<>(desc, conexion, tareas, registro, depurador);
        });
    }

    public FuturoAsync<List<Fila>> consultaCruda(String sql, Object... parametros) {
        if (conexion == null) throw new IllegalStateException("Base de datos no disponible.");
        return tareas.async(() -> {
            try (Connection c = conexion.tomar();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                for (int i = 0; i < parametros.length; i++) ps.setObject(i + 1, parametros[i]);
                try (ResultSet rs = ps.executeQuery()) {
                    return leerFilas(rs);
                }
            } catch (Throwable t) {
                throw new RuntimeException("Fallo en consultaCruda: " + sql, t);
            }
        });
    }

    public ConexionSql conexion() {
        return conexion;
    }

    private ConexionSql abrirConexion(Nucleo nucleo, ConfigNucleo.BaseDatos bd) {
        String tipo = bd.tipo == null ? "sqlite" : bd.tipo.toLowerCase(Locale.ROOT);
        switch (tipo) {
            case "sqlite" -> {
                Path dir = nucleo.plugin().getDataFolder().toPath();
                try { Files.createDirectories(dir); } catch (Exception ignored) {}
                Path archivo = dir.resolve(bd.archivo == null || bd.archivo.isBlank() ? "datos.db" : bd.archivo);
                String url = "jdbc:sqlite:" + archivo.toAbsolutePath();
                return ConexionSql.crear(url, null, null, null, new DialectoSqlite(), "sqlite");
            }
            case "mariadb", "mysql" -> {
                String url = "jdbc:mariadb://" + bd.host + ":" + bd.puerto + "/" + bd.nombre;
                return ConexionSql.crear(url, bd.usuario, bd.clave, null, new DialectoMariaDb(), "mariadb");
            }
            default -> throw new IllegalStateException("Tipo de base de datos desconocido: " + tipo);
        }
    }

    private static List<Fila> leerFilas(ResultSet rs) throws java.sql.SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int cols = meta.getColumnCount();
        List<Fila> filas = new ArrayList<>();
        while (rs.next()) {
            Map<String, Object> fila = new LinkedHashMap<>(cols);
            for (int i = 1; i <= cols; i++) {
                fila.put(meta.getColumnLabel(i), rs.getObject(i));
            }
            filas.add(new Fila(fila));
        }
        return filas;
    }
}
