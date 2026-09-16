package studio.kuraky.kSCore.nucleo;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class Tareas {

    private final JavaPlugin plugin;
    private final Registro registro;
    private final ExecutorService virtual;
    private final Set<TareaCancelable> activas = ConcurrentHashMap.newKeySet();

    public Tareas(JavaPlugin plugin, Registro registro) {
        this.plugin = plugin;
        this.registro = registro;
        this.virtual = Executors.newVirtualThreadPerTaskExecutor();
    }

    public TareaCancelable sync(Runnable accion) {
        ScheduledTask t = plugin.getServer().getGlobalRegionScheduler()
                .run(plugin, tarea -> ejecutarSeguro("sync", accion));
        return registrar(new TareaCancelable(t::cancel));
    }

    public TareaCancelable async(Runnable accion) {
        CompletableFuture<Void> futuro = CompletableFuture.runAsync(
                () -> ejecutarSeguro("async", accion), virtual);
        return registrar(new TareaCancelable(() -> futuro.cancel(true)));
    }

    public <T> FuturoAsync<T> asyncLuegoSync(Supplier<T> trabajo, Consumer<T> continuacion) {
        CompletableFuture<T> futuro = CompletableFuture.supplyAsync(trabajo, virtual);
        FuturoAsync<T> envoltorio = new FuturoAsync<>(futuro, this);
        envoltorio.enHiloPrincipal(continuacion);
        return envoltorio;
    }

    public <T> FuturoAsync<T> async(Supplier<T> trabajo) {
        CompletableFuture<T> futuro = CompletableFuture.supplyAsync(trabajo, virtual);
        return new FuturoAsync<>(futuro, this);
    }

    public TareaCancelable repetir(long ticks, Runnable accion) {
        ScheduledTask t = plugin.getServer().getGlobalRegionScheduler()
                .runAtFixedRate(plugin, tarea -> ejecutarSeguro("repetir", accion), Math.max(1, ticks), Math.max(1, ticks));
        return registrar(new TareaCancelable(t::cancel));
    }

    public TareaCancelable retrasar(long ticks, Runnable accion) {
        ScheduledTask t = plugin.getServer().getGlobalRegionScheduler()
                .runDelayed(plugin, tarea -> ejecutarSeguro("retrasar", accion), Math.max(1, ticks));
        return registrar(new TareaCancelable(t::cancel));
    }

    public TareaCancelable enEntidad(Entity entidad, Runnable accion) {
        ScheduledTask t = entidad.getScheduler().run(plugin, tarea -> ejecutarSeguro("entidad", accion), null);
        Runnable cancelar = (t == null) ? () -> {} : t::cancel;
        return registrar(new TareaCancelable(cancelar));
    }

    public TareaCancelable enRegion(Location loc, Runnable accion) {
        ScheduledTask t = plugin.getServer().getRegionScheduler()
                .run(plugin, loc, tarea -> ejecutarSeguro("region", accion));
        return registrar(new TareaCancelable(t::cancel));
    }

    public ExecutorService ejecutorVirtual() {
        return virtual;
    }

    public void cancelarTodas() {
        for (TareaCancelable t : activas) {
            t.cancelar();
        }
        activas.clear();
        try {
            virtual.shutdownNow();
        } catch (Throwable t) {
            registro.error("Fallo al cerrar el ejecutor virtual", t);
        }
    }

    private TareaCancelable registrar(TareaCancelable tc) {
        activas.add(tc);
        return tc;
    }

    private void ejecutarSeguro(String tipo, Runnable accion) {
        try {
            accion.run();
        } catch (Throwable t) {
            registro.error("Tarea " + tipo + " lanzó excepción", t);
        }
    }
}
