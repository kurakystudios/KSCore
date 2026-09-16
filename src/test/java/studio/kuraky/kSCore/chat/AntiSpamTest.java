package studio.kuraky.kSCore.chat;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AntiSpamTest {

    @Test
    void primer_mensaje_no_bloqueado() {
        AntiSpam as = new AntiSpam();
        assertNull(as.evaluar(UUID.randomUUID(), "hola", 5, true));
    }

    @Test
    void segundo_inmediato_es_cooldown() {
        AntiSpam as = new AntiSpam();
        UUID u = UUID.randomUUID();
        assertNull(as.evaluar(u, "a", 5, true));
        String razon = as.evaluar(u, "b", 5, true);
        assertNotNull(razon);
        assertTrue(razon.startsWith("cooldown:"), "esperado cooldown:*, fue " + razon);
    }

    @Test
    void repetido_bloqueado_si_activo() {
        AntiSpam as = new AntiSpam();
        UUID u = UUID.randomUUID();
        assertNull(as.evaluar(u, "hola", 0, true));
        assertNull(as.evaluar(u, "otro", 0, true));
        // Ahora repetimos otro.
        String razon = as.evaluar(u, "otro", 0, true);
        assertNotNull(razon);
    }

    @Test
    void olvidar_reinicia_contador() {
        AntiSpam as = new AntiSpam();
        UUID u = UUID.randomUUID();
        as.evaluar(u, "a", 60, true);
        as.olvidar(u);
        assertNull(as.evaluar(u, "b", 60, true));
    }

    @Test
    void cooldown_cero_no_bloquea_por_tiempo() {
        AntiSpam as = new AntiSpam();
        UUID u = UUID.randomUUID();
        assertNull(as.evaluar(u, "1", 0, false));
        assertNull(as.evaluar(u, "2", 0, false));
        assertNull(as.evaluar(u, "3", 0, false));
    }

    @Test
    void bloqueo_repetidos_desactivado() {
        AntiSpam as = new AntiSpam();
        UUID u = UUID.randomUUID();
        assertNull(as.evaluar(u, "hola", 0, false));
        assertNull(as.evaluar(u, "hola", 0, false));
    }
}
