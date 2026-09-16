package studio.kuraky.kSCore.comandos;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca una clase como comando. La clase debe tener un constructor sin
 * argumentos; el sistema instanciará una copia durante el arranque y la
 * reutilizará en todos los despachos.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Comando {

    /** Nombre principal del comando. Se usa como literal raíz. */
    String nombre();

    /** Alias adicionales. Se registran como literales independientes. */
    String[] alias() default {};

    /** Permiso requerido para ver o ejecutar el comando raíz. Vacío = sin restricción. */
    String permiso() default "";

    /** Descripción mostrada en la ayuda del servidor. */
    String descripcion() default "";

    /** Si es {@code true}, todo el comando exige que el emisor sea jugador. */
    boolean soloJugador() default false;
}
