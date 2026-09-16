package studio.kuraky.kSCore.gui;

import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

/**
 * Un slot de la GUI con el item que se muestra y el manejador de clic
 * opcional. Un botón sin manejador es un item decorativo (no dispara
 * lógica pero sigue siendo click-safe).
 */
public record Boton(int slot, ItemStack item, Consumer<Clic> manejador) {

    public boolean tieneManejador() {
        return manejador != null;
    }
}
