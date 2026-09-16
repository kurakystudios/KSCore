package studio.kuraky.kSCore.chat;

import net.kyori.adventure.text.event.ClickEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccionTest {

    private static String texto(ClickEvent<?> c) {
        return ((ClickEvent.Payload.Text) c.payload()).value();
    }

    @Test
    void ejecutar_antepone_slash_si_falta() {
        ClickEvent<?> c = Accion.ejecutar("duelo aceptar");
        assertEquals(ClickEvent.Action.RUN_COMMAND, c.action());
        assertEquals("/duelo aceptar", texto(c));
    }

    @Test
    void ejecutar_respeta_slash_existente() {
        ClickEvent<?> c = Accion.ejecutar("/spawn");
        assertEquals("/spawn", texto(c));
    }

    @Test
    void sugerir_usa_suggest_command() {
        ClickEvent<?> c = Accion.sugerir("cambiar nombre");
        assertEquals(ClickEvent.Action.SUGGEST_COMMAND, c.action());
        assertTrue(texto(c).startsWith("/"));
    }

    @Test
    void copiar_es_copy_to_clipboard() {
        ClickEvent<?> c = Accion.copiar("play.servidor.com");
        assertEquals(ClickEvent.Action.COPY_TO_CLIPBOARD, c.action());
        assertEquals("play.servidor.com", texto(c));
    }

    @Test
    void abrirUrl_es_open_url() {
        ClickEvent<?> c = Accion.abrirUrl("https://ejemplo.com");
        assertEquals(ClickEvent.Action.OPEN_URL, c.action());
    }
}
