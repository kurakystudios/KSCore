package studio.kuraky.kSCore.items;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.Registry;

import java.util.Base64;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fachada estática del sistema de items. Antes de arrancar
 * {@link ModuloItems} lanza {@link IllegalStateException} en cualquier
 * llamada.
 */
public final class Items {

    /** Clave PDC bajo la que se guarda el identificador de {@link Item @Item}. */
    public static final String CLAVE_ID = "id";

    private static volatile ModuloItems modulo;
    private static final AtomicInteger refs = new AtomicInteger();

    private Items() {}

    static void inicializar(ModuloItems activo) {
        Items.modulo = activo;
        refs.incrementAndGet();
    }

    static void desinicializar() {
        if (refs.decrementAndGet() <= 0) {
            refs.set(0);
            Items.modulo = null;
        }
    }

    public static boolean disponible() {
        return modulo != null;
    }

    private static ModuloItems requerir() {
        ModuloItems m = modulo;
        if (m == null) throw new IllegalStateException("Items: ModuloItems aún no está iniciado.");
        return m;
    }

    // ---------- construcción ----------

    public static ConstructorItem crear(Material material) {
        return new ConstructorItem(requerir().plugin(), new ItemStack(material));
    }

    /** Devuelve un builder que modifica el item in-place. */
    public static ConstructorItem editar(ItemStack item) {
        return new ConstructorItem(requerir().plugin(), item);
    }

    // ---------- PDC ----------

    public static Optional<String> dato(ItemStack item, String clave) {
        Plugin plugin = requerir().plugin();
        if (item == null) return Optional.empty();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return Optional.empty();
        NamespacedKey key = new NamespacedKey(plugin, clave);
        String valor = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        return Optional.ofNullable(valor);
    }

    public static boolean es(ItemStack item, String id) {
        return dato(item, CLAVE_ID).map(id::equals).orElse(false);
    }

    // ---------- registro ----------

    /** Nombres de todos los items registrados con {@link Item @Item}. */
    public static Collection<String> registrados() {
        return requerir().registrados();
    }

    /** Devuelve una copia del ItemStack cacheado, o {@code null} si el id no existe. */
    public static ItemStack obtener(String id) {
        ItemPersonalizado p = requerir().buscar(id);
        if (p == null) return null;
        ItemStack plantilla = requerir().plantilla(id);
        return plantilla == null ? null : plantilla.clone();
    }

    /** Entrega {@code cantidad} unidades del item registrado al jugador. */
    public static void dar(Player jugador, String id, int cantidad) {
        ItemStack item = obtener(id);
        if (item == null) return;
        item.setAmount(Math.max(1, cantidad));
        jugador.getInventory().addItem(item);
    }

    /** Búsqueda interna usada por el enrutador de eventos. */
    static ItemPersonalizado buscarPorId(String id) {
        ModuloItems m = modulo;
        return m == null ? null : m.buscar(id);
    }

    // ---------- serialización ----------

    public static String aBase64(ItemStack item) {
        return Base64.getEncoder().encodeToString(item.serializeAsBytes());
    }

    public static ItemStack desdeBase64(String base64) {
        return ItemStack.deserializeBytes(Base64.getDecoder().decode(base64));
    }

    // ---------- YAML ----------

    public static ItemStack desdeSeccion(ConfigurationSection seccion) {
        String materialTexto = seccion.getString("material");
        if (materialTexto == null || materialTexto.isBlank()) {
            throw new IllegalArgumentException("Falta 'material' en la sección.");
        }
        Material material = Material.matchMaterial(materialTexto);
        if (material == null) throw new IllegalArgumentException("Material desconocido: " + materialTexto);

        ConstructorItem c = crear(material);
        String nombre = seccion.getString("nombre");
        if (nombre != null && !nombre.isEmpty()) c.nombre(nombre);
        if (seccion.isList("lore")) c.lore(seccion.getStringList("lore"));
        String cabeza = seccion.getString("cabeza");
        if (cabeza != null && !cabeza.isBlank()) c.cabeza(cabeza);
        String modelo = seccion.getString("modelo");
        if (modelo != null && !modelo.isBlank()) c.modelo(modelo);
        if (seccion.getBoolean("irrompible", false)) c.irrompible();
        if (seccion.getBoolean("ocultar-todo", false)) c.ocultarTodo();
        if (seccion.getBoolean("brillo", false)) c.brillo();
        c.cantidad(seccion.getInt("cantidad", 1));

        ConfigurationSection encantamientos = seccion.getConfigurationSection("encantamientos");
        if (encantamientos != null) {
            for (String clave : encantamientos.getKeys(false)) {
                NamespacedKey enchKey = NamespacedKey.minecraft(clave.toLowerCase(java.util.Locale.ROOT));
                Enchantment ench = Registry.ENCHANTMENT.get(enchKey);
                if (ench != null) {
                    c.encantar(ench, Math.max(1, encantamientos.getInt(clave)));
                }
            }
        }

        ConfigurationSection dato = seccion.getConfigurationSection("dato");
        if (dato != null) {
            for (String clave : dato.getKeys(false)) {
                c.dato(clave, String.valueOf(dato.get(clave)));
            }
        }

        return c.construir();
    }
}
