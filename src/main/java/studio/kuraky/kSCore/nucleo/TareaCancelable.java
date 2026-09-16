package studio.kuraky.kSCore.nucleo;

import java.util.concurrent.atomic.AtomicBoolean;

public final class TareaCancelable {

    private final Runnable accionCancelar;
    private final AtomicBoolean cancelada = new AtomicBoolean(false);

    public TareaCancelable(Runnable accionCancelar) {
        this.accionCancelar = accionCancelar;
    }

    public void cancelar() {
        if (cancelada.compareAndSet(false, true)) {
            try {
                accionCancelar.run();
            } catch (Throwable ignorado) {
                // Silencioso: cancelar nunca debe romper el flujo.
            }
        }
    }

    public boolean estaCancelada() {
        return cancelada.get();
    }
}
