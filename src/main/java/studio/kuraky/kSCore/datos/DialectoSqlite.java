package studio.kuraky.kSCore.datos;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;

public final class DialectoSqlite implements DialectoSql {

    @Override public String nombre() { return "sqlite"; }

    @Override
    public String tipoColumna(DescriptorColumna col) {
        return DialectoSql.tipoJavaANumericoSqlite(col.tipoJava());
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
            if (col.esId()) sb.append(" PRIMARY KEY");
            primera = false;
        }
        sb.append(")");
        return sb.toString();
    }

    @Override
    public String agregarColumna(DescriptorTabla tabla, DescriptorColumna col) {
        return "ALTER TABLE " + citar(tabla.nombre())
                + " ADD COLUMN " + citar(col.nombre()) + " " + tipoColumna(col);
    }

    @Override
    public String crearIndice(DescriptorTabla tabla, DescriptorColumna col) {
        return "CREATE INDEX IF NOT EXISTS " + citar("idx_" + tabla.nombre() + "_" + col.nombre())
                + " ON " + citar(tabla.nombre()) + "(" + citar(col.nombre()) + ")";
    }

    @Override
    public Set<String> columnasExistentes(Connection conexion, String tabla) throws SQLException {
        try (Statement st = conexion.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA table_info(" + citar(tabla) + ")")) {
            return DialectoSql.lecturaColumnasEnumeradas(rs, "name");
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
        sb.append(" ON CONFLICT(").append(citar(tabla.id().nombre())).append(") DO UPDATE SET ");
        primera = true;
        for (DescriptorColumna col : tabla.columnas()) {
            if (col.esId()) continue;
            if (!primera) sb.append(", ");
            sb.append(citar(col.nombre())).append(" = excluded.").append(citar(col.nombre()));
            primera = false;
        }
        return sb.toString();
    }
}
