package studio.kuraky.kSCore.eventos;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca una clase como contenedor de métodos {@link Evento}. El sistema
 * la instancia con un constructor sin argumentos durante el arranque y
 * registra cada método anotado en Bukkit.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Escuchador {
}
