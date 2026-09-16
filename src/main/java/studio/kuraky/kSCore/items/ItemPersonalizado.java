package studio.kuraky.kSCore.items;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Base de un item personalizado descubierto por {@link Item @Item}. La
 * clase se instancia una vez durante el arranque y sus callbacks se
 * despachan a través de un único listener interno leyendo el PDC.
 */
public abstract class ItemPersonalizado {

    /**
     * Construye la plantilla del item. Se invoca una sola vez en el
     * arranque; la instancia devuelta se clona en cada entrega.
     */
    public abstract ItemStack construir();

    /** Callback opcional: clic derecho con el item en mano. */
    public void alClicDerecho(Player jugador, ItemStack item, PlayerInteractEvent evento) {}

    /** Callback opcional: clic izquierdo con el item en mano. */
    public void alClicIzquierdo(Player jugador, ItemStack item, PlayerInteractEvent evento) {}

    /**
     * Callback opcional: el jugador golpea a otra entidad con el item
     * en la mano principal.
     */
    public void alGolpear(Player jugador, Entity objetivo, ItemStack item,
                          EntityDamageByEntityEvent evento) {}
}
