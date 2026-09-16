package studio.kuraky.kSCore.datos;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Excluye un campo del mapeo persistente. Los campos {@code transient}
 * o {@code static} también se ignoran automáticamente.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Ignorar {
}
