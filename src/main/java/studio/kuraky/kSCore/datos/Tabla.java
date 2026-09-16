package studio.kuraky.kSCore.datos;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca una clase como entidad persistente. El nombre de la tabla, si
 * no se especifica, se deriva del nombre simple de la clase en snake_case.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Tabla {

    String nombre() default "";

    /** Minutos que un objeto vive en caché desde su última escritura. */
    int cacheMinutos() default 15;

    /** Tamaño máximo de la caché en entradas. */
    int cacheMaximo() default 2_000;
}
