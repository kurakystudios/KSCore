package studio.kuraky.kSCore.chat;

import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fachada estática del sistema de chat. Antes del arranque de
 * {@link ModuloChat} sólo funciona {@link #construir()}
 * (no depende del módulo).
 */
public final class Chat {

    private static volatile ModuloChat modulo;
    private static final AtomicInteger refs = new AtomicInteger();

    private Chat() {}

    static void inicializar(ModuloChat activo) {
        Chat.modulo = activo;
        refs.incrementAndGet();
    }

    static void desinicializar() {
        if (refs.decrementAndGet() <= 0) {
            refs.set(0);
            Chat.modulo = null;
        }
    }

    public static boolean disponible() {
        return modulo != null;
    }

    /** Inicia un builder para un mensaje interactivo. */
    public static ConstructorMensaje construir() {
        return new ConstructorMensaje();
    }

    /**
     * Registra un canal programáticamente. Sobreescribe si el id ya
     * existía (útil para reconfigurarlos en caliente).
     */
    public static Canal registrarCanal(String id, String prefijo, String permiso, String color) {
        Canal c = new Canal(id, prefijo, permiso, color);
        requerir().registrarCanal(c);
        return c;
    }

    public static Canal canal(String id) {
        return requerir().canal(id);
    }

    public static Collection<Canal> canales() {
        return requerir().canales();
    }

    /** Emite un mensaje a todos los jugadores online con permiso para el canal. */
    public static void difundirCanal(String id, Player emisor, String mensaje) {
        requerir().difundirCanal(id, emisor, mensaje);
    }

    private static ModuloChat requerir() {
        ModuloChat m = modulo;
        if (m == null) throw new IllegalStateException("Chat: ModuloChat aún no está iniciado.");
        return m;
    }
}
