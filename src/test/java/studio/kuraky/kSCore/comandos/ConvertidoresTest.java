package studio.kuraky.kSCore.comandos;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConvertidoresTest {

    enum Prioridad { BAJA, MEDIA, ALTA }

    @Test
    void tipos_basicos_disponibles() {
        assertNotNull(Convertidores.obtener(String.class));
        assertNotNull(Convertidores.obtener(int.class));
        assertNotNull(Convertidores.obtener(Integer.class));
        assertNotNull(Convertidores.obtener(long.class));
        assertNotNull(Convertidores.obtener(double.class));
        assertNotNull(Convertidores.obtener(boolean.class));
        assertNotNull(Convertidores.obtener(UUID.class));
        assertNotNull(Convertidores.obtener(Duration.class));
    }

    @Test
    void int_convierte_correctamente() {
        Convertidor<Integer> c = Convertidores.obtener(int.class);
        assertEquals(42, c.convertir(null, "42"));
        assertThrows(ErrorArgumento.class, () -> c.convertir(null, "abc"));
    }

    @Test
    void long_admite_negativos() {
        Convertidor<Long> c = Convertidores.obtener(long.class);
        assertEquals(-7L, c.convertir(null, "-7"));
    }

    @Test
    void double_convierte_notacion_estandar() {
        Convertidor<Double> c = Convertidores.obtener(double.class);
        assertEquals(3.5d, c.convertir(null, "3.5"));
        assertThrows(ErrorArgumento.class, () -> c.convertir(null, "no-numero"));
    }

    @Test
    void boolean_admite_variantes() {
        Convertidor<Boolean> c = Convertidores.obtener(boolean.class);
        assertEquals(true, c.convertir(null, "true"));
        assertEquals(true, c.convertir(null, "si"));
        assertEquals(true, c.convertir(null, "YES"));
        assertEquals(false, c.convertir(null, "false"));
        assertEquals(false, c.convertir(null, "no"));
        assertThrows(ErrorArgumento.class, () -> c.convertir(null, "quizás"));
    }

    @Test
    void enum_convierte_case_insensitive() {
        Convertidor<Prioridad> c = Convertidores.obtener(Prioridad.class);
        assertNotNull(c);
        assertEquals(Prioridad.ALTA, c.convertir(null, "alta"));
        assertEquals(Prioridad.MEDIA, c.convertir(null, "MEDIA"));
        assertThrows(ErrorArgumento.class, () -> c.convertir(null, "critica"));
    }

    @Test
    void enum_sugerencias_filtra_por_prefijo() {
        Convertidor<Prioridad> c = Convertidores.obtener(Prioridad.class);
        assertTrue(c.sugerencias(null, "").containsAll(java.util.List.of("BAJA", "MEDIA", "ALTA")));
        assertEquals(java.util.List.of("MEDIA"), c.sugerencias(null, "me"));
    }

    @Test
    void uuid_convierte_o_falla() {
        Convertidor<UUID> c = Convertidores.obtener(UUID.class);
        UUID u = UUID.randomUUID();
        assertEquals(u, c.convertir(null, u.toString()));
        assertThrows(ErrorArgumento.class, () -> c.convertir(null, "no-uuid"));
    }

    @Test
    void duration_usa_tiempos() {
        Convertidor<Duration> c = Convertidores.obtener(Duration.class);
        assertEquals(Duration.ofMinutes(90), c.convertir(null, "1h30m"));
        assertThrows(ErrorArgumento.class, () -> c.convertir(null, "xyz"));
    }

    @Test
    void tipo_no_registrado_devuelve_null() {
        assertNull(Convertidores.obtener(Object.class));
    }

    @Test
    void registro_personalizado() {
        record Punto(int x, int y) {}
        Convertidor<Punto> conv = (ctx, t) -> {
            String[] parts = t.split(",");
            return new Punto(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        };
        Convertidores.registrar(Punto.class, conv);
        assertEquals(new Punto(3, 5), Convertidores.obtener(Punto.class).convertir(null, "3,5"));
    }

    @Test
    void varargs_absorbe_resto() {
        Convertidor<String[]> c = Convertidores.obtener(String[].class);
        assertTrue(c.absorbeResto());
        assertEquals(3, c.convertir(null, "uno dos tres").length);
    }
}
