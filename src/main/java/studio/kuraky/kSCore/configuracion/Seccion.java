package studio.kuraky.kSCore.configuracion;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca un campo como una sección anidada. El valor opcional permite
 * cambiar el nombre de la sección; si se deja vacío se usa el nombre
 * del campo.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Seccion {

    String value() default "";
}
