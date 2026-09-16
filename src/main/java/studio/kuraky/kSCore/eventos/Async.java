package studio.kuraky.kSCore.eventos;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declara explícitamente que un manejador de eventos está preparado
 * para ejecutarse fuera del hilo principal. Sólo se permite en eventos
 * cuyo tipo indique que se disparan de forma asíncrona (por ejemplo,
 * {@code AsyncChatEvent}); en eventos síncronos se rechaza en arranque
 * porque mutar el evento desde otro hilo no es seguro.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Async {
}
