package studio.kuraky.kSCore.configuracion;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Sobrescribe el nombre de la clave usada para serializar/deserializar
 * el campo, permitiendo usar guiones u otros nombres no válidos en Java.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Clave {

    String value();
}
