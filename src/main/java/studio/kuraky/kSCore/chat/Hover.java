package studio.kuraky.kSCore.chat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.HoverEventSource;
import org.bukkit.inventory.ItemStack;
import studio.kuraky.kSCore.mensajes.Mensajes;

/**
 * Factorías de acciones de hover.
 */
public final class Hover {

    private Hover() {}

    public static HoverEvent<Component> texto(String texto) {
        return HoverEvent.showText(Mensajes.parsear(texto));
    }

    public static HoverEvent<Component> texto(Component componente) {
        return HoverEvent.showText(componente);
    }

    /**
     * Muestra el tooltip completo del item al pasar el ratón. Delega
     * en la implementación de Paper ({@code ItemStack.asHoverEvent}).
     */
    public static HoverEventSource<?> item(ItemStack item) {
        return item;
    }
}
