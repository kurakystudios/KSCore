package studio.kuraky.kSCore.datos;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca un campo público como columna persistente.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Columna {

    /** Nombre de la columna. Vacío = nombre del campo en snake_case. */
    String value() default "";

    /** Si es {@code true}, se crea un índice sobre esta columna. */
    boolean indice() default false;

    /** Si es {@code true}, la columna permite {@code NULL}. */
    boolean nullable() default true;

    /** Longitud sugerida para tipos textuales (VARCHAR). Ignorado por SQLite. */
    int longitud() default 255;
}
