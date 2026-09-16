package studio.kuraky.kSCore.eventos;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import studio.kuraky.kSCore.nucleo.Depurador;
import studio.kuraky.kSCore.nucleo.Modulo;
import studio.kuraky.kSCore.nucleo.Nucleo;
import studio.kuraky.kSCore.nucleo.Registro;
import studio.kuraky.kSCore.nucleo.Tareas;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class ModuloEventos implements Modulo {

    /** Un listener que falle más de este tiempo (ms) en el main thread genera aviso en debug. */
    private static final long UMBRAL_LENTO_MS = 5L;

    private final List<Listener> registrados = new ArrayList<>();

    private Nucleo nucleo;
    private JavaPlugin plugin;
    private Registro registro;
    private Depurador depurador;
    private Tareas tareas;

    @Override
    public void iniciar(Nucleo nucleo) {
        this.nucleo = nucleo;
        this.plugin = nucleo.plugin();
        this.registro = nucleo.registro();
        this.depurador = nucleo.depurador();
        this.tareas = nucleo.tareas();
        Eventos.inicializar(this);

        for (Class<?> clase : nucleo.escaner().conAnotacion(Escuchador.class)) {
            if (Modifier.isAbstract(clase.getModifiers())) continue;
            try {
                Constructor<?> ctor = clase.getDeclaredConstructor();
                ctor.setAccessible(true);
                registrar(ctor.newInstance());
            } catch (NoSuchMethodException e) {
                registro.error("La clase " + clase.getName()
                        + " necesita un constructor sin argumentos para registrarse como @Escuchador.");
            } catch (ReflectiveOperationException e) {
                registro.error("No se pudo instanciar " + clase.getName(), e);
            }
        }
        depurador.log("Eventos", () -> "Escuchadores registrados: " + registrados.size());
    }

    @Override
    public void detener() {
        for (Listener l : registrados) {
            try { HandlerList.unregisterAll(l); }
            catch (Throwable ignored) {}
        }
        registrados.clear();
        Eventos.desinicializar();
        this.nucleo = null;
        this.plugin = null;
        this.registro = null;
        this.depurador = null;
        this.tareas = null;
    }

    public void registrar(Object instancia) {
        Class<?> clase = instancia.getClass();
        Listener listener = new Listener() {};
        PluginManager pm = Bukkit.getPluginManager();
        int cuenta = 0;

        for (Method metodo : clase.getDeclaredMethods()) {
            Evento anot = metodo.getAnnotation(Evento.class);
            if (anot == null) continue;
            Class<? extends Event> tipoEvento = validarMetodo(clase, metodo);
            if (tipoEvento == null) continue;

            boolean async = metodo.isAnnotationPresent(Async.class);
            boolean eventoEsAsync = esEventoAsync(tipoEvento);
            if (async && !eventoEsAsync) {
                registro.aviso("@Async rechazado en " + clase.getSimpleName() + "#" + metodo.getName()
                        + ": " + tipoEvento.getSimpleName() + " no es un evento asíncrono.");
                continue;
            }

            metodo.setAccessible(true);
            EventExecutor ejecutor = crearEjecutor(instancia, metodo, tipoEvento, async);
            EventPriority prioridad = anot.prioridad().bukkit();
            try {
                pm.registerEvent(tipoEvento, listener, prioridad, ejecutor, plugin, anot.ignorarCancelados());
                cuenta++;
            } catch (Throwable t) {
                registro.error("No se pudo registrar " + clase.getSimpleName() + "#" + metodo.getName(), t);
            }
        }

        if (cuenta > 0) registrados.add(listener);
    }

    public <E extends Event> Suscripcion escucharProgramatico(Class<E> tipo, Prioridad prioridad,
                                                              boolean ignorarCancelados,
                                                              Consumer<E> manejador) {
        Listener listener = new Listener() {};
        EventExecutor ejecutor = (list, evento) -> {
            if (!tipo.isInstance(evento)) return;
            long inicio = depurador.activo() ? System.nanoTime() : 0L;
            try {
                manejador.accept(tipo.cast(evento));
            } catch (Throwable t) {
                registro.error("Listener programático (" + tipo.getSimpleName() + ") falló", t);
            } finally {
                cronometrar(inicio, evento);
            }
        };
        Bukkit.getPluginManager().registerEvent(tipo, listener, prioridad.bukkit(), ejecutor,
                plugin, ignorarCancelados);
        registrados.add(listener);
        return new SuscripcionListener(listener);
    }

    public void programarCancelacion(long ticks, Suscripcion suscripcion) {
        tareas.retrasar(Math.max(1, ticks), suscripcion::cancelar);
    }

    public <E extends Event> void lanzarAsync(E evento) {
        tareas.async(() -> Bukkit.getPluginManager().callEvent(evento));
    }

    private EventExecutor crearEjecutor(Object instancia, Method metodo,
                                        Class<? extends Event> tipoEvento, boolean async) {
        return (listener, evento) -> {
            if (!tipoEvento.isInstance(evento)) return;
            Runnable trabajo = () -> invocar(instancia, metodo, evento);
            if (async && !evento.isAsynchronous()) {
                // Nunca debería ocurrir por la validación en arranque, pero por seguridad
                // hacemos el dispatch de todos modos.
                tareas.async(trabajo);
            } else {
                long inicio = depurador.activo() ? System.nanoTime() : 0L;
                trabajo.run();
                cronometrar(inicio, evento);
            }
        };
    }

    private void invocar(Object instancia, Method metodo, Event evento) {
        try {
            metodo.invoke(instancia, evento);
        } catch (Throwable t) {
            Throwable causa = (t instanceof java.lang.reflect.InvocationTargetException ite && ite.getCause() != null)
                    ? ite.getCause() : t;
            registro.error("Listener " + metodo.getDeclaringClass().getSimpleName() + "#" + metodo.getName()
                    + " falló", causa);
        }
    }

    private void cronometrar(long inicio, Event evento) {
        if (inicio == 0L || !depurador.activo()) return;
        long ms = (System.nanoTime() - inicio) / 1_000_000L;
        if (!evento.isAsynchronous() && ms >= UMBRAL_LENTO_MS) {
            registro.aviso("Listener lento (" + ms + "ms) manejando " + evento.getEventName());
        }
    }

    private Class<? extends Event> validarMetodo(Class<?> clase, Method metodo) {
        Class<?>[] params = metodo.getParameterTypes();
        if (params.length != 1) {
            registro.aviso(clase.getSimpleName() + "#" + metodo.getName()
                    + " debe tener exactamente un parámetro (subclase de Event).");
            return null;
        }
        if (!Event.class.isAssignableFrom(params[0])) {
            registro.aviso(clase.getSimpleName() + "#" + metodo.getName()
                    + " tiene un parámetro que no extiende Event: " + params[0].getName());
            return null;
        }
        @SuppressWarnings("unchecked")
        Class<? extends Event> tipo = (Class<? extends Event>) params[0];
        return tipo;
    }

    static boolean esEventoAsync(Class<?> tipo) {
        for (Class<?> c = tipo; c != null && c != Event.class; c = c.getSuperclass()) {
            if (c.getSimpleName().contains("Async")) return true;
        }
        return false;
    }

    private final class SuscripcionListener implements Suscripcion {
        private final Listener listener;
        private volatile boolean activa = true;

        SuscripcionListener(Listener listener) {
            this.listener = listener;
        }

        @Override
        public void cancelar() {
            if (!activa) return;
            activa = false;
            HandlerList.unregisterAll(listener);
            registrados.remove(listener);
        }

        @Override
        public boolean activa() {
            return activa;
        }
    }

    // Getters para tests.
    public int cantidadRegistrados() {
        return registrados.size();
    }

    public Plugin plugin() {
        return plugin;
    }
}
