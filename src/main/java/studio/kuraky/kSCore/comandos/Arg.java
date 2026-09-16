package studio.kuraky.kSCore.comandos;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca un parámetro de un método de comando como argumento posicional.
 * El {@link #value()} controla el nombre mostrado en Brigadier; si se
 * deja vacío se usa el nombre del parámetro (requiere el flag
 * {@code -parameters} en la compilación).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Arg {

    String value() default "";

    /** Si es {@code true}, el argumento puede omitirse y se usará {@link #defecto()}. */
    boolean opcional() default false;

    /** Valor por defecto (como texto) cuando el argumento se omite. */
    String defecto() default "";
}
