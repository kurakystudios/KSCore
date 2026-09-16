package studio.kuraky.kSCore.items;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import studio.kuraky.kSCore.mensajes.Mensajes;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder fluído para construir o editar {@link ItemStack}. Prefiere
 * la Data Component API de Paper 26.2 sobre {@link ItemMeta} legacy en
 * los casos donde ya está expuesta (nombre, lore, modelo, brillo,
 * ocultación de tooltip, perfil de cabeza).
 */
public final class ConstructorItem {

    private static final DataComponentType[] HIDDENS_POR_DEFECTO = new DataComponentType[] {
            DataComponentTypes.ENCHANTMENTS,
            DataComponentTypes.ATTRIBUTE_MODIFIERS,
            DataComponentTypes.UNBREAKABLE,
            DataComponentTypes.CAN_BREAK,
            DataComponentTypes.CAN_PLACE_ON,
            DataComponentTypes.STORED_ENCHANTMENTS,
            DataComponentTypes.DYED_COLOR
    };

    private final Plugin plugin;
    private final ItemStack item;

    ConstructorItem(Plugin plugin, ItemStack item) {
        this.plugin = plugin;
        this.item = item;
    }

    public ConstructorItem nombre(String texto) {
        Component c = Mensajes.parsear(texto).decoration(TextDecoration.ITALIC, false);
        item.setData(DataComponentTypes.CUSTOM_NAME, c);
        return this;
    }

    public ConstructorItem lore(String... lineas) {
        return lore(List.of(lineas));
    }

    public ConstructorItem lore(List<String> lineas) {
        List<Component> componentes = new ArrayList<>(lineas.size());
        for (String l : lineas) {
            componentes.add(Mensajes.parsear(l).decoration(TextDecoration.ITALIC, false));
        }
        item.setData(DataComponentTypes.LORE, ItemLore.lore(componentes));
        return this;
    }

    public ConstructorItem encantar(Enchantment ench, int nivel) {
        ItemMeta meta = item.getItemMeta();
        meta.addEnchant(ench, nivel, true);
        item.setItemMeta(meta);
        return this;
    }

    /** Aplica el brillo (glint) sin necesidad de encantar. */
    public ConstructorItem brillo() {
        item.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        return this;
    }

    /**
     * Establece el modelo del item (component {@code item_model}). Si
     * la cadena no contiene ":", se asume el namespace del plugin.
     */
    public ConstructorItem modelo(String modelo) {
        Key key = modelo.contains(":")
                ? Key.key(modelo)
                : Key.key(plugin.getName().toLowerCase(java.util.Locale.ROOT), modelo);
        item.setData(DataComponentTypes.ITEM_MODEL, key);
        return this;
    }

    public ConstructorItem irrompible() {
        ItemMeta meta = item.getItemMeta();
        meta.setUnbreakable(true);
        item.setItemMeta(meta);
        return this;
    }

    /** Oculta los tags "auxiliares" (encantamientos, atributos, etc.). */
    public ConstructorItem ocultarTodo() {
        TooltipDisplay display = TooltipDisplay.tooltipDisplay()
                .addHiddenComponents(HIDDENS_POR_DEFECTO)
                .build();
        item.setData(DataComponentTypes.TOOLTIP_DISPLAY, display);
        return this;
    }

    /** Establece la cabeza usando URL, hash o base64 (autodetecta). */
    public ConstructorItem cabeza(String textura) {
        item.setData(DataComponentTypes.PROFILE, Cabezas.porTextura(textura));
        return this;
    }

    public ConstructorItem cabezaDe(OfflinePlayer jugador) {
        item.setData(DataComponentTypes.PROFILE, Cabezas.porJugador(jugador));
        return this;
    }

    public ConstructorItem cabezaBase64(String base64) {
        item.setData(DataComponentTypes.PROFILE, Cabezas.porBase64(base64));
        return this;
    }

    public ConstructorItem cabezaHash(String hash) {
        item.setData(DataComponentTypes.PROFILE, Cabezas.porHash(hash));
        return this;
    }

    /** Escribe un dato de texto en el PDC bajo el namespace del plugin. */
    public ConstructorItem dato(String clave, String valor) {
        NamespacedKey namespaced = new NamespacedKey(plugin, clave);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(namespaced, PersistentDataType.STRING, valor);
        item.setItemMeta(meta);
        return this;
    }

    public ConstructorItem cantidad(int cantidad) {
        item.setAmount(Math.max(1, cantidad));
        return this;
    }

    /** Aplica un {@link ResolvableProfile} ya resuelto (para casos avanzados). */
    public ConstructorItem perfilCrudo(ResolvableProfile perfil) {
        item.setData(DataComponentTypes.PROFILE, perfil);
        return this;
    }

    /** Termina el build y devuelve el {@link ItemStack} resultante. */
    public ItemStack construir() {
        return item;
    }

    /** Alias de {@link #construir()} cuando se usa a través de {@code Items.editar(...)}. */
    public ItemStack aplicar() {
        return item;
    }
}
