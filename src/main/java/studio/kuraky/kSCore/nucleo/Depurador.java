package studio.kuraky.kSCore.nucleo;

import java.util.function.Supplier;

public final class Depurador {

    private final Registro registro;
    private volatile boolean activo;

    public Depurador(Registro registro, boolean activo) {
        this.registro = registro;
        this.activo = activo;
    }

    public boolean activo() {
        return activo;
    }

    public void activar(boolean activo) {
        this.activo = activo;
    }

    public void log(String modulo, String mensaje) {
        if (!activo) return;
        registro.debug(modulo + " » " + mensaje);
    }

    public void log(String modulo, Supplier<String> mensaje) {
        if (!activo) return;
        registro.debug(modulo + " » " + mensaje.get());
    }

    public void cronometrar(String modulo, String etiqueta, Runnable accion) {
        if (!activo) {
            accion.run();
            return;
        }
        long inicio = System.nanoTime();
        try {
            accion.run();
        } finally {
            long ms = (System.nanoTime() - inicio) / 1_000_000L;
            registro.debug(modulo + " » " + etiqueta + " tardó " + ms + "ms");
        }
    }
}
