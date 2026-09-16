package studio.kuraky.kSCore.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * {@link InventoryHolder} propio: la única forma segura de identificar
 * que un inventario pertenece a este sistema es {@code getHolder()
 * instanceof PortadorGui}. Comparar por título es inseguro porque los
 * plugins pueden abrir varios menús con nombres idénticos.
 */
public final class PortadorGui implements InventoryHolder {

    private final Gui gui;
    private Inventory inventario;

    public PortadorGui(Gui gui) {
        this.gui = gui;
    }

    public Gui gui() {
        return gui;
    }

    void adjuntar(Inventory inventario) {
        this.inventario = inventario;
    }

    @Override
    public Inventory getInventory() {
        return inventario;
    }
}
