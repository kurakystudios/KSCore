package studio.kuraky.kSCore.comandos;

/**
 * Excepción lanzada por un {@link Convertidor} cuando el texto no puede
 * transformarse al tipo esperado. El mensaje se muestra al emisor tal
 * cual (ya viene formateado con la sintaxis de {@code Mensajes}).
 */
public final class ErrorArgumento extends RuntimeException {

    public ErrorArgumento(String mensaje) {
        super(mensaje);
    }

    public ErrorArgumento(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
