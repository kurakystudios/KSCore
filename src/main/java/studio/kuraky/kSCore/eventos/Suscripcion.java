package studio.kuraky.kSCore.eventos;

/**
 * Handle devuelto por {@code Eventos.escuchar(...)} para desregistrar
 * el listener programático.
 */
public interface Suscripcion {

    void cancelar();

    boolean activa();
}
