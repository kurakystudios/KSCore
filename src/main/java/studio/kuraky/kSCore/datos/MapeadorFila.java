package studio.kuraky.kSCore.datos;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * Traduce entre {@link ResultSet}/{@link PreparedStatement} y objetos
 * Java usando los descriptores compilados de la tabla. Soporta
 * primitivos, sus versiones envueltas, {@link String}, {@link UUID},
 * {@link Instant} y cualquier {@link Enum}.
 */
public final class MapeadorFila {

    private MapeadorFila() {}

    /** Deserializa una fila del ResultSet posicionado en un registro. */
    public static Object aObjeto(ResultSet rs, DescriptorTabla tabla) throws SQLException {
        try {
            Object entidad = tabla.instanciar();
            for (DescriptorColumna col : tabla.columnas()) {
                Object valor = leerColumna(rs, col);
                col.escribir(entidad, valor);
            }
            return entidad;
        } catch (SQLException e) {
            throw e;
        } catch (Throwable t) {
            throw new SQLException("No se pudo mapear fila a " + tabla.clase().getSimpleName(), t);
        }
    }

    public static Object leerColumna(ResultSet rs, DescriptorColumna col) throws SQLException {
        String nombre = col.nombre();
        Class<?> t = col.tipoJava();

        if (t == String.class) return rs.getString(nombre);
        if (t == boolean.class) return rs.getBoolean(nombre);
        if (t == Boolean.class) {
            boolean v = rs.getBoolean(nombre);
            return rs.wasNull() ? null : v;
        }
        if (t == int.class) return rs.getInt(nombre);
        if (t == Integer.class) {
            int v = rs.getInt(nombre);
            return rs.wasNull() ? null : v;
        }
        if (t == long.class) return rs.getLong(nombre);
        if (t == Long.class) {
            long v = rs.getLong(nombre);
            return rs.wasNull() ? null : v;
        }
        if (t == double.class) return rs.getDouble(nombre);
        if (t == Double.class) {
            double v = rs.getDouble(nombre);
            return rs.wasNull() ? null : v;
        }
        if (t == float.class) return rs.getFloat(nombre);
        if (t == Float.class) {
            float v = rs.getFloat(nombre);
            return rs.wasNull() ? null : v;
        }
        if (t == short.class) return rs.getShort(nombre);
        if (t == byte.class) return rs.getByte(nombre);

        if (t == UUID.class) {
            String s = rs.getString(nombre);
            return s == null ? null : UUID.fromString(s);
        }
        if (t == Instant.class) {
            long v = rs.getLong(nombre);
            return rs.wasNull() ? null : Instant.ofEpochMilli(v);
        }
        if (t.isEnum()) {
            String s = rs.getString(nombre);
            if (s == null) return null;
            @SuppressWarnings({"rawtypes", "unchecked"})
            Enum<?> e = Enum.valueOf((Class<? extends Enum>) t, s.toUpperCase(Locale.ROOT));
            return e;
        }

        return rs.getObject(nombre);
    }

    /** Fija un parámetro de PreparedStatement respetando el tipo Java del campo. */
    public static void escribirParametro(PreparedStatement ps, int indice,
                                         Class<?> tipo, Object valor) throws SQLException {
        if (valor == null) {
            ps.setNull(indice, tiposSqlPara(tipo));
            return;
        }
        if (tipo == String.class) { ps.setString(indice, (String) valor); return; }
        if (tipo == boolean.class || tipo == Boolean.class) { ps.setBoolean(indice, (Boolean) valor); return; }
        if (tipo == int.class || tipo == Integer.class) { ps.setInt(indice, (Integer) valor); return; }
        if (tipo == long.class || tipo == Long.class) { ps.setLong(indice, (Long) valor); return; }
        if (tipo == double.class || tipo == Double.class) { ps.setDouble(indice, (Double) valor); return; }
        if (tipo == float.class || tipo == Float.class) { ps.setFloat(indice, (Float) valor); return; }
        if (tipo == short.class || tipo == Short.class) { ps.setShort(indice, (Short) valor); return; }
        if (tipo == byte.class || tipo == Byte.class) { ps.setByte(indice, (Byte) valor); return; }
        if (tipo == UUID.class) { ps.setString(indice, valor.toString()); return; }
        if (tipo == Instant.class) { ps.setLong(indice, ((Instant) valor).toEpochMilli()); return; }
        if (tipo.isEnum()) { ps.setString(indice, ((Enum<?>) valor).name()); return; }
        ps.setObject(indice, valor);
    }

    public static int tiposSqlPara(Class<?> tipo) {
        if (tipo == String.class || tipo == UUID.class || tipo.isEnum()) return Types.VARCHAR;
        if (tipo == boolean.class || tipo == Boolean.class) return Types.BOOLEAN;
        if (tipo == int.class || tipo == Integer.class) return Types.INTEGER;
        if (tipo == long.class || tipo == Long.class || tipo == Instant.class) return Types.BIGINT;
        if (tipo == double.class || tipo == Double.class) return Types.DOUBLE;
        if (tipo == float.class || tipo == Float.class) return Types.FLOAT;
        if (tipo == short.class || tipo == Short.class) return Types.SMALLINT;
        if (tipo == byte.class || tipo == Byte.class) return Types.TINYINT;
        return Types.OTHER;
    }
}
