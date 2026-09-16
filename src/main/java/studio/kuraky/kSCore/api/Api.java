package studio.kuraky.kSCore.api;

import studio.kuraky.kSCore.nucleo.Depurador;
import studio.kuraky.kSCore.nucleo.Nucleo;
import studio.kuraky.kSCore.nucleo.Registro;
import studio.kuraky.kSCore.nucleo.Tareas;

/**
 * Punto de entrada al núcleo. Sirve para código externo que necesite
 * acceso directo a los servicios transversales (logger, tareas, modo
 * debug). Los subsistemas específicos se usan directamente por sus
 * fachadas estáticas: {@code Mensajes}, {@code Comandos},
 * {@code Eventos}, {@code Archivos}, {@code Datos}, {@code Items},
 * {@code Efectos}, {@code Guis} y {@code Chat}.
 *
 * <p>Todos los métodos devuelven {@code null} si el núcleo aún no ha
 * arrancado o ya se detuvo ({@code onDisable}). Comprueba antes de
 * usar los valores.
 */
public final class Api {

    private Api() {}

    /** Instancia viva del núcleo, o {@code null} si el plugin no está iniciado. */
    public static Nucleo nucleo() {
        return Nucleo.actual();
    }

    /** Logger con prefijo del plugin. */
    public static Registro registro() {
        Nucleo n = nucleo();
        return n == null ? null : n.registro();
    }

    /** Modo debug: consulta el flag y cronometraje. */
    public static Depurador depurador() {
        Nucleo n = nucleo();
        return n == null ? null : n.depurador();
    }

    /** Fachada de tareas: {@code sync}, {@code async}, Folia-safe. */
    public static Tareas tareas() {
        Nucleo n = nucleo();
        return n == null ? null : n.tareas();
    }
}
