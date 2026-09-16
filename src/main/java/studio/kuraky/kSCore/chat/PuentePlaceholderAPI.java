package studio.kuraky.kSCore.chat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

/**
 * Puente por reflexión a PlaceholderAPI. Si el plugin no está
 * instalado, {@link #expandir(Player, String)} devuelve el texto tal
 * cual y {@link #disponible()} devuelve {@code false}. Nunca
 * importamos {@code me.clip.placeholderapi.*} directamente para que
 * el core siga funcionando sin PAPI.
 */
public final class PuentePlaceholderAPI {

    private static volatile Boolean cache;
    private static volatile Method metodoSetPlaceholders;

    private PuentePlaceholderAPI() {}

    public static boolean disponible() {
        Boolean actual = cache;
        if (actual != null) return actual;
        try {
            if (Bukkit.getServer() == null || Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
                cache = false;
                return false;
            }
            Class<?> api = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            metodoSetPlaceholders = api.getMethod("setPlaceholders", Player.class, String.class);
            cache = true;
            return true;
        } catch (Throwable t) {
            cache = false;
            return false;
        }
    }

    /**
     * Aplica placeholders de PAPI al texto. Si PAPI no está o hay
     * fallo por reflexión, devuelve el texto sin modificar.
     */
    public static String expandir(Player jugador, String texto) {
        if (!disponible() || jugador == null || texto == null || texto.isEmpty()) return texto;
        try {
            return (String) metodoSetPlaceholders.invoke(null, jugador, texto);
        } catch (Throwable t) {
            return texto;
        }
    }

    /** Se resetea el cache si PAPI se instala/desinstala en caliente. */
    public static void resetear() {
        cache = null;
        metodoSetPlaceholders = null;
    }
}
