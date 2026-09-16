package studio.kuraky.kSCore.datos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

/** Dialecto compatible con MariaDB y MySQL. */
public final class DialectoMariaDb implements DialectoSql {

    @Override public String nombre() { return "mariadb"; }

    @Override
    public String tipoColumna(DescriptorColumna col) {
        return DialectoSql.tipoJavaANumericoMysql(col.tipoJava(), col.longitud());
    }

    @Override
    public String crearTabla(DescriptorTabla tabla) {
        StringBuilder sb = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
        sb.append(citar(tabla.nombre())).append(" (");
        boolean primera = true;
        for (DescriptorColumna col : tabla.columnas()) {
            if (!primera) sb.append(", ");
            sb.append(citar(col.nombre())).append(' ').append(tipoColumna(col));
            if (!col.nullable() || col.esId()) sb.append(" NOT NULL");
            primera = false;
        }
        sb.append(", PRIMARY KEY (").append(citar(tabla.id().nombre())).append(')');
        sb.append(") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        return sb.toString();
    }

    @Override
    public String agregarColumna(DescriptorTabla tabla, DescriptorColumna col) {
        return "ALTER TABLE " + citar(tabla.nombre())
                + " ADD COLUMN " + citar(col.nombre()) + " " + tipoColumna(col);
    }

    @Override
    public String crearIndice(DescriptorTabla tabla, DescriptorColumna col) {
        return "CREATE INDEX " + citar("idx_" + tabla.nombre() + "_" + col.nombre())
                + " ON " + citar(tabla.nombre()) + "(" + citar(col.nombre()) + ")";
    }

    @Override
    public Set<String> columnasExistentes(Connection conexion, String tabla) throws SQLException {
        String sql = "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, tabla);
            try (ResultSet rs = ps.executeQuery()) {
                return DialectoSql.lecturaColumnasEnumeradas(rs, "COLUMN_NAME");
            }
        }
    }

    @Override
    public String upsert(DescriptorTabla tabla) {
        StringBuilder sb = new StringBuilder("INSERT INTO ").append(citar(tabla.nombre())).append(" (");
        StringBuilder placeholders = new StringBuilder(") VALUES (");
        boolean primera = true;
        for (DescriptorColumna col : tabla.columnas()) {
            if (!primera) { sb.append(", "); placeholders.append(", "); }
            sb.append(citar(col.nombre()));
            placeholders.append('?');
            primera = false;
        }
        sb.append(placeholders).append(')');
        sb.append(" ON DUPLICATE KEY UPDATE ");
        primera = true;
        for (DescriptorColumna col : tabla.columnas()) {
            if (col.esId()) continue;
            if (!primera) sb.append(", ");
            sb.append(citar(col.nombre())).append(" = VALUES(").append(citar(col.nombre())).append(')');
            primera = false;
        }
        return sb.toString();
    }

    @Override
    public String citar(String identificador) {
        return "`" + identificador.replace("`", "``") + "`";
    }
}
