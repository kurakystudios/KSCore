package studio.kuraky.kSCore.mensajes;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class Mensajes {

    private static volatile Analizador analizador;
    private static volatile Map<String, String> cadenas = Map.of();
    private static final AtomicInteger refs = new AtomicInteger();

    private Mensajes() {}

    static synchronized void inicializar(Analizador analizador, Map<String, String> cadenas) {
        refs.incrementAndGet();
        // Primera inicialización: adopta analizador y cadenas tal cual.
        // Subsecuentes (otro Nucleo arrancando en la misma JVM): fusiona sin
        // sobrescribir el analizador ni las cadenas ya aportadas por otros
        // plugins vía agregarCadenas().
        if (Mensajes.analizador == null) {
            Mensajes.analizador = analizador;
            Mensajes.cadenas = Map.copyOf(cadenas);
        } else if (cadenas != null && !cadenas.isEmpty()) {
            java.util.Map<String, String> merged = new java.util.LinkedHashMap<>(Mensajes.cadenas);
            merged.putAll(cadenas);
            Mensajes.cadenas = Map.copyOf(merged);
            Mensajes.analizador.invalidarCache();
        }
    }

    static void actualizarCadenas(Map<String, String> nuevas) {
        Mensajes.cadenas = Map.copyOf(nuevas);
        Analizador local = analizador;
        if (local != null) local.invalidarCache();
    }

    /**
     * Fusiona un conjunto de cadenas encima del mapa actual sin
     * sobrescribir la totalidad. Pensado para plugins que consumen
     * KsCore embebido y necesitan aportar sus propias claves de
     * mensaje. Las claves duplicadas se pisan con las nuevas.
     */
    public static void agregarCadenas(Map<String, String> extras) {
        if (extras == null || extras.isEmpty()) return;
        java.util.Map<String, String> merged = new java.util.LinkedHashMap<>(cadenas);
        merged.putAll(extras);
        Mensajes.cadenas = Map.copyOf(merged);
        Analizador local = analizador;
        if (local != null) local.invalidarCache();
    }

    static synchronized void desinicializar() {
        if (refs.decrementAndGet() <= 0) {
            refs.set(0);
            Mensajes.analizador = null;
            Mensajes.cadenas = Map.of();
        }
    }

    public static boolean disponible() {
        return analizador != null;
    }

    public static Component parsear(String texto) {
        return requerir().analizar(texto);
    }

    public static Component parsear(String texto, Map<String, ?> variables) {
        return requerir().analizar(texto, variables);
    }

    public static void enviar(Audience destino, String texto) {
        destino.sendMessage(parsear(texto));
    }

    public static void enviar(Audience destino, String texto, Map<String, ?> variables) {
        destino.sendMessage(parsear(texto, variables));
    }

    public static void difundir(String texto) {
        Bukkit.getServer().sendMessage(parsear(texto));
    }

    public static void difundir(String texto, Map<String, ?> variables) {
        Bukkit.getServer().sendMessage(parsear(texto, variables));
    }

    public static void titulo(Audience destino, String arriba, String abajo,
                              Duration entrada, Duration duracion, Duration salida) {
        Title.Times tiempos = Title.Times.times(entrada, duracion, salida);
        destino.showTitle(Title.title(parsear(arriba), parsear(abajo), tiempos));
    }

    public static void barraAccion(Audience destino, String texto) {
        destino.sendActionBar(parsear(texto));
    }

    public static String sinFormato(String texto) {
        return requerir().sinFormato(texto);
    }

    public static String de(String clave) {
        String valor = cadenas.get(clave);
        return valor != null ? valor : clave;
    }

    public static Component parsearDe(String clave) {
        return parsear(de(clave));
    }

    public static Component parsearDe(String clave, Map<String, ?> variables) {
        return parsear(de(clave), variables);
    }

    public static void enviarDe(Audience destino, String clave) {
        enviar(destino, de(clave));
    }

    public static void enviarDe(Audience destino, String clave, Map<String, ?> variables) {
        enviar(destino, de(clave), variables);
    }

    private static Analizador requerir() {
        Analizador local = analizador;
        if (local == null) {
            throw new IllegalStateException(
                    "Mensajes no inicializado: ModuloMensajes aún no se ha registrado en Nucleo.");
        }
        return local;
    }
}
