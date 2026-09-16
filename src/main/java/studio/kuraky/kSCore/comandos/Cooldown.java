package studio.kuraky.kSCore.comandos;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Añade un tiempo de espera entre invocaciones consecutivas del método
 * por el mismo jugador. La consola nunca tiene cooldown.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Cooldown {

    int segundos();

    /**
     * Clave de {@code mensajes.yml} para el mensaje mostrado al usuario
     * mientras está en cooldown. Recibe la variable {@code %restante%}.
     */
    String mensaje() default "comun.cooldown";
}
