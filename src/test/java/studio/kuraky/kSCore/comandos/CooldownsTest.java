package studio.kuraky.kSCore.comandos;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CooldownsTest {

    @SuppressWarnings("unused")
    private static void metodoFalso() {}

    private static Method metodo() throws NoSuchMethodException {
        return CooldownsTest.class.getDeclaredMethod("metodoFalso");
    }

    @Test
    void sin_marca_previa_no_hay_cooldown() throws NoSuchMethodException {
        Cooldowns cd = new Cooldowns();
        assertEquals(0L, cd.tiempoRestante(metodo(), UUID.randomUUID(), 10));
    }

    @Test
    void tras_marca_esta_en_cooldown() throws NoSuchMethodException {
        Cooldowns cd = new Cooldowns();
        UUID u = UUID.randomUUID();
        cd.marcar(metodo(), u);
        long restante = cd.tiempoRestante(metodo(), u, 10);
        assertTrue(restante > 0 && restante <= 10);
    }

    @Test
    void cooldown_es_por_jugador() throws NoSuchMethodException {
        Cooldowns cd = new Cooldowns();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        cd.marcar(metodo(), a);
        assertTrue(cd.tiempoRestante(metodo(), a, 10) > 0);
        assertEquals(0L, cd.tiempoRestante(metodo(), b, 10));
    }

    @Test
    void limpiar_borra_todo() throws NoSuchMethodException {
        Cooldowns cd = new Cooldowns();
        UUID u = UUID.randomUUID();
        cd.marcar(metodo(), u);
        cd.limpiar();
        assertEquals(0L, cd.tiempoRestante(metodo(), u, 10));
    }
}
