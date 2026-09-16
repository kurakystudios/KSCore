package studio.kuraky.kSCore.chat;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;
import studio.kuraky.kSCore.configuracion.Archivos;
import studio.kuraky.kSCore.eventos.Eventos;
import studio.kuraky.kSCore.eventos.Prioridad;
import studio.kuraky.kSCore.eventos.Suscripcion;
import studio.kuraky.kSCore.mensajes.Mensajes;
import studio.kuraky.kSCore.nucleo.Modulo;
import studio.kuraky.kSCore.nucleo.Nucleo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ModuloChat implements Modulo {

    private static final Pattern PATRON_MENCION = Pattern.compile("@(\\w+)");
    private static final String PERMISO_COLOR = "kscore.chat.color";
    private static final String PREFIJO_PERMISO_FORMATO = "kscore.chat.formato.";

    private final Map<String, Canal> canales = new ConcurrentHashMap<>();
    private final List<Suscripcion> suscripciones = new ArrayList<>();
    private final AntiSpam antiSpam = new AntiSpam();

    private Nucleo nucleo;
    private ConfigChat cfg;
    private boolean primario;

    @Override
    public void iniciar(Nucleo nucleo) {
        this.nucleo = nucleo;
        // Solo el primer ModuloChat registra AsyncChatEvent/PlayerQuitEvent.
        // Los demás delegan en la fachada Chat.
        if (Chat.disponible()) {
            this.primario = false;
            return;
        }
        this.primario = true;
        this.cfg = Archivos.obtener(ConfigChat.class);
        cargarCanalesDesdeCfg();
        Chat.inicializar(this);
        suscripciones.add(Eventos.escuchar(AsyncChatEvent.class, Prioridad.NORMAL, false, this::alChat));
        suscripciones.add(Eventos.escuchar(PlayerQuitEvent.class, Prioridad.NORMAL, false,
                e -> antiSpam.olvidar(e.getPlayer().getUniqueId())));
    }

    @Override
    public void detener() {
        if (!primario) {
            this.nucleo = null;
            return;
        }
        for (Suscripcion s : suscripciones) { try { s.cancelar(); } catch (Throwable ignored) {} }
        suscripciones.clear();
        antiSpam.limpiar();
        canales.clear();
        Chat.desinicializar();
        PuentePlaceholderAPI.resetear();
        this.nucleo = null;
        this.cfg = null;
    }

    // ---------- API pública para Chat facade ----------

    void registrarCanal(Canal canal) {
        canales.put(canal.id(), canal);
    }

    Canal canal(String id) {
        return canales.get(id);
    }

    Collection<Canal> canales() {
        return List.copyOf(canales.values());
    }

    void difundirCanal(String id, Player emisor, String mensaje) {
        Canal canal = canales.get(id);
        if (canal == null) throw new IllegalArgumentException("Canal desconocido: " + id);
        if (emisor != null && !canal.permitido(emisor)) {
            throw new IllegalStateException("El emisor no tiene permiso para el canal " + id);
        }
        String texto = (canal.prefijo().isEmpty() ? "" : canal.prefijo() + " ")
                + "&r&7" + (emisor == null ? "*" : emisor.getName()) + "&8: "
                + canal.color() + mensaje;
        Component componente = Mensajes.parsear(texto);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (canal.permitido(p)) p.sendMessage(componente);
        }
    }

    // ---------- ciclo ----------

    private void cargarCanalesDesdeCfg() {
        canales.clear();
        for (Map.Entry<String, ConfigChat.DefinicionCanal> e : cfg.canales.entrySet()) {
            ConfigChat.DefinicionCanal def = e.getValue();
            canales.put(e.getKey(), new Canal(e.getKey(), def.prefijo, def.permiso, def.color));
        }
    }

    // ---------- AsyncChatEvent ----------

    private void alChat(AsyncChatEvent e) {
        Player emisor = e.getPlayer();
        String mensajePlano = PlainTextComponentSerializer.plainText().serialize(e.message());

        String razon = antiSpam.evaluar(emisor.getUniqueId(), mensajePlano,
                cfg.cooldownSegundos, cfg.bloquearRepetidos);
        if (razon != null) {
            e.setCancelled(true);
            avisarBloqueo(emisor, razon);
            return;
        }

        Set<Player> mencionados = detectarMenciones(mensajePlano, emisor);
        if (cfg.menciones) notificarMenciones(mencionados);

        e.renderer((source, sourceDisplayName, message, viewer) ->
                construirMensaje(source, message, mencionados));
    }

    private Component construirMensaje(Player source, Component message, Set<Player> mencionados) {
        String formato = elegirFormato(source);
        formato = PuentePlaceholderAPI.expandir(source, formato);
        String plantilla = formato
                .replace("{nombre}", source.getName())
                .replace("{display}", PlainTextComponentSerializer.plainText().serialize(source.displayName()))
                .replace("{mundo}", source.getWorld() != null ? source.getWorld().getName() : "");
        Component base = Mensajes.parsear(plantilla);

        Component mensajeFinal = source.hasPermission(PERMISO_COLOR)
                ? Mensajes.parsear(PlainTextComponentSerializer.plainText().serialize(message))
                : message;
        for (Player m : mencionados) {
            String literal = "@" + m.getName();
            Component reemplazo = Mensajes.parsear(cfg.colorMencion + literal + "&r");
            mensajeFinal = mensajeFinal.replaceText(TextReplacementConfig.builder()
                    .matchLiteral(literal).replacement(reemplazo).build());
        }

        return base.replaceText(TextReplacementConfig.builder()
                .matchLiteral("{mensaje}").replacement(mensajeFinal).build());
    }

    private String elegirFormato(Player source) {
        // El orden de los formatos en el YAML manda; los primeros ganan.
        for (Map.Entry<String, String> e : cfg.formatosPorPermiso.entrySet()) {
            if (source.hasPermission(PREFIJO_PERMISO_FORMATO + e.getKey())) return e.getValue();
        }
        return cfg.formatoPorDefecto;
    }

    private Set<Player> detectarMenciones(String texto, Player emisor) {
        Set<Player> salida = new HashSet<>();
        Matcher m = PATRON_MENCION.matcher(texto);
        while (m.find()) {
            String nombre = m.group(1);
            Player p = Bukkit.getPlayerExact(nombre);
            if (p == null) continue;
            if (p.getUniqueId().equals(emisor.getUniqueId())) continue;
            salida.add(p);
        }
        return salida;
    }

    private void notificarMenciones(Set<Player> mencionados) {
        String nombreSonido = cfg.sonidoMencion;
        if (nombreSonido == null || nombreSonido.isBlank()) return;
        Sound sonido = resolverSonido(nombreSonido);
        if (sonido == null) return;
        for (Player m : mencionados) {
            try { m.playSound(m.getLocation(), sonido, 1f, 1f); }
            catch (Throwable ignored) {}
        }
    }

    private static Sound resolverSonido(String nombre) {
        String base = nombre.toLowerCase(Locale.ROOT).replace('_', '.');
        if (base.startsWith("minecraft.")) base = base.substring(10);
        return Registry.SOUNDS.get(NamespacedKey.minecraft(base));
    }

    private void avisarBloqueo(Player p, String razon) {
        if (razon.startsWith("cooldown:")) {
            String restante = razon.substring("cooldown:".length());
            Mensajes.enviar(p, "&eEspera &f" + restante + "s&e antes de volver a escribir.");
        } else if (razon.equals("repetido")) {
            Mensajes.enviar(p, "&eNo repitas el mismo mensaje inmediatamente.");
        }
    }
}
