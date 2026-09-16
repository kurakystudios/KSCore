package studio.kuraky.kSCore.chat;

/**
 * Definición inmutable de un canal de chat. Los canales se registran
 * programáticamente vía {@link Chat#registrarCanal(String, String,
 * String, String)} o desde {@code chat.yml}.
 */
public final class Canal {

    private final String id;
    private final String prefijo;
    private final String permiso;
    private final String color;

    public Canal(String id, String prefijo, String permiso, String color) {
        this.id = id;
        this.prefijo = prefijo == null ? "" : prefijo;
        this.permiso = permiso == null ? "" : permiso;
        this.color = color == null || color.isEmpty() ? "&f" : color;
    }

    public String id() { return id; }
    public String prefijo() { return prefijo; }
    public String permiso() { return permiso; }
    public String color() { return color; }

    /** {@code true} si el emisor puede escribir/ver este canal. */
    public boolean permitido(org.bukkit.permissions.Permissible p) {
        return permiso.isEmpty() || p.hasPermission(permiso);
    }
}
