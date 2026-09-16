package studio.kuraky.kSCore.configuracion;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca una clase como archivo de configuración cargable por el sistema
 * de configuración. La ruta es relativa a la carpeta de datos del plugin.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Archivo {

    String ruta();

    /** Si es {@code true}, el archivo se copia desde los recursos del jar si existe allí. */
    boolean copiarRecurso() default true;
}
