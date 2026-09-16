package studio.kuraky.kSCore.eventos;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Base para eventos personalizados lanzados desde plugins que usan el
 * núcleo. Implementa {@link Cancellable} y expone el patrón
 * {@code HandlerList} obligatorio de Bukkit.
 * <p>
 * <b>Importante:</b> Cada subclase concreta debe declarar sus propios
 * campos estáticos siguiendo el patrón de Bukkit:
 * <pre>{@code
 * private static final HandlerList handlers = new HandlerList();
 * @Override public HandlerList getHandlers() { return handlers; }
 * public static HandlerList getHandlerList() { return handlers; }
 * }</pre>
 * De lo contrario Bukkit rechazará el registro del evento.
 */
public abstract class EventoCore extends Event implements Cancellable {

    private boolean cancelado;

    protected EventoCore() {
        super();
    }

    protected EventoCore(boolean async) {
        super(async);
    }

    @Override
    public boolean isCancelled() {
        return cancelado;
    }

    @Override
    public void setCancelled(boolean cancelar) {
        this.cancelado = cancelar;
    }

    @Override
    public abstract HandlerList getHandlers();
}
