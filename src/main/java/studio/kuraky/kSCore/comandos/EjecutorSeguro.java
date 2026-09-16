package studio.kuraky.kSCore.comandos;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import studio.kuraky.kSCore.nucleo.Depurador;
import studio.kuraky.kSCore.nucleo.Registro;
import studio.kuraky.kSCore.nucleo.Tareas;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 * Envoltorio que aplica permisos, cooldowns, dispatch a hilo virtual y
 * captura cualquier {@link Throwable} lanzado por el método. Un fallo
 * en un comando no afecta al resto: se registra en consola y se avisa
 * al emisor con un mensaje amigable.
 */
public final class EjecutorSeguro {

    private static final String PERMISO_DEBUG = "core.debug";

    private final Tareas tareas;
    private final Registro registro;
    private final Depurador depurador;
    private final Cooldowns cooldowns;

    public EjecutorSeguro(Tareas tareas, Registro registro, Depurador depurador, Cooldowns cooldowns) {
        this.tareas = tareas;
        this.registro = registro;
        this.depurador = depurador;
        this.cooldowns = cooldowns;
    }

    public void ejecutar(DescriptorMetodo m, Contexto ctx, Object[] valores) {
        if (!m.permiso().isEmpty() && !ctx.tienePermiso(m.permiso())) {
            ctx.enviarDe("comun.sin-permiso");
            return;
        }
        if (m.soloJugador() && !ctx.esJugador()) {
            ctx.enviarDe("comun.solo-jugadores");
            return;
        }
        if (m.segundosCooldown() > 0 && ctx.esJugador()) {
            Player jugador = ctx.jugador();
            long restante = cooldowns.tiempoRestante(m.metodo(), jugador.getUniqueId(), m.segundosCooldown());
            if (restante > 0) {
                ctx.enviarDe(m.claveCooldown(), Map.of("restante", String.valueOf(restante)));
                return;
            }
            cooldowns.marcar(m.metodo(), jugador.getUniqueId());
        }

        if (m.async()) {
            tareas.async(() -> invocar(m, ctx, valores));
        } else {
            invocar(m, ctx, valores);
        }
    }

    private void invocar(DescriptorMetodo m, Contexto ctx, Object[] valores) {
        long inicio = System.nanoTime();
        try {
            m.metodo().invoke(m.instancia(), valores);
        } catch (InvocationTargetException e) {
            Throwable causa = e.getCause();
            if (causa instanceof ErrorArgumento err) {
                if (err.getMessage() != null) ctx.enviar(err.getMessage());
            } else {
                errorInterno(m, ctx, causa);
            }
        } catch (Throwable t) {
            errorInterno(m, ctx, t);
        } finally {
            if (depurador.activo()) {
                long ms = (System.nanoTime() - inicio) / 1_000_000L;
                depurador.log("Comandos", () -> lugar(m) + " tardó " + ms + "ms");
            }
        }
    }

    private void errorInterno(DescriptorMetodo m, Contexto ctx, Throwable t) {
        ctx.enviarDe("comun.error-interno");
        String detalle = "Comando /" + ctx.etiqueta() + " " + m.rutaLegible() + " (" + lugar(m) + ") falló";
        registro.error(detalle, t);

        // A los jugadores con el permiso de debug, mostramos la excepción concreta.
        String resumen = "&c[Core] " + detalle + ": &f" + t.getClass().getSimpleName()
                + (t.getMessage() != null ? ": " + t.getMessage() : "");
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission(PERMISO_DEBUG)) {
                studio.kuraky.kSCore.mensajes.Mensajes.enviar(p, resumen);
            }
        }
    }

    private static String lugar(DescriptorMetodo m) {
        return m.metodo().getDeclaringClass().getSimpleName() + "#" + m.metodo().getName();
    }
}
