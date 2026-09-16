package studio.kuraky.kSCore.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import studio.kuraky.kSCore.mensajes.Mensajes;
import studio.kuraky.kSCore.nucleo.TareaCancelable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Base declarativa para menús. Cada subclase implementa {@link
 * #construir()} llenando slots con {@link #boton(int, ItemStack,
 * Consumer)} y {@link #relleno(ItemStack)}. La lógica de anti-dupe y
 * el ciclo abrir/cerrar viven en el {@link ModuloGuis}.
 */
public abstract class Gui {

    private final Player jugador;
    private final int filas;
    private final PortadorGui portador;
    private final Map<Integer, Boton> botones = new HashMap<>();
    private final Set<Integer> slotsEditables = new HashSet<>();

    private Component tituloComp;
    private Inventory inventario;
    private TareaCancelable tareaRefresco;

    protected Gui(Player jugador, String titulo, int filas) {
        if (filas < 1 || filas > 6) {
            throw new IllegalArgumentException("Filas debe estar entre 1 y 6: " + filas);
        }
        this.jugador = jugador;
        this.filas = filas;
        this.tituloComp = titulo == null || titulo.isBlank()
                ? Component.empty()
                : Mensajes.parsear(titulo);
        this.portador = new PortadorGui(this);
    }

    // ---------- API declarativa ----------

    /** Se ejecuta al abrir y en cada {@link #actualizar()}. Rellena el menú. */
    protected abstract void construir();

    /** Coloca un item decorativo (sin manejador). */
    public final void boton(int slot, ItemStack item) {
        boton(slot, item, null);
    }

    /** Coloca un botón con manejador de clic. */
    public final void boton(int slot, ItemStack item, Consumer<Clic> manejador) {
        if (slot < 0 || slot >= tamano()) {
            throw new IndexOutOfBoundsException("Slot fuera de rango: " + slot);
        }
        botones.put(slot, new Boton(slot, item, manejador));
        if (inventario != null) inventario.setItem(slot, item);
    }

    /** Rellena los slots vacíos con {@code item}. */
    public final void relleno(ItemStack item) {
        for (int i = 0; i < tamano(); i++) {
            if (!botones.containsKey(i)) boton(i, item, null);
        }
    }

    /** Marca un slot como editable: el jugador puede meter/sacar items. */
    public final void slotEditable(int slot) {
        slotsEditables.add(slot);
    }

    public final boolean esSlotEditable(int slot) {
        return slotsEditables.contains(slot);
    }

    /** Cambia el título sin reabrir (usa {@code InventoryView.setTitle}). */
    public final void titulo(String texto) {
        this.tituloComp = Mensajes.parsear(texto);
        if (inventario != null && jugador.getOpenInventory().getTopInventory() == inventario) {
            jugador.getOpenInventory().setTitle(Mensajes.sinFormato(texto));
        }
    }

    /**
     * Programa una acción periódica que se ejecuta cada {@code ticks}
     * ticks mientras la GUI esté abierta. El propio ciclo cancela la
     * tarea al cerrar la GUI.
     */
    public final void actualizarCada(long ticks, Runnable accion) {
        Guis.programarRefresco(this, ticks, accion);
    }

    // ---------- ciclo de vida ----------

    /** Abre la GUI para el jugador. Idempotente si ya estaba abierta. */
    public final void abrir() {
        Guis.registrarApertura(this);
    }

    /** Cierra la GUI de forma segura (1 tick de retraso si estamos dentro de un evento). */
    public final void cerrar() {
        Guis.programarCierre(this);
    }

    /** Redibuja la GUI: limpia botones, ejecuta construir() y reaplica. */
    public final void actualizar() {
        botones.clear();
        construir();
        if (inventario != null) {
            inventario.clear();
            for (Boton b : botones.values()) inventario.setItem(b.slot(), b.item());
        }
    }

    // ---------- accesores usados por el módulo ----------

    public final Player jugador() { return jugador; }
    public final int filas() { return filas; }
    public final int tamano() { return filas * 9; }
    public final PortadorGui portador() { return portador; }
    public final Component titulo() { return tituloComp; }
    public final Map<Integer, Boton> botones() { return botones; }
    public final Set<Integer> slotsEditables() { return slotsEditables; }
    public final Inventory inventario() { return inventario; }

    final void inventario(Inventory inv) {
        this.inventario = inv;
        portador.adjuntar(inv);
    }

    final void tareaRefresco(TareaCancelable t) {
        this.tareaRefresco = t;
    }

    final TareaCancelable tareaRefresco() {
        return tareaRefresco;
    }

    /**
     * Se invoca justo antes de que el inventario se cree y se le
     * inyecten los items. Llamado por el módulo.
     */
    final void invocarConstruir() {
        botones.clear();
        construir();
    }

    /**
     * Crea el inventario Bukkit del tamaño correcto con este portador.
     * Público a nivel de paquete: sólo el módulo debe llamarlo.
     */
    final Inventory crearInventario() {
        return Bukkit.createInventory(portador, tamano(), tituloComp);
    }
}
