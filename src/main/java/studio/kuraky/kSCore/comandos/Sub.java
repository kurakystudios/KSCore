package studio.kuraky.kSCore.comandos;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca un método como subcomando. El {@link #nombre()} puede contener
 * espacios para producir subcomandos anidados (por ejemplo, {@code
 * "grupo crear"} genera {@code /rango grupo crear}).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Sub {

    String nombre();

    String permiso() default "";

    /** Texto de uso mostrado cuando la sintaxis es incorrecta. */
    String uso() default "";

    /** Si es {@code true}, exige que el emisor sea jugador. */
    boolean soloJugador() default false;
}
