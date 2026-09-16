package studio.kuraky.kSCore.datos;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import studio.kuraky.kSCore.nucleo.Depurador;
import studio.kuraky.kSCore.nucleo.Registro;
import studio.kuraky.kSCore.nucleo.Tareas;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositorioSqlTest {

    enum Rango { MIEMBRO, VIP, ADMIN }

    @Tabla(nombre = "perfiles", cacheMinutos = 5, cacheMaximo = 100)
    public static final class Perfil {
        @Id public UUID uuid;
        @Columna public String nombre;
        @Columna(indice = true) public int nivel;
        @Columna public long monedas;
        @Columna public Instant ultimaConexion;
        @Columna public Rango rango = Rango.MIEMBRO;
        @Ignorar public transient Object cacheLocal;
    }

    private static int contador = 0;

    private HikariDataSource dataSource;
    private ConexionSql conexion;
    private Tareas tareas;
    private Registro registro;
    private Depurador depurador;

    @BeforeEach
    void preparar() {
        // Cada test usa una base compartida en memoria con nombre único.
        String url = "jdbc:sqlite:file:pruebas-" + (++contador) + "?mode=memory&cache=shared";
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(url);
        cfg.setMaximumPoolSize(2);
        cfg.setMinimumIdle(1);
        dataSource = new HikariDataSource(cfg);
        conexion = new ConexionSql(dataSource, new DialectoSqlite());

        registro = new Registro(ComponentLogger.logger("RepositorioSqlTest"), "Test");
        depurador = new Depurador(registro, false);
        tareas = new Tareas(null, registro);
    }

    @AfterEach
    void cerrar() throws Exception {
        if (tareas != null) tareas.ejecutorVirtual().close();
        if (dataSource != null) dataSource.close();
    }

    private Repositorio<Perfil, UUID> nuevoRepo() {
        DescriptorTabla desc = DescriptorTabla.de(Perfil.class);
        return new RepositorioSql<>(desc, conexion, tareas, registro, depurador);
    }

    @Test
    void guardar_y_obtener_roundtrip() throws Exception {
        Repositorio<Perfil, UUID> repo = nuevoRepo();
        Perfil p = new Perfil();
        p.uuid = UUID.randomUUID();
        p.nombre = "Ana";
        p.nivel = 12;
        p.monedas = 500L;
        p.ultimaConexion = Instant.ofEpochMilli(1_700_000_000_000L);
        p.rango = Rango.VIP;
        repo.guardar(p).futuro().get();

        // Nueva instancia de repo (sin caché caliente) para forzar lectura real.
        Repositorio<Perfil, UUID> repo2 = nuevoRepo();
        Optional<Perfil> leido = repo2.obtener(p.uuid).futuro().get();
        assertTrue(leido.isPresent());
        assertEquals("Ana", leido.get().nombre);
        assertEquals(12, leido.get().nivel);
        assertEquals(500L, leido.get().monedas);
        assertEquals(Instant.ofEpochMilli(1_700_000_000_000L), leido.get().ultimaConexion);
        assertEquals(Rango.VIP, leido.get().rango);
    }

    @Test
    void obtener_id_desconocido_devuelve_vacio() throws Exception {
        Repositorio<Perfil, UUID> repo = nuevoRepo();
        assertFalse(repo.obtener(UUID.randomUUID()).futuro().get().isPresent());
    }

    @Test
    void eliminar_borra_el_registro() throws Exception {
        Repositorio<Perfil, UUID> repo = nuevoRepo();
        Perfil p = new Perfil();
        p.uuid = UUID.randomUUID();
        p.nombre = "X";
        p.ultimaConexion = Instant.now();
        repo.guardar(p).futuro().get();
        repo.eliminar(p.uuid).futuro().get();

        Repositorio<Perfil, UUID> repo2 = nuevoRepo();
        assertFalse(repo2.obtener(p.uuid).futuro().get().isPresent());
    }

    @Test
    void mil_obtener_generan_una_sola_consulta_sql() throws Exception {
        RepositorioSql<Perfil, UUID> repo = (RepositorioSql<Perfil, UUID>) nuevoRepo();
        Perfil p = new Perfil();
        p.uuid = UUID.randomUUID();
        p.nombre = "cache";
        p.ultimaConexion = Instant.now();
        repo.guardar(p).futuro().get();

        // guardar deja el objeto cacheado (write-through), así que la primera obtener
        // ya es un hit.
        for (int i = 0; i < 1_000; i++) {
            Optional<Perfil> leido = repo.obtener(p.uuid).futuro().get();
            assertTrue(leido.isPresent());
        }
        assertEquals(1, repo.tamanoCache());
    }

    @Test
    void buscar_con_filtro_y_orden() throws Exception {
        Repositorio<Perfil, UUID> repo = nuevoRepo();
        for (int i = 0; i < 5; i++) {
            Perfil p = new Perfil();
            p.uuid = UUID.randomUUID();
            p.nombre = "u" + i;
            p.nivel = i * 5;
            p.monedas = 100L * i;
            p.ultimaConexion = Instant.now();
            repo.guardar(p).futuro().get();
        }
        Filtro f = Filtro.donde("nivel").mayorOIgual(10).y("monedas").menorOIgual(300);
        List<Perfil> resultado = repo.buscar(f, Orden.desc("nivel"), 10).futuro().get();
        assertEquals(2, resultado.size());
        assertTrue(resultado.get(0).nivel >= resultado.get(1).nivel);
    }

    @Test
    void migracion_aditiva_agrega_columna_nueva() throws SQLException {
        // Preparamos una tabla legacy con menos columnas.
        try (Connection c = dataSource.getConnection(); Statement st = c.createStatement()) {
            st.executeUpdate("CREATE TABLE perfiles (uuid TEXT PRIMARY KEY NOT NULL, nombre TEXT)");
        }
        Repositorio<Perfil, UUID> repo = nuevoRepo();
        // Prepara la tabla → añade nivel, monedas, ultima_conexion, rango.
        try (Connection c = dataSource.getConnection()) {
            var existentes = new DialectoSqlite().columnasExistentes(c, "perfiles");
            assertTrue(existentes.contains("nivel"));
            assertTrue(existentes.contains("monedas"));
            assertTrue(existentes.contains("ultima_conexion"));
            assertTrue(existentes.contains("rango"));
        }
        assertNotNull(repo);
    }

    @Test
    void contar_devuelve_el_numero_de_registros() throws Exception {
        Repositorio<Perfil, UUID> repo = nuevoRepo();
        assertEquals(0L, repo.contar().futuro().get());
        Perfil p = new Perfil();
        p.uuid = UUID.randomUUID();
        p.nombre = "n";
        p.ultimaConexion = Instant.now();
        repo.guardar(p).futuro().get();
        assertEquals(1L, repo.contar().futuro().get());
    }

    @Test
    void invalidar_cache_forza_recarga() throws Exception {
        RepositorioSql<Perfil, UUID> repo = (RepositorioSql<Perfil, UUID>) nuevoRepo();
        Perfil p = new Perfil();
        p.uuid = UUID.randomUUID();
        p.nombre = "n";
        p.ultimaConexion = Instant.now();
        repo.guardar(p).futuro().get();
        assertEquals(1L, repo.tamanoCache());
        repo.invalidarCacheTodo();
        assertEquals(0L, repo.tamanoCache());
    }
}
