package studio.kuraky.kSCore.eventos;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Fachada estática del sistema de eventos. La suscripción programática
 * y el lanzado de eventos personalizados están disponibles después de
 * que {@link ModuloEventos} arranque.
 */
public final class Eventos {

    private static volatile ModuloEventos modulo;
    private static final AtomicInteger refs = new AtomicInteger();

    private Eventos() {}

    static void inicializar(ModuloEventos activo) {
        Eventos.modulo = activo;
        refs.incrementAndGet();
    }

    static void desinicializar() {
        if (refs.decrementAndGet() <= 0) {
            refs.set(0);
            Eventos.modulo = null;
        }
    }

    /** Registra una instancia ya construida por el usuario. */
    public static void registrar(Object instancia) {
        requerir().registrar(instancia);
    }

    /**
     * Registra un listener puntual. Devuelve una {@link Suscripcion}
     * que permite cancelarlo. Prioridad normal, no ignora cancelados.
     */
    public static <E extends Event> Suscripcion escuchar(Class<E> tipo, Consumer<E> manejador) {
        return escuchar(tipo, Prioridad.NORMAL, false, manejador);
    }

    public static <E extends Event> Suscripcion escuchar(Class<E> tipo, Prioridad prioridad,
                                                         boolean ignorarCancelados,
                                                         Consumer<E> manejador) {
        return requerir().escucharProgramatico(tipo, prioridad, ignorarCancelados, manejador);
    }

    /**
     * Registra un listener que se cancela automáticamente tras el
     * primer disparo.
     */
    public static <E extends Event> Suscripcion unaVez(Class<E> tipo, Consumer<E> manejador) {
        AtomicReference<Suscripcion> ref = new AtomicReference<>();
        Suscripcion s = escuchar(tipo, evento -> {
            Suscripcion mia = ref.get();
            if (mia != null) mia.cancelar();
            manejador.accept(evento);
        });
        ref.set(s);
        return s;
    }

    /**
     * Registra un listener que se cancela automáticamente al cabo de
     * {@code ticks} ticks del servidor.
     */
    public static <E extends Event> Suscripcion durante(long ticks, Class<E> tipo, Consumer<E> manejador) {
        Suscripcion s = escuchar(tipo, manejador);
        requerir().programarCancelacion(ticks, s);
        return s;
    }

    /** Dispara un evento en el hilo actual. */
    public static <E extends Event> E lanzar(E evento) {
        Bukkit.getPluginManager().callEvent(evento);
        return evento;
    }

    /** Dispara un evento desde un hilo virtual (no bloquea al invocador). */
    public static <E extends Event> void lanzarAsync(E evento) {
        requerir().lanzarAsync(evento);
    }

    private static ModuloEventos requerir() {
        ModuloEventos m = modulo;
        if (m == null) throw new IllegalStateException("Eventos: ModuloEventos aún no está iniciado.");
        return m;
    }

    /**
     * Utilidad de bajo nivel: expone la construcción de un {@link
     * EventExecutor} con captura de excepciones para código que use
     * directamente el {@link org.bukkit.plugin.PluginManager}.
     */
    public static <E extends Event> EventExecutor capturaExcepciones(Class<E> tipo, Consumer<E> manejador) {
        return (listener, evento) -> {
            if (!tipo.isInstance(evento)) return;
            try {
                manejador.accept(tipo.cast(evento));
            } catch (Throwable t) {
                Bukkit.getLogger().warning("[Core] Listener programático falló: " + t);
            }
        };
    }

    /** Utilidad para desuscribir un listener manualmente. */
    public static void desregistrar(Listener listener) {
        HandlerList.unregisterAll(listener);
    }

    /** Conversión externa Prioridad → EventPriority sin depender del enum interno. */
    public static EventPriority prioridadBukkit(Prioridad p) {
        return p.bukkit();
    }
}
