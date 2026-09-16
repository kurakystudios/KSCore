package studio.kuraky.kSCore.configuracion;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapeadorTest {

    enum Nivel { BAJO, MEDIO, ALTO }

    public static final class SubConfig {
        public String host = "localhost";
        public int puerto = 5432;
    }

    public static final class Config {
        public boolean activo = true;
        public int intervalo = 30;
        public double umbral = 0.75;
        public String prefijo = "&8[&bCore&8]";
        public Nivel nivel = Nivel.MEDIO;
        public List<String> canales = List.of("global", "staff");

        @Clave("nombre-servidor")
        public String nombre = "Kuraky";

        @Ignorar
        public String secreto = "no-persistir";

        @Seccion("base-datos")
        public SubConfig bd = new SubConfig();
    }

    @Test
    void ida_y_vuelta_conserva_todos_los_valores() {
        Config original = new Config();
        original.activo = false;
        original.intervalo = 120;
        original.umbral = 0.5;
        original.prefijo = "test";
        original.nivel = Nivel.ALTO;
        original.canales = List.of("uno", "dos");
        original.nombre = "Otro";
        original.bd.host = "otro-host";
        original.bd.puerto = 27017;

        Map<String, Object> mapa = Mapeador.aMapa(original);

        Config destino = new Config();
        boolean faltantes = Mapeador.aObjeto(destino, mapa);

        assertFalse(faltantes);
        assertEquals(false, destino.activo);
        assertEquals(120, destino.intervalo);
        assertEquals(0.5, destino.umbral);
        assertEquals("test", destino.prefijo);
        assertEquals(Nivel.ALTO, destino.nivel);
        assertEquals(List.of("uno", "dos"), destino.canales);
        assertEquals("Otro", destino.nombre);
        assertEquals("otro-host", destino.bd.host);
        assertEquals(27017, destino.bd.puerto);
    }

    @Test
    void clave_renombra_la_entrada_serializada() {
        Config original = new Config();
        Map<String, Object> mapa = Mapeador.aMapa(original);
        assertTrue(mapa.containsKey("nombre-servidor"));
        assertFalse(mapa.containsKey("nombre"));
    }

    @Test
    void ignorar_omite_el_campo() {
        Config original = new Config();
        Map<String, Object> mapa = Mapeador.aMapa(original);
        assertFalse(mapa.containsKey("secreto"));
    }

    @Test
    void seccion_produce_submapa() {
        Config original = new Config();
        Map<String, Object> mapa = Mapeador.aMapa(original);
        Object sub = mapa.get("base-datos");
        assertNotNull(sub);
        assertTrue(sub instanceof Map<?, ?>);
    }

    @Test
    void faltantes_devuelve_true_cuando_hay_claves_ausentes() {
        Config destino = new Config();
        boolean faltantes = Mapeador.aObjeto(destino, Map.of("activo", false));
        assertTrue(faltantes);
        assertEquals(false, destino.activo);
        // El resto conserva los valores por defecto.
        assertEquals(30, destino.intervalo);
        assertEquals("Kuraky", destino.nombre);
    }

    @Test
    void enums_se_deserializan_ignorando_mayusculas() {
        Config destino = new Config();
        Mapeador.aObjeto(destino, Map.of(
                "activo", true,
                "intervalo", 1,
                "umbral", 0.0,
                "prefijo", "p",
                "nivel", "alto",
                "canales", List.of(),
                "nombre-servidor", "n",
                "base-datos", Map.of("host", "h", "puerto", 1)
        ));
        assertEquals(Nivel.ALTO, destino.nivel);
    }

    @Test
    void numeros_se_coercen_entre_int_long_double() {
        Config destino = new Config();
        // TOML/JSON pueden devolver Long donde esperamos int; el mapeador coerce.
        Mapeador.aObjeto(destino, Map.of(
                "activo", true,
                "intervalo", 42L,
                "umbral", 1,
                "prefijo", "p",
                "nivel", "BAJO",
                "canales", List.of(),
                "nombre-servidor", "n",
                "base-datos", Map.of("host", "h", "puerto", 2L)
        ));
        assertEquals(42, destino.intervalo);
        assertEquals(1.0, destino.umbral);
        assertEquals(2, destino.bd.puerto);
    }

    @Test
    void comentarios_se_extraen_con_ruta_jerarquica() {
        Map<String, List<String>> comentarios = Mapeador.comentarios(new ConComentarios());
        assertEquals(List.of("Primera línea", "Segunda línea"), comentarios.get("activo"));
        assertEquals(List.of("Bloque"), comentarios.get("sub"));
        assertEquals(List.of("Puerto por defecto"), comentarios.get("sub.puerto"));
    }

    public static final class ConComentarios {
        @Comentario({"Primera línea", "Segunda línea"})
        public boolean activo = true;

        @Seccion
        @Comentario("Bloque")
        public SubConfigComentado sub = new SubConfigComentado();
    }

    public static final class SubConfigComentado {
        @Comentario("Puerto por defecto")
        public int puerto = 8080;
    }
}
