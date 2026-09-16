package studio.kuraky.kSCore.eventos;

import org.bukkit.event.HandlerList;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventoCoreTest {

    /** Evento sync personalizado con la convención requerida. */
    public static final class EventoTest extends EventoCore {
        private static final HandlerList handlers = new HandlerList();

        @Override public HandlerList getHandlers() { return handlers; }
        public static HandlerList getHandlerList() { return handlers; }
    }

    /** Evento async personalizado (llama a super(true)). */
    public static final class EventoAsyncTest extends EventoCore {
        private static final HandlerList handlers = new HandlerList();

        public EventoAsyncTest() { super(true); }

        @Override public HandlerList getHandlers() { return handlers; }
        public static HandlerList getHandlerList() { return handlers; }
    }

    @Test
    void cancellable_por_defecto_no_cancelado() {
        EventoTest e = new EventoTest();
        assertFalse(e.isCancelled());
    }

    @Test
    void set_cancelled_persiste() {
        EventoTest e = new EventoTest();
        e.setCancelled(true);
        assertTrue(e.isCancelled());
        e.setCancelled(false);
        assertFalse(e.isCancelled());
    }

    @Test
    void handler_list_no_nulo() {
        EventoTest e = new EventoTest();
        assertNotNull(e.getHandlers());
        assertSame(EventoTest.getHandlerList(), e.getHandlers());
    }

    @Test
    void constructor_async_marca_evento_asincrono() {
        EventoAsyncTest e = new EventoAsyncTest();
        assertTrue(e.isAsynchronous());
    }
}
