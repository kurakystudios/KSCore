package studio.kuraky.kSCore.eventos;

import org.bukkit.event.EventPriority;

/**
 * Prioridad de ejecución para un manejador de eventos. Se mapea 1-a-1
 * con {@link EventPriority}, pero con los nombres en español.
 */
public enum Prioridad {

    MAS_BAJA(EventPriority.LOWEST),
    BAJA(EventPriority.LOW),
    NORMAL(EventPriority.NORMAL),
    ALTA(EventPriority.HIGH),
    MAS_ALTA(EventPriority.HIGHEST),
    MONITOR(EventPriority.MONITOR);

    private final EventPriority bukkit;

    Prioridad(EventPriority bukkit) {
        this.bukkit = bukkit;
    }

    public EventPriority bukkit() {
        return bukkit;
    }
}
