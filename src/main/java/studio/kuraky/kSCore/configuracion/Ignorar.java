package studio.kuraky.kSCore.configuracion;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Excluye el campo del mapeo de configuración. Los campos {@code static}
 * o {@code transient} también se ignoran automáticamente.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Ignorar {
}
