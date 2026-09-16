package studio.kuraky.kSCore.comandos;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import studio.kuraky.kSCore.mensajes.Mensajes;

import java.util.Arrays;
import java.util.Map;

/**
 * Contexto de ejecución de un comando. Encapsula al emisor, los
 * argumentos crudos escritos por el jugador y utilidades de envío de
 * mensajes con formato.
 */
public final class Contexto {

    private final CommandSender emisor;
    private final Location ubicacion;
    private final String etiqueta;
    private final String[] args;

    public Contexto(CommandSender emisor, Location ubicacion, String etiqueta, String[] args) {
        this.emisor = emisor;
        this.ubicacion = ubicacion;
        this.etiqueta = etiqueta;
        this.args = args != null ? args : new String[0];
    }

    public CommandSender emisor() {
        return emisor;
    }

    public boolean esJugador() {
        return emisor instanceof Player;
    }

    public boolean esConsola() {
        return emisor instanceof ConsoleCommandSender;
    }

    /**
     * Devuelve el jugador emisor o {@code null} si el emisor no es
     * jugador. Los comandos con {@code soloJugador = true} nunca lo
     * verán como {@code null} porque el sistema aborta antes.
     */
    public Player jugador() {
        return emisor instanceof Player p ? p : null;
    }

    public Location ubicacion() {
        return ubicacion != null ? ubicacion.clone() : null;
    }

    public String etiqueta() {
        return etiqueta;
    }

    public boolean tienePermiso(String permiso) {
        return permiso == null || permiso.isEmpty() || emisor.hasPermission(permiso);
    }

    public String arg(int indice) {
        return indice >= 0 && indice < args.length ? args[indice] : null;
    }

    public String[] args() {
        return args.clone();
    }

    public int cantidad() {
        return args.length;
    }

    public void enviar(String texto) {
        Mensajes.enviar(emisor, texto);
    }

    public void enviar(String texto, Map<String, ?> variables) {
        Mensajes.enviar(emisor, texto, variables);
    }

    public void enviarDe(String clave) {
        Mensajes.enviarDe(emisor, clave);
    }

    public void enviarDe(String clave, Map<String, ?> variables) {
        Mensajes.enviarDe(emisor, clave, variables);
    }

    public void enviar(Component componente) {
        emisor.sendMessage(componente);
    }

    @Override
    public String toString() {
        return "Contexto{" + emisor.getName() + "," + etiqueta + "," + Arrays.toString(args) + "}";
    }
}
