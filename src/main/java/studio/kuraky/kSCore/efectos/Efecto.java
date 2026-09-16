package studio.kuraky.kSCore.efectos;

import org.bukkit.Material;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declara un efecto personalizado. La clase debe extender
 * {@link EfectoPersonalizado} y tener un constructor sin argumentos.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Efecto {

    /** Identificador único del efecto. */
    String id();

    /** Nombre mostrado (soporta {@code &} y hex). */
    String nombre() default "";

    /** Material del icono (para action bar / boss bar). */
    Material icono() default Material.POTION;

    /** Si es {@code true}, se guarda en BD y se restaura al reconectar. */
    boolean persistente() default false;

    /** Estrategia cuando ya existe un efecto del mismo id en el jugador. */
    Apilado apilado() default Apilado.REEMPLAZAR;

    /** Cada cuántos ticks se invoca {@code cadaTick}. Mínimo 1. */
    int intervaloTicks() default 20;

    /** Dónde se muestra el efecto. */
    Mostrar mostrar() default Mostrar.NINGUNO;
}
