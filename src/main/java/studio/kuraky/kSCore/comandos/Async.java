package studio.kuraky.kSCore.comandos;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca un método para ejecutarse en un hilo virtual en lugar de en el
 * hilo principal. Adventure es thread-safe, así que {@code
 * Contexto.enviar} sigue siendo seguro; cualquier acceso a la API de
 * Bukkit desde el método es responsabilidad del autor.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Async {
}
