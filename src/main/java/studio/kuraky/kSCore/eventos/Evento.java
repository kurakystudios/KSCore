package studio.kuraky.kSCore.eventos;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca un método como manejador de eventos. El primer y único
 * parámetro debe ser una subclase de {@code org.bukkit.event.Event}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Evento {

    Prioridad prioridad() default Prioridad.NORMAL;

    /** Si es {@code true}, no se invoca cuando el evento ya está cancelado. */
    boolean ignorarCancelados() default false;
}
