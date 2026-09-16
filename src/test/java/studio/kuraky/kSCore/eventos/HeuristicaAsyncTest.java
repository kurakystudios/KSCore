package studio.kuraky.kSCore.eventos;

import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeuristicaAsyncTest {

    /** Padre ficticio cuyo nombre contiene "Async" para probar la herencia. */
    public static class BaseAsyncFalsa extends Event {
        private static final HandlerList handlers = new HandlerList();
        public BaseAsyncFalsa() { super(true); }
        @Override public HandlerList getHandlers() { return handlers; }
        public static HandlerList getHandlerList() { return handlers; }
    }

    /** Subclase de evento async → hereda la característica por el nombre del padre. */
    public static final class MiEventoDerivado extends BaseAsyncFalsa {
    }

    /** Evento personalizado no marcado como async. */
    public static final class EventoSync extends Event {
        private static final HandlerList handlers = new HandlerList();
        @Override public HandlerList getHandlers() { return handlers; }
        public static HandlerList getHandlerList() { return handlers; }
    }

    @Test
    void detecta_paper_async_chat() {
        assertTrue(ModuloEventos.esEventoAsync(AsyncChatEvent.class));
    }

    @Test
    void detecta_async_player_pre_login() {
        assertTrue(ModuloEventos.esEventoAsync(AsyncPlayerPreLoginEvent.class));
    }

    @Test
    void rechaza_sync_player_join() {
        assertFalse(ModuloEventos.esEventoAsync(PlayerJoinEvent.class));
    }

    @Test
    void rechaza_sync_player_quit() {
        assertFalse(ModuloEventos.esEventoAsync(PlayerQuitEvent.class));
    }

    @Test
    void subclase_de_async_hereda_por_nombre_del_padre() {
        assertTrue(ModuloEventos.esEventoAsync(MiEventoDerivado.class));
    }

    @Test
    void evento_sync_personalizado_no_es_async() {
        assertFalse(ModuloEventos.esEventoAsync(EventoSync.class));
    }
}
