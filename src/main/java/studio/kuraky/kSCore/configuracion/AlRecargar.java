package studio.kuraky.kSCore.configuracion;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca un método sin argumentos que se invoca tras cargar o recargar
 * el archivo. Se ejecuta en el hilo desde el que se disparó la recarga
 * (generalmente hilo virtual).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AlRecargar {
}
