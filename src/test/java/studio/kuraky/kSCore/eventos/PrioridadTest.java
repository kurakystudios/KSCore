package studio.kuraky.kSCore.eventos;

import org.bukkit.event.EventPriority;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PrioridadTest {

    @Test
    void mapeo_completo_a_bukkit() {
        assertEquals(EventPriority.LOWEST, Prioridad.MAS_BAJA.bukkit());
        assertEquals(EventPriority.LOW, Prioridad.BAJA.bukkit());
        assertEquals(EventPriority.NORMAL, Prioridad.NORMAL.bukkit());
        assertEquals(EventPriority.HIGH, Prioridad.ALTA.bukkit());
        assertEquals(EventPriority.HIGHEST, Prioridad.MAS_ALTA.bukkit());
        assertEquals(EventPriority.MONITOR, Prioridad.MONITOR.bukkit());
    }

    @Test
    void fachada_devuelve_prioridad_bukkit() {
        assertEquals(EventPriority.HIGH, Eventos.prioridadBukkit(Prioridad.ALTA));
    }
}
