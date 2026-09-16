package studio.kuraky.kSCore.chat;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Filtro anti-spam ligero: cooldown por jugador y bloqueo del mismo
 * mensaje enviado dos veces seguidas.
 */
public final class AntiSpam {

    private final Map<UUID, Long> ultimoEn = new ConcurrentHashMap<>();
    private final Map<UUID, String> ultimoTexto = new ConcurrentHashMap<>();

    /** Devuelve el motivo del bloqueo o {@code null} si el mensaje puede seguir. */
    public String evaluar(UUID jugador, String mensaje, int cooldownSegundos, boolean bloquearRepetidos) {
        long ahora = System.currentTimeMillis();
        if (cooldownSegundos > 0) {
            Long ultimo = ultimoEn.get(jugador);
            if (ultimo != null && (ahora - ultimo) < cooldownSegundos * 1000L) {
                long restante = cooldownSegundos - (ahora - ultimo) / 1000L;
                return "cooldown:" + Math.max(1, restante);
            }
        }
        if (bloquearRepetidos && mensaje != null && mensaje.equals(ultimoTexto.get(jugador))) {
            return "repetido";
        }
        ultimoEn.put(jugador, ahora);
        if (mensaje != null) ultimoTexto.put(jugador, mensaje);
        return null;
    }

    public void olvidar(UUID jugador) {
        ultimoEn.remove(jugador);
        ultimoTexto.remove(jugador);
    }

    public void limpiar() {
        ultimoEn.clear();
        ultimoTexto.clear();
    }
}
