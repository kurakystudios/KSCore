package studio.kuraky.kSCore.comandos;

/**
 * Contrato opcional que una clase {@code @Comando} puede implementar para
 * decidir en tiempo de arranque si debe registrarse. Se consulta después
 * de instanciar la clase en {@link ModuloComandos} y antes de construir
 * el árbol Brigadier.
 *
 * <p>Uso típico: comandos gobernados por un flag de configuración cargado
 * en el módulo de configuración (que arranca antes que el de comandos).
 * Si {@link #debeRegistrarse()} devuelve {@code false}, el comando queda
 * fuera del registro y por tanto no aparece en tab-completion ni se puede
 * invocar. Como las registraciones Brigadier se aplican una sola vez en
 * el evento {@code LifecycleEvents.COMMANDS}, cambiar el flag en caliente
 * requiere reiniciar el servidor para que el comando aparezca/desaparezca.
 */
public interface Condicional {

    boolean debeRegistrarse();
}
