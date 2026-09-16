package studio.kuraky.kSCore.gui;

import org.bukkit.entity.Player;
import studio.kuraky.kSCore.nucleo.TareaCancelable;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Fachada estática del sistema de GUIs. Antes de que arranque
 * {@link ModuloGuis} lanza {@link IllegalStateException} en cualquier
 * llamada.
 */
public final class Guis {

    private static volatile ModuloGuis modulo;
    private static final AtomicInteger refs = new AtomicInteger();

    private Guis() {}

    static void inicializar(ModuloGuis activo) {
        Guis.modulo = activo;
        refs.incrementAndGet();
    }

    static void desinicializar() {
        if (refs.decrementAndGet() <= 0) {
            refs.set(0);
            Guis.modulo = null;
        }
    }

    public static boolean disponible() {
        return modulo != null;
    }

    private static ModuloGuis requerir() {
        ModuloGuis m = modulo;
        if (m == null) throw new IllegalStateException("Guis: ModuloGuis aún no está iniciado.");
        return m;
    }

    /** Instancia la clase pasando al jugador al constructor y la abre. */
    public static <G extends Gui> G abrir(Player jugador, Class<G> claseGui) {
        return requerir().abrirPorClase(jugador, claseGui);
    }

    /** Busca una clase registrada con {@link Menu @Menu} por id y la abre. */
    public static Gui abrir(Player jugador, String id) {
        return requerir().abrirPorId(jugador, id);
    }

    /** Cierra la GUI abierta por el jugador (si hay). */
    public static void cerrar(Player jugador) {
        requerir().cerrarDe(jugador);
    }

    /** Cierra todas las GUIs abiertas del core. */
    public static void cerrarTodo() {
        requerir().cerrarTodas();
    }

    public static Gui abierta(Player jugador) {
        return requerir().abiertaDe(jugador);
    }

    public static Collection<String> nombresRegistrados() {
        return requerir().nombresRegistrados();
    }

    // ---------- helpers usados desde Gui ----------

    static void registrarApertura(Gui gui) {
        requerir().abrirInstancia(gui);
    }

    static void programarCierre(Gui gui) {
        requerir().programarCierre(gui);
    }

    static void programarRefresco(Gui gui, long ticks, Runnable accion) {
        requerir().programarRefresco(gui, ticks, accion);
    }

    /** Guarda una tarea cancelable asociada a la GUI (usado por tests avanzados). */
    static void asociarTarea(Gui gui, TareaCancelable tarea, Consumer<Gui> ignored) {
        gui.tareaRefresco(tarea);
    }
}
