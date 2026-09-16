package studio.kuraky.kSCore.efectos;

import java.util.UUID;

/**
 * Estado de un efecto activo sobre un jugador. Los efectos vanilla se
 * exponen a través de esta misma clase para que
 * {@code Efectos.activos(jugador)} devuelva una lista unificada.
 */
public final class EfectoActivo {

    private final UUID jugador;
    private final String id;
    private final int nivel;
    private volatile long ticksRestantes;
    private final int intervaloTicks;
    private final boolean personalizado;
    private long tickInterno;

    public EfectoActivo(UUID jugador, String id, int nivel, long ticksRestantes,
                        int intervaloTicks, boolean personalizado) {
        this.jugador = jugador;
        this.id = id;
        this.nivel = nivel;
        this.ticksRestantes = ticksRestantes;
        this.intervaloTicks = Math.max(1, intervaloTicks);
        this.personalizado = personalizado;
    }

    public UUID jugador() { return jugador; }
    public String id() { return id; }
    public int nivel() { return nivel; }
    public long ticksRestantes() { return ticksRestantes; }
    public int intervaloTicks() { return intervaloTicks; }
    public boolean esPersonalizado() { return personalizado; }

    public boolean tocaTick() {
        return (tickInterno % intervaloTicks) == 0;
    }

    public void tick() {
        tickInterno++;
        ticksRestantes--;
    }

    public void sumar(long ticks) {
        ticksRestantes += ticks;
    }

    public void asignarRestantes(long ticks) {
        ticksRestantes = ticks;
    }

    public boolean expirado() {
        return ticksRestantes <= 0;
    }
}
