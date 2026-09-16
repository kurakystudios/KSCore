package studio.kuraky.kSCore.nucleo;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.plugin.java.JavaPlugin;

public final class Registro {

    private final ComponentLogger logger;
    private final String prefijo;

    public Registro(JavaPlugin plugin, String prefijo) {
        this(plugin.getComponentLogger(), prefijo);
    }

    public Registro(ComponentLogger logger, String prefijo) {
        this.logger = logger;
        this.prefijo = prefijo;
    }

    public void info(String mensaje) {
        logger.info(prefijar(mensaje, NamedTextColor.GRAY));
    }

    public void aviso(String mensaje) {
        logger.warn(prefijar(mensaje, NamedTextColor.YELLOW));
    }

    public void error(String mensaje) {
        logger.error(prefijar(mensaje, NamedTextColor.RED));
    }

    public void error(String mensaje, Throwable causa) {
        logger.error(prefijar(mensaje, NamedTextColor.RED), causa);
    }

    public void debug(String mensaje) {
        logger.info(prefijar("[DEBUG] " + mensaje, NamedTextColor.AQUA));
    }

    public String prefijo() {
        return prefijo;
    }

    private Component prefijar(String mensaje, NamedTextColor color) {
        return Component.text("[" + prefijo + "] ", NamedTextColor.LIGHT_PURPLE)
                .append(Component.text(mensaje, color));
    }
}
