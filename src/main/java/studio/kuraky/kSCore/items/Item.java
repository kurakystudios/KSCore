package studio.kuraky.kSCore.items;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca una clase como item personalizado registrable. La clase debe
 * tener un constructor sin argumentos y extender {@link ItemPersonalizado}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Item {

    /**
     * Identificador único del item. Se almacena como PDC {@code kscore:id}
     * en cada ItemStack construido, y es la clave por la que se enrutan
     * los eventos.
     */
    String id();
}
