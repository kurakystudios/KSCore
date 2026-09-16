package studio.kuraky.kSCore.mensajes;

import studio.kuraky.kSCore.configuracion.Archivos;
import studio.kuraky.kSCore.nucleo.Modulo;
import studio.kuraky.kSCore.nucleo.Nucleo;

public final class ModuloMensajes implements Modulo {

    private Nucleo nucleo;
    private Analizador analizador;

    @Override
    public void iniciar(Nucleo nucleo) {
        this.nucleo = nucleo;
        this.analizador = new Analizador();
        ConfigMensajes cfg = Archivos.obtener(ConfigMensajes.class);
        Mensajes.inicializar(analizador, cfg.aplanado());
        nucleo.depurador().log("Mensajes", () -> "Cadenas cargadas: " + cfg.aplanado().size());
    }

    @Override
    public void detener() {
        Mensajes.desinicializar();
        if (analizador != null) analizador.invalidarCache();
        this.analizador = null;
        this.nucleo = null;
    }

    public Analizador analizador() {
        return analizador;
    }
}
