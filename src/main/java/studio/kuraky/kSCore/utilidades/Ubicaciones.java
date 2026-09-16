package studio.kuraky.kSCore.utilidades;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Locale;
import java.util.Optional;

public final class Ubicaciones {

    private static final String SEPARADOR = ";";

    private Ubicaciones() {}

    public static String serializar(Location loc) {
        if (loc == null || loc.getWorld() == null) return "";
        return String.join(SEPARADOR,
                loc.getWorld().getName(),
                String.format(Locale.ROOT, "%.4f", loc.getX()),
                String.format(Locale.ROOT, "%.4f", loc.getY()),
                String.format(Locale.ROOT, "%.4f", loc.getZ()),
                String.format(Locale.ROOT, "%.2f", loc.getYaw()),
                String.format(Locale.ROOT, "%.2f", loc.getPitch()));
    }

    public static Optional<Location> deserializar(String s) {
        if (s == null || s.isBlank()) return Optional.empty();
        String[] partes = s.split(SEPARADOR);
        if (partes.length < 4) return Optional.empty();
        World mundo = Bukkit.getWorld(partes[0]);
        if (mundo == null) return Optional.empty();
        try {
            double x = Double.parseDouble(partes[1]);
            double y = Double.parseDouble(partes[2]);
            double z = Double.parseDouble(partes[3]);
            float yaw = partes.length > 4 ? Float.parseFloat(partes[4]) : 0f;
            float pitch = partes.length > 5 ? Float.parseFloat(partes[5]) : 0f;
            return Optional.of(new Location(mundo, x, y, z, yaw, pitch));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
