package studio.kuraky.kSCore.configuracion;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Añade comentarios encima de la clave al serializar. Sólo se aplica en
 * formatos que soportan comentarios (YAML, TOML).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Comentario {

    String[] value();
}
