package studio.kuraky.kSCore.comandos;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca un método de la clase de comando que produce sugerencias
 * personalizadas para un argumento concreto. Debe devolver una
 * {@code Collection<String>} o {@code List<String>} y aceptar
 * {@code (Contexto ctx, String parcial)}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Completar {

    /** Ruta del subcomando ({@code ""} para el principal). */
    String sub() default "";

    /** Nombre del argumento a completar. */
    String arg();
}
