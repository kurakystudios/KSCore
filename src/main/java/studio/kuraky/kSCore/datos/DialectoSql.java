package studio.kuraky.kSCore.datos;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Diferencias entre motores SQL. Cada implementación traduce nuestro
 * descriptor genérico a SQL específico (tipos, upsert, migración).
 */
public interface DialectoSql {

    /** Nombre para logs y tests. */
    String nombre();

    /** Traduce el tipo Java del campo a la definición SQL para esa columna. */
    String tipoColumna(DescriptorColumna col);

    /** Genera CREATE TABLE IF NOT EXISTS. */
    String crearTabla(DescriptorTabla tabla);

    /** Genera ALTER TABLE ADD COLUMN. */
    String agregarColumna(DescriptorTabla tabla, DescriptorColumna col);

    /** Genera CREATE INDEX IF NOT EXISTS para la columna dada. */
    String crearIndice(DescriptorTabla tabla, DescriptorColumna col);

    /** Devuelve el conjunto de columnas presentes en la tabla en disco. */
    Set<String> columnasExistentes(Connection conexion, String tabla) throws SQLException;

    /** SQL de upsert: {@code INSERT ... ON CONFLICT/ON DUPLICATE ...}. */
    String upsert(DescriptorTabla tabla);

    /** SQL de {@code SELECT ... FROM tabla WHERE id = ?}. */
    default String selectPorId(DescriptorTabla tabla) {
        return "SELECT * FROM " + citar(tabla.nombre())
                + " WHERE " + citar(tabla.id().nombre()) + " = ?";
    }

    /** SQL de {@code DELETE ... WHERE id = ?}. */
    default String eliminarPorId(DescriptorTabla tabla) {
        return "DELETE FROM " + citar(tabla.nombre())
                + " WHERE " + citar(tabla.id().nombre()) + " = ?";
    }

    /** SQL de {@code SELECT COUNT(*) FROM tabla}. */
    default String contar(DescriptorTabla tabla) {
        return "SELECT COUNT(*) FROM " + citar(tabla.nombre());
    }

    /** SQL de {@code SELECT * FROM tabla}. */
    default String selectTodos(DescriptorTabla tabla) {
        return "SELECT * FROM " + citar(tabla.nombre());
    }

    /** Citado del identificador según el motor. */
    default String citar(String identificador) {
        return "\"" + identificador.replace("\"", "\"\"") + "\"";
    }

    /** Helpers estáticos comunes a todos los dialectos. */
    static String enumeracionColumnas(DescriptorTabla tabla, String separador, String citaCol) {
        StringBuilder sb = new StringBuilder();
        boolean primera = true;
        for (DescriptorColumna c : tabla.columnas()) {
            if (!primera) sb.append(separador);
            sb.append(citaCol).append(c.nombre()).append(citaCol);
            primera = false;
        }
        return sb.toString();
    }

    static String tipoJavaANumericoSqlite(Class<?> tipo) {
        if (tipo == boolean.class || tipo == Boolean.class) return "INTEGER";
        if (tipo == int.class || tipo == Integer.class) return "INTEGER";
        if (tipo == long.class || tipo == Long.class || tipo == Instant.class) return "INTEGER";
        if (tipo == short.class || tipo == Short.class) return "INTEGER";
        if (tipo == byte.class || tipo == Byte.class) return "INTEGER";
        if (tipo == double.class || tipo == Double.class) return "REAL";
        if (tipo == float.class || tipo == Float.class) return "REAL";
        if (tipo == String.class || tipo == UUID.class || tipo.isEnum()) return "TEXT";
        return "TEXT";
    }

    static String tipoJavaANumericoMysql(Class<?> tipo, int longitud) {
        if (tipo == boolean.class || tipo == Boolean.class) return "TINYINT(1)";
        if (tipo == int.class || tipo == Integer.class) return "INT";
        if (tipo == long.class || tipo == Long.class || tipo == Instant.class) return "BIGINT";
        if (tipo == short.class || tipo == Short.class) return "SMALLINT";
        if (tipo == byte.class || tipo == Byte.class) return "TINYINT";
        if (tipo == double.class || tipo == Double.class) return "DOUBLE";
        if (tipo == float.class || tipo == Float.class) return "FLOAT";
        if (tipo == UUID.class) return "VARCHAR(36)";
        if (tipo.isEnum()) return "VARCHAR(64)";
        if (tipo == String.class) return "VARCHAR(" + Math.max(1, longitud) + ")";
        return "TEXT";
    }

    static Set<String> lecturaColumnasEnumeradas(ResultSet rs, String columnaNombre) throws SQLException {
        Set<String> salida = new HashSet<>();
        while (rs.next()) {
            String n = rs.getString(columnaNombre);
            if (n != null) salida.add(n.toLowerCase(java.util.Locale.ROOT));
        }
        return Collections.unmodifiableSet(salida);
    }
}
