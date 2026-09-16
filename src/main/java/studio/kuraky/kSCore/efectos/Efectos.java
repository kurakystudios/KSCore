package studio.kuraky.kSCore.efectos;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fachada estática del sistema de efectos. Enruta las llamadas al
 * {@link ModuloEfectos} activo tras el arranque.
 */
public final class Efectos {

    private static volatile ModuloEfectos modulo;
    private static final AtomicInteger refs = new AtomicInteger();

    private Efectos() {}

    static void inicializar(ModuloEfectos activo) {
        Efectos.modulo = activo;
        refs.incrementAndGet();
    }

    static void desinicializar() {
        if (refs.decrementAndGet() <= 0) {
            refs.set(0);
            Efectos.modulo = null;
        }
    }

    public static boolean disponible() {
        return modulo != null;
    }

    private static ModuloEfectos requerir() {
        ModuloEfectos m = modulo;
        if (m == null) throw new IllegalStateException("Efectos: ModuloEfectos aún no está iniciado.");
        return m;
    }

    // ---------- vanilla ----------

    public static void dar(Player jugador, PotionEffectType tipo, int nivel, Duration duracion) {
        requerir().aplicarVanilla(jugador, tipo, nivel, duracion);
    }

    public static void quitar(Player jugador, PotionEffectType tipo) {
        requerir().quitarVanilla(jugador, tipo);
    }

    public static boolean tiene(Player jugador, PotionEffectType tipo) {
        return jugador.hasPotionEffect(tipo);
    }

    // ---------- personalizados ----------

    public static void dar(Player jugador, String id, int nivel, Duration duracion) {
        requerir().aplicarPersonalizado(jugador, id, nivel, duracion);
    }

    public static void quitar(Player jugador, String id) {
        requerir().quitarPersonalizado(jugador, id);
    }

    public static boolean tiene(Player jugador, String id) {
        return requerir().tienePersonalizado(jugador, id);
    }

    /** Lista unificada de efectos vanilla + personalizados activos. */
    public static List<EfectoActivo> activos(Player jugador) {
        return requerir().activosDe(jugador);
    }

    public static Collection<String> idsRegistrados() {
        return requerir().idsRegistrados();
    }
}
