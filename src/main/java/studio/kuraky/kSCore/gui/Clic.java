package studio.kuraky.kSCore.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Contexto pasado al manejador de un {@link Boton}. Envuelve el
 * {@link InventoryClickEvent} original (ya cancelado por defecto para
 * evitar duplicaciones) y expone utilidades legibles.
 */
public final class Clic {

    private final Gui gui;
    private final InventoryClickEvent evento;

    Clic(Gui gui, InventoryClickEvent evento) {
        this.gui = gui;
        this.evento = evento;
    }

    public Gui gui() {
        return gui;
    }

    public Player jugador() {
        return (Player) evento.getWhoClicked();
    }

    public int slot() {
        return evento.getSlot();
    }

    public ItemStack item() {
        return evento.getCurrentItem();
    }

    public ClickType tipo() {
        return evento.getClick();
    }

    public boolean esIzquierdo() {
        ClickType t = evento.getClick();
        return t == ClickType.LEFT || t == ClickType.SHIFT_LEFT;
    }

    public boolean esDerecho() {
        ClickType t = evento.getClick();
        return t == ClickType.RIGHT || t == ClickType.SHIFT_RIGHT;
    }

    public boolean esShift() {
        ClickType t = evento.getClick();
        return t == ClickType.SHIFT_LEFT || t == ClickType.SHIFT_RIGHT;
    }

    public boolean esMedio() {
        return evento.getClick() == ClickType.MIDDLE;
    }

    public boolean esTecla() {
        return evento.getClick() == ClickType.NUMBER_KEY;
    }

    /** Devuelve la tecla numérica (1-9) usada, o 0 si no fue número. */
    public int tecla() {
        return esTecla() ? evento.getHotbarButton() + 1 : 0;
    }

    /** Cierra la GUI tras el clic (retrasado 1 tick para evitar reentrar). */
    public void cerrar() {
        gui.cerrar();
    }

    /** Redibuja la GUI (re-ejecuta {@code construir()}). */
    public void actualizar() {
        gui.actualizar();
    }

    /**
     * El evento ya está cancelado por defecto. Este método existe por
     * simetría con la API: si un botón editable quisiera bloquear un
     * clic concreto, puede llamarlo.
     */
    public void cancelar() {
        evento.setCancelled(true);
    }

    public InventoryClickEvent evento() {
        return evento;
    }
}
