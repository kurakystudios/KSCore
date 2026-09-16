package studio.kuraky.kSCore.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import studio.kuraky.kSCore.eventos.Eventos;
import studio.kuraky.kSCore.eventos.Prioridad;
import studio.kuraky.kSCore.eventos.Suscripcion;
import studio.kuraky.kSCore.nucleo.Modulo;
import studio.kuraky.kSCore.nucleo.Nucleo;
import studio.kuraky.kSCore.nucleo.TareaCancelable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ModuloGuis implements Modulo {

    /** Acciones permitidas en un slot editable dentro de la GUI. */
    private static final Set<InventoryAction> ACCIONES_EDITABLES_PERMITIDAS = EnumSet.of(
            InventoryAction.PLACE_ALL,
            InventoryAction.PLACE_SOME,
            InventoryAction.PLACE_ONE,
            InventoryAction.PICKUP_ALL,
            InventoryAction.PICKUP_HALF,
            InventoryAction.PICKUP_SOME,
            InventoryAction.PICKUP_ONE,
            InventoryAction.SWAP_WITH_CURSOR,
            InventoryAction.DROP_ALL_SLOT,
            InventoryAction.DROP_ONE_SLOT
    );

    private static final long DEBOUNCE_MS = 100L;

    private final Map<UUID, Gui> abiertas = new ConcurrentHashMap<>();
    private final Map<String, Class<? extends Gui>> registradasPorId = new ConcurrentHashMap<>();
    private final Map<UUID, Long> ultimoClic = new ConcurrentHashMap<>();
    private final List<Suscripcion> suscripciones = new ArrayList<>();

    private Nucleo nucleo;
    private boolean primario;

    @Override
    public void iniciar(Nucleo nucleo) {
        this.nucleo = nucleo;
        // Con múltiples plugins consumiendo KSCore standalone, solo el primer
        // ModuloGuis en arrancar registra listeners de InventoryClickEvent
        // etc. Los demás delegan en la fachada Guis, que apunta al primario.
        // Si duplicamos, cada InventoryClickEvent ejecuta el handler del
        // botón N veces → close+open en cadena → el cliente pierde la GUI.
        if (Guis.disponible()) {
            this.primario = false;
            return;
        }
        this.primario = true;
        Guis.inicializar(this);
        descubrirRegistradas();
        registrarEventos();
    }

    @Override
    public void detener() {
        if (!primario) {
            this.nucleo = null;
            return;
        }
        for (Suscripcion s : suscripciones) {
            try { s.cancelar(); } catch (Throwable ignored) {}
        }
        suscripciones.clear();
        cerrarTodas();
        abiertas.clear();
        registradasPorId.clear();
        ultimoClic.clear();
        Guis.desinicializar();
        this.nucleo = null;
    }

    // ---------- API ----------

    public <G extends Gui> G abrirPorClase(Player jugador, Class<G> clase) {
        try {
            Constructor<G> ctor = clase.getDeclaredConstructor(Player.class);
            ctor.setAccessible(true);
            G gui = ctor.newInstance(jugador);
            gui.abrir();
            return gui;
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(clase.getName()
                    + " necesita un constructor (Player) para abrirse por clase.");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("No se pudo instanciar " + clase.getName(), e);
        }
    }

    public Gui abrirPorId(Player jugador, String id) {
        Class<? extends Gui> clase = registradasPorId.get(id);
        if (clase == null) throw new IllegalArgumentException("Menú desconocido: " + id);
        return abrirPorClase(jugador, clase);
    }

    public void cerrarDe(Player jugador) {
        Gui gui = abiertas.get(jugador.getUniqueId());
        if (gui != null) programarCierre(gui);
    }

    public void cerrarTodas() {
        for (Gui g : new ArrayList<>(abiertas.values())) {
            try { g.jugador().closeInventory(); } catch (Throwable ignored) {}
        }
    }

    public Gui abiertaDe(Player jugador) {
        return abiertas.get(jugador.getUniqueId());
    }

    public Collection<String> nombresRegistrados() {
        return List.copyOf(registradasPorId.keySet());
    }

    void abrirInstancia(Gui gui) {
        Inventory inv = gui.crearInventario();
        gui.inventario(inv);
        gui.invocarConstruir();
        for (Boton b : gui.botones().values()) {
            inv.setItem(b.slot(), b.item());
        }
        abiertas.put(gui.jugador().getUniqueId(), gui);
        gui.jugador().openInventory(inv);
    }

    void programarCierre(Gui gui) {
        nucleo.tareas().retrasar(1, () -> {
            try { gui.jugador().closeInventory(); } catch (Throwable ignored) {}
        });
    }

    void programarRefresco(Gui gui, long ticks, Runnable accion) {
        TareaCancelable actual = gui.tareaRefresco();
        if (actual != null) actual.cancelar();
        TareaCancelable nueva = nucleo.tareas().repetir(Math.max(1, ticks), () -> {
            if (!abiertas.containsKey(gui.jugador().getUniqueId())) return;
            try { accion.run(); } catch (Throwable t) {
                nucleo.registro().error("actualizarCada falló en " + gui.getClass().getSimpleName(), t);
            }
        });
        gui.tareaRefresco(nueva);
    }

    // ---------- Descubrimiento ----------

    private void descubrirRegistradas() {
        for (Class<?> clase : nucleo.escaner().conAnotacion(Menu.class)) {
            if (Modifier.isAbstract(clase.getModifiers())) continue;
            if (!Gui.class.isAssignableFrom(clase)) {
                nucleo.registro().aviso(clase.getName() + " tiene @Menu pero no extiende Gui.");
                continue;
            }
            Menu m = clase.getAnnotation(Menu.class);
            @SuppressWarnings("unchecked")
            Class<? extends Gui> claseGui = (Class<? extends Gui>) clase;
            registradasPorId.put(m.id(), claseGui);
            nucleo.depurador().log("Guis", () -> "Menú @" + m.id() + " → " + clase.getSimpleName());
        }
    }

    // ---------- Anti-dupe ----------

    private void registrarEventos() {
        suscripciones.add(Eventos.escuchar(InventoryClickEvent.class, Prioridad.MAS_ALTA, false, this::alClic));
        suscripciones.add(Eventos.escuchar(InventoryDragEvent.class, Prioridad.MAS_ALTA, false, this::alArrastrar));
        suscripciones.add(Eventos.escuchar(InventoryCreativeEvent.class, Prioridad.MAS_ALTA, false, this::alCreativo));
        suscripciones.add(Eventos.escuchar(InventoryMoveItemEvent.class, Prioridad.MAS_ALTA, false, this::alMoverItem));
        suscripciones.add(Eventos.escuchar(InventoryCloseEvent.class, Prioridad.NORMAL, false, this::alCerrar));
        suscripciones.add(Eventos.escuchar(PlayerQuitEvent.class, Prioridad.NORMAL, false, this::alSalir));
        suscripciones.add(Eventos.escuchar(PlayerDeathEvent.class, Prioridad.NORMAL, false, this::alMorir));
    }

    private void alClic(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof PortadorGui portador)) return;
        Gui gui = portador.gui();

        UUID uid = e.getWhoClicked().getUniqueId();
        long ahora = System.currentTimeMillis();
        Long ultimo = ultimoClic.get(uid);
        if (ultimo != null && ahora - ultimo < DEBOUNCE_MS) {
            e.setCancelled(true);
            return;
        }
        ultimoClic.put(uid, ahora);

        // Cancelamos siempre por defecto (anti-dupe).
        e.setCancelled(true);

        InventoryAction accion = e.getAction();
        ClickType tipo = e.getClick();
        boolean tieneEditables = !gui.slotsEditables().isEmpty();

        // Bloqueos explícitos de la sección "Anti-dupe" del plan.
        if (tipo == ClickType.SWAP_OFFHAND) return;                       // F key
        if (tipo == ClickType.DOUBLE_CLICK) return;                       // doble clic → COLLECT_TO_CURSOR
        if (accion == InventoryAction.COLLECT_TO_CURSOR) return;
        if (accion == InventoryAction.HOTBAR_SWAP) return;                // teclas 1-9 con GUI
        if (accion == InventoryAction.CLONE_STACK) return;                // creative middle-click
        // Shift-click de bottom→top: permitir sólo si la GUI tiene slots
        // editables (SellChestGui). En una GUI puramente de botones sigue
        // bloqueado como antes.
        if (accion == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            if (tieneEditables) e.setCancelled(false);
            return;
        }

        int rawSlot = e.getRawSlot();
        boolean enTop = rawSlot < gui.tamano();
        if (!enTop) {
            // GUI con slots editables: dejar que el jugador manipule su
            // propio inventario (tomar/soltar). Sin esto no puede llevar
            // ítems al chest de venta.
            if (tieneEditables && ACCIONES_EDITABLES_PERMITIDAS.contains(accion)) {
                e.setCancelled(false);
            }
            return;
        }

        int slot = e.getSlot();
        if (gui.esSlotEditable(slot)) {
            if (ACCIONES_EDITABLES_PERMITIDAS.contains(accion)) {
                e.setCancelled(false);
            }
            return;
        }

        Boton boton = gui.botones().get(slot);
        if (boton == null || !boton.tieneManejador()) return;

        Clic clic = new Clic(gui, e);
        try {
            boton.manejador().accept(clic);
        } catch (Throwable t) {
            nucleo.registro().error("Botón " + gui.getClass().getSimpleName()
                    + "#" + slot + " falló", t);
        }
    }

    private void alArrastrar(InventoryDragEvent e) {
        if (!(e.getInventory().getHolder() instanceof PortadorGui portador)) return;
        Gui gui = portador.gui();
        // Cancelar si algún slot del top no es editable.
        for (int rawSlot : e.getRawSlots()) {
            if (rawSlot < gui.tamano() && !gui.esSlotEditable(rawSlot)) {
                e.setCancelled(true);
                return;
            }
        }
    }

    private void alCreativo(InventoryCreativeEvent e) {
        if (e.getInventory().getHolder() instanceof PortadorGui) {
            e.setCancelled(true);
        }
    }

    private void alMoverItem(InventoryMoveItemEvent e) {
        if (e.getDestination().getHolder() instanceof PortadorGui
                || e.getSource().getHolder() instanceof PortadorGui) {
            e.setCancelled(true);
        }
    }

    private void alCerrar(InventoryCloseEvent e) {
        if (!(e.getInventory().getHolder() instanceof PortadorGui portador)) return;
        Gui gui = portador.gui();
        // Devolver items en slots editables al jugador (o dropearlos si el inventario está lleno).
        if (!gui.slotsEditables().isEmpty()) {
            Player p = (Player) e.getPlayer();
            for (int slot : gui.slotsEditables()) {
                ItemStack it = gui.inventario().getItem(slot);
                if (it == null || it.getType().isAir()) continue;
                var restantes = p.getInventory().addItem(it);
                for (ItemStack sobrante : restantes.values()) {
                    p.getWorld().dropItemNaturally(p.getLocation(), sobrante);
                }
                gui.inventario().setItem(slot, null);
            }
        }
        TareaCancelable t = gui.tareaRefresco();
        if (t != null) t.cancelar();
        // Sólo removemos del map si el GUI que se cerró es exactamente el
        // registrado actualmente. Cuando openInventory reemplaza una GUI
        // por otra en el mismo tick, este evento se dispara para la GUI
        // ANTERIOR después de que la nueva ya entró al map; comparar por
        // identidad evita descartar la nueva por accidente.
        UUID uid = e.getPlayer().getUniqueId();
        abiertas.remove(uid, gui);
    }

    private void alSalir(PlayerQuitEvent e) {
        Gui g = abiertas.remove(e.getPlayer().getUniqueId());
        if (g != null) {
            TareaCancelable t = g.tareaRefresco();
            if (t != null) t.cancelar();
        }
    }

    private void alMorir(PlayerDeathEvent e) {
        Gui g = abiertas.get(e.getEntity().getUniqueId());
        if (g != null) e.getEntity().closeInventory();
    }
}
