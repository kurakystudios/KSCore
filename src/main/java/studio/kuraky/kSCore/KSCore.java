package studio.kuraky.kSCore;

import org.bukkit.plugin.java.JavaPlugin;
import studio.kuraky.kSCore.nucleo.Nucleo;

public final class KSCore extends JavaPlugin {

    private static KSCore instancia;
    private Nucleo nucleo;

    @Override
    public void onEnable() {
        instancia = this;
        this.nucleo = new Nucleo(this, getClass().getPackageName());
        this.nucleo.iniciar();
    }

    @Override
    public void onDisable() {
        if (this.nucleo != null) {
            this.nucleo.detener();
            this.nucleo = null;
        }
        instancia = null;
    }

    public static KSCore obtener() {
        return instancia;
    }

    public Nucleo nucleo() {
        return nucleo;
    }
}
