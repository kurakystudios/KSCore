package studio.kuraky.kSCore.datos;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Envuelve un {@link HikariDataSource} con su dialecto. El pool se
 * dimensiona en función de los núcleos disponibles (núcleos*2, máx.
 * 10) siguiendo la recomendación del plan.
 */
public final class ConexionSql implements AutoCloseable {

    private final HikariDataSource dataSource;
    private final DialectoSql dialecto;

    public ConexionSql(HikariDataSource dataSource, DialectoSql dialecto) {
        this.dataSource = dataSource;
        this.dialecto = dialecto;
    }

    public Connection tomar() throws SQLException {
        return dataSource.getConnection();
    }

    public DialectoSql dialecto() {
        return dialecto;
    }

    public HikariDataSource dataSource() {
        return dataSource;
    }

    @Override
    public void close() {
        dataSource.close();
    }

    /** Fábrica que aplica el dimensionado estándar del pool. */
    public static ConexionSql crear(String url, String usuario, String clave,
                                    String driver, DialectoSql dialecto,
                                    String nombreCliente) {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(url);
        if (usuario != null && !usuario.isEmpty()) cfg.setUsername(usuario);
        if (clave != null && !clave.isEmpty()) cfg.setPassword(clave);
        if (driver != null && !driver.isEmpty()) cfg.setDriverClassName(driver);
        int nucleos = Runtime.getRuntime().availableProcessors();
        int max = Math.min(10, Math.max(2, nucleos * 2));
        cfg.setMaximumPoolSize(max);
        cfg.setMinimumIdle(Math.max(1, max / 2));
        cfg.setPoolName(nombreCliente == null ? "KSCore-Hikari" : "KSCore-" + nombreCliente);
        cfg.setAutoCommit(true);
        cfg.setConnectionTimeout(10_000L);
        return new ConexionSql(new HikariDataSource(cfg), dialecto);
    }
}
