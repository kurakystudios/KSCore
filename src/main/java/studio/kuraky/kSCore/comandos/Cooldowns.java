package studio.kuraky.kSCore.comandos;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cooldown por método y por jugador. La consola nunca se ve afectada.
 */
public final class Cooldowns {

    private final Map<Method, Map<UUID, Long>> registros = new ConcurrentHashMap<>();

    /** Segundos restantes ({@code 0} si el jugador puede ejecutar ya). */
    public long tiempoRestante(Method metodo, UUID jugador, int segundos) {
        Map<UUID, Long> porJugador = registros.get(metodo);
        if (porJugador == null) return 0;
        Long marca = porJugador.get(jugador);
        if (marca == null) return 0;
        long transcurrido = (System.currentTimeMillis() - marca) / 1000L;
        return Math.max(0, segundos - transcurrido);
    }

    public void marcar(Method metodo, UUID jugador) {
        registros.computeIfAbsent(metodo, k -> new ConcurrentHashMap<>())
                .put(jugador, System.currentTimeMillis());
    }

    public void limpiar() {
        registros.clear();
    }
}
