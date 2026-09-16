package studio.kuraky.kSCore.items;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import studio.kuraky.kSCore.eventos.Eventos;
import studio.kuraky.kSCore.eventos.Prioridad;
import studio.kuraky.kSCore.eventos.Suscripcion;
import studio.kuraky.kSCore.nucleo.Modulo;
import studio.kuraky.kSCore.nucleo.Nucleo;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class ModuloItems implements Modulo {

    private final Map<String, ItemPersonalizado> registrados = new ConcurrentHashMap<>();
    private final Map<String, ItemStack> plantillas = new ConcurrentHashMap<>();
    private final List<Suscripcion> suscripciones = new ArrayList<>();

    private Nucleo nucleo;
    private boolean primario;

    @Override
    public void iniciar(Nucleo nucleo) {
        this.nucleo = nucleo;
        // Solo el primer ModuloItems registra listeners globales; los demás
        // delegan en la fachada para no duplicar eventos entre plugins.
        if (Items.disponible()) {
            this.primario = false;
            return;
        }
        this.primario = true;
        Items.inicializar(this);

        for (Class<?> clase : nucleo.escaner().conAnotacion(Item.class)) {
            if (Modifier.isAbstract(clase.getModifiers())) continue;
            if (!ItemPersonalizado.class.isAssignableFrom(clase)) {
                nucleo.registro().aviso(clase.getName() + " tiene @Item pero no extiende ItemPersonalizado.");
                continue;
            }
            Item meta = clase.getAnnotation(Item.class);
            String id = meta.id();
            try {
                Constructor<?> ctor = clase.getDeclaredConstructor();
                ctor.setAccessible(true);
                ItemPersonalizado personalizado = (ItemPersonalizado) ctor.newInstance();
                ItemStack plantilla = personalizado.construir();
                marcarConId(plantilla, id);
                registrados.put(id, personalizado);
                plantillas.put(id, plantilla);
                nucleo.depurador().log("Items", () -> "Registrado " + id + " (" + clase.getSimpleName() + ")");
            } catch (NoSuchMethodException e) {
                nucleo.registro().error(clase.getName() + " necesita un constructor sin argumentos.");
            } catch (Throwable t) {
                nucleo.registro().error("No se pudo registrar el item " + id, t);
            }
        }

        // Un solo listener enruta por PDC id.
        suscripciones.add(Eventos.escuchar(PlayerInteractEvent.class, Prioridad.NORMAL, true, this::enrutarInteractuar));
        suscripciones.add(Eventos.escuchar(EntityDamageByEntityEvent.class, Prioridad.NORMAL, true, this::enrutarGolpe));
    }

    @Override
    public void detener() {
        if (!primario) {
            nucleo = null;
            return;
        }
        for (Suscripcion s : suscripciones) {
            try { s.cancelar(); } catch (Throwable ignored) {}
        }
        suscripciones.clear();
        registrados.clear();
        plantillas.clear();
        Items.desinicializar();
        nucleo = null;
    }

    public Plugin plugin() {
        return nucleo.plugin();
    }

    public Collection<String> registrados() {
        return List.copyOf(registrados.keySet());
    }

    public ItemPersonalizado buscar(String id) {
        return registrados.get(id);
    }

    public ItemStack plantilla(String id) {
        return plantillas.get(id);
    }

    private void marcarConId(ItemStack item, String id) {
        NamespacedKey key = new NamespacedKey(nucleo.plugin(), Items.CLAVE_ID);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, id);
        item.setItemMeta(meta);
    }

    private void enrutarInteractuar(PlayerInteractEvent e) {
        ItemStack item = e.getItem();
        if (item == null) return;
        Optional<String> id = Items.dato(item, Items.CLAVE_ID);
        if (id.isEmpty()) return;
        ItemPersonalizado personalizado = registrados.get(id.get());
        if (personalizado == null) return;
        Action a = e.getAction();
        Player p = e.getPlayer();
        try {
            if (a == Action.RIGHT_CLICK_AIR || a == Action.RIGHT_CLICK_BLOCK) {
                personalizado.alClicDerecho(p, item, e);
            } else if (a == Action.LEFT_CLICK_AIR || a == Action.LEFT_CLICK_BLOCK) {
                personalizado.alClicIzquierdo(p, item, e);
            }
        } catch (Throwable t) {
            nucleo.registro().error("Item " + id.get() + " falló al interactuar", t);
        }
    }

    private void enrutarGolpe(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player jugador)) return;
        ItemStack item = jugador.getInventory().getItemInMainHand();
        if (item == null) return;
        Optional<String> id = Items.dato(item, Items.CLAVE_ID);
        if (id.isEmpty()) return;
        ItemPersonalizado personalizado = registrados.get(id.get());
        if (personalizado == null) return;
        try {
            personalizado.alGolpear(jugador, e.getEntity(), item, e);
        } catch (Throwable t) {
            nucleo.registro().error("Item " + id.get() + " falló al golpear", t);
        }
    }
}
