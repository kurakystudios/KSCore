package studio.kuraky.kSCore.comandos;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca el método a ejecutar cuando el usuario escribe el comando sin
 * subcomandos. Debe existir como máximo uno por clase.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Principal {
}
