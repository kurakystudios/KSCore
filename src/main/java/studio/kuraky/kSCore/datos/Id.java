package studio.kuraky.kSCore.datos;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca el campo que identifica de forma única la entidad. Debe haber
 * exactamente uno por clase {@link Tabla}. Se usa como clave primaria
 * en la base de datos y como clave en la caché.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Id {

    /** Nombre de la columna. Vacío = nombre del campo en snake_case. */
    String value() default "";
}
