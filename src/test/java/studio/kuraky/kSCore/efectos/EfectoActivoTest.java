package studio.kuraky.kSCore.efectos;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EfectoActivoTest {

    private static EfectoActivo nuevo(long ticks, int intervalo) {
        return new EfectoActivo(UUID.randomUUID(), "test", 1, ticks, intervalo, true);
    }

    @Test
    void expirado_es_false_al_crear() {
        assertFalse(nuevo(20, 20).expirado());
    }

    @Test
    void tick_decrementa_los_restantes() {
        EfectoActivo e = nuevo(3, 1);
        e.tick(); e.tick(); e.tick();
        assertTrue(e.expirado());
    }

    @Test
    void tocaTick_cada_intervalo() {
        EfectoActivo e = nuevo(1000, 5);
        // tickInterno inicia en 0 → tocaTick=true en la primera vuelta
        assertTrue(e.tocaTick());
        e.tick(); // interno=1
        assertFalse(e.tocaTick());
        for (int i = 0; i < 4; i++) e.tick(); // interno=5
        assertTrue(e.tocaTick());
    }

    @Test
    void sumar_extiende_la_duracion() {
        EfectoActivo e = nuevo(20, 20);
        e.sumar(40);
        assertEquals(60, e.ticksRestantes());
    }

    @Test
    void asignar_reemplaza_la_duracion() {
        EfectoActivo e = nuevo(20, 20);
        e.asignarRestantes(5);
        assertEquals(5, e.ticksRestantes());
    }

    @Test
    void intervalo_minimo_es_uno() {
        EfectoActivo e = new EfectoActivo(UUID.randomUUID(), "t", 1, 20, 0, true);
        assertEquals(1, e.intervaloTicks());
    }
}
