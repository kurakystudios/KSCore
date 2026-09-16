package studio.kuraky.kSCore.gui;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca una clase que extiende {@link Gui} como menú registrable por
 * id. Sirve para abrir el menú por identificador con
 * {@code Guis.abrir(jugador, "id")}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Menu {

    String id();
}
