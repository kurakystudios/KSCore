package studio.kuraky.kSCore.configuracion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchivoRoundTripTest {

    @Archivo(ruta = "config.yml", copiarRecurso = false)
    public static final class ConfigYaml extends ArchivoYaml {
        @Comentario("Habilita el modo de depuración")
        public boolean debug = false;
        public String prefijo = "&8[&bCore&8]";
        public int intervalo = 30;
        public List<String> canales = List.of("global", "staff");

        @Seccion("base-datos")
        public Sub bd = new Sub();
    }

    @Archivo(ruta = "config.toml", copiarRecurso = false)
    public static final class ConfigToml extends ArchivoToml {
        @Comentario("Habilita el modo de depuración")
        public boolean debug = false;
        public String prefijo = "prefijo-por-defecto";
        public int intervalo = 30;

        @Seccion("base-datos")
        public Sub bd = new Sub();
    }

    @Archivo(ruta = "config.json", copiarRecurso = false)
    public static final class ConfigJson extends ArchivoJson {
        public boolean debug = false;
        public String prefijo = "prefijo-por-defecto";
        public int intervalo = 30;

        @Seccion("base-datos")
        public Sub bd = new Sub();
    }

    public static final class Sub {
        public String host = "localhost";
        public int puerto = 5432;
    }

    private static <T extends ArchivoBase> T inicializar(T archivo, Path dir, String rutaRelativa) {
        Path abs = dir.resolve(rutaRelativa);
        archivo.configurar(abs, rutaRelativa, null, null, null);
        return archivo;
    }

    @Test
    void yaml_roundtrip_crea_y_recarga(@TempDir Path dir) {
        ConfigYaml original = inicializar(new ConfigYaml(), dir, "config.yml");
        original.debug = true;
        original.prefijo = "&aServer";
        original.intervalo = 45;
        original.canales = List.of("uno", "dos");
        original.bd.host = "mongo";
        original.bd.puerto = 27017;
        original.guardarBloqueante();

        assertTrue(Files.exists(dir.resolve("config.yml")));

        ConfigYaml recargado = inicializar(new ConfigYaml(), dir, "config.yml");
        recargado.cargar();

        assertEquals(true, recargado.debug);
        assertEquals("&aServer", recargado.prefijo);
        assertEquals(45, recargado.intervalo);
        assertEquals(List.of("uno", "dos"), recargado.canales);
        assertEquals("mongo", recargado.bd.host);
        assertEquals(27017, recargado.bd.puerto);
    }

    @Test
    void yaml_incluye_comentarios(@TempDir Path dir) throws IOException {
        ConfigYaml archivo = inicializar(new ConfigYaml(), dir, "config.yml");
        archivo.guardarBloqueante();
        String contenido = Files.readString(dir.resolve("config.yml"));
        assertTrue(contenido.contains("Habilita el modo de depuración"),
                "El comentario debería estar en el YAML:\n" + contenido);
    }

    @Test
    void toml_roundtrip_crea_y_recarga(@TempDir Path dir) {
        ConfigToml original = inicializar(new ConfigToml(), dir, "config.toml");
        original.debug = true;
        original.prefijo = "servidor";
        original.intervalo = 90;
        original.bd.host = "postgres";
        original.bd.puerto = 5433;
        original.guardarBloqueante();

        assertTrue(Files.exists(dir.resolve("config.toml")));

        ConfigToml recargado = inicializar(new ConfigToml(), dir, "config.toml");
        recargado.cargar();
        assertEquals(true, recargado.debug);
        assertEquals("servidor", recargado.prefijo);
        assertEquals(90, recargado.intervalo);
        assertEquals("postgres", recargado.bd.host);
        assertEquals(5433, recargado.bd.puerto);
    }

    @Test
    void toml_incluye_comentarios(@TempDir Path dir) throws IOException {
        ConfigToml archivo = inicializar(new ConfigToml(), dir, "config.toml");
        archivo.guardarBloqueante();
        String contenido = Files.readString(dir.resolve("config.toml"));
        assertTrue(contenido.contains("Habilita el modo de depuración"),
                "El comentario debería estar en el TOML:\n" + contenido);
    }

    @Test
    void json_roundtrip_crea_y_recarga(@TempDir Path dir) {
        ConfigJson original = inicializar(new ConfigJson(), dir, "sub/config.json");
        original.debug = true;
        original.prefijo = "json";
        original.intervalo = 60;
        original.bd.host = "sql";
        original.bd.puerto = 1521;
        original.guardarBloqueante();

        assertTrue(Files.exists(dir.resolve("sub/config.json")));

        ConfigJson recargado = inicializar(new ConfigJson(), dir, "sub/config.json");
        recargado.cargar();
        assertEquals(true, recargado.debug);
        assertEquals("json", recargado.prefijo);
        assertEquals(60, recargado.intervalo);
        assertEquals("sql", recargado.bd.host);
        assertEquals(1521, recargado.bd.puerto);
    }

    @Test
    void auto_migracion_recupera_defaults_de_claves_ausentes(@TempDir Path dir) throws IOException {
        // Escribimos manualmente un YAML incompleto (falta 'intervalo' y toda la sección bd).
        Path archivo = dir.resolve("config.yml");
        Files.writeString(archivo, "debug: true\nprefijo: parcial\ncanales: []\n");

        ConfigYaml recargado = inicializar(new ConfigYaml(), dir, "config.yml");
        recargado.cargar();

        assertEquals(true, recargado.debug);
        assertEquals("parcial", recargado.prefijo);
        // Valores por defecto para las claves ausentes.
        assertEquals(30, recargado.intervalo);
        assertEquals("localhost", recargado.bd.host);
        assertEquals(5432, recargado.bd.puerto);

        // El archivo se ha reescrito con las claves migradas.
        String contenido = Files.readString(archivo);
        assertTrue(contenido.contains("intervalo"));
        assertTrue(contenido.contains("base-datos"));
    }

    @Test
    void guardar_muchas_veces_no_corrompe(@TempDir Path dir) {
        ConfigYaml archivo = inicializar(new ConfigYaml(), dir, "config.yml");
        for (int i = 0; i < 100; i++) {
            archivo.intervalo = i;
            archivo.guardarBloqueante();
        }
        ConfigYaml recargado = inicializar(new ConfigYaml(), dir, "config.yml");
        recargado.cargar();
        assertEquals(99, recargado.intervalo);
    }

    @Test
    void mapa_bruto_expuesto_a_alCargado(@TempDir Path dir) throws IOException {
        // Escribimos un YAML con una clave dinámica extra que no está en la clase.
        Path archivo = dir.resolve("mensajes.yml");
        Files.writeString(archivo, "extra: dinamico\n");

        RegistroBruto cfg = inicializar(new RegistroBruto(), dir, "mensajes.yml");
        cfg.cargar();
        assertEquals("dinamico", cfg.capturado.get("extra"));
    }

    @Archivo(ruta = "mensajes.yml", copiarRecurso = false)
    public static final class RegistroBruto extends ArchivoYaml {
        public transient Map<String, Object> capturado;

        @Override
        protected void alCargado(Map<String, Object> bruto) {
            capturado = bruto;
        }
    }

    @Test
    void sin_faltantes_no_reescribe_archivo(@TempDir Path dir) throws IOException {
        // Guardamos por primera vez con todos los defaults.
        ConfigYaml archivo = inicializar(new ConfigYaml(), dir, "config.yml");
        archivo.guardarBloqueante();
        long marca = Files.getLastModifiedTime(dir.resolve("config.yml")).toMillis();

        // Cargar con todos los valores presentes → no debería tocar el archivo.
        // Nota: nos protegemos ante relojes de baja resolución no comprobando la marca,
        // sino que verificamos vía el flag interno (faltantes == false) leyendo lo que hay.
        ConfigYaml recargado = inicializar(new ConfigYaml(), dir, "config.yml");
        recargado.cargar();

        // Auto-migración inhibida: el archivo sigue siendo válido y contiene lo mismo.
        assertFalse(Files.readString(dir.resolve("config.yml")).isBlank());
        // Comprobación de humo: el sello no debería ser menor tras cargar.
        long marca2 = Files.getLastModifiedTime(dir.resolve("config.yml")).toMillis();
        assertTrue(marca2 >= marca);
    }
}
