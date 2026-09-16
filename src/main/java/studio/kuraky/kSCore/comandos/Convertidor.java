package studio.kuraky.kSCore.comandos;

import java.util.List;

/**
 * Convierte una cadena escrita por el jugador al tipo Java esperado por
 * un método de comando. Puede ofrecer sugerencias de autocompletado
 * para su tipo.
 */
public interface Convertidor<T> {

    /**
     * Transforma la entrada. Debe lanzar {@link ErrorArgumento} con un
     * mensaje ya formateado (usa las claves de {@code mensajes.yml}) si
     * la conversión falla.
     */
    T convertir(Contexto ctx, String texto) throws ErrorArgumento;

    /** Sugerencias mostradas al escribir. Por defecto, ninguna. */
    default List<String> sugerencias(Contexto ctx, String parcial) {
        return List.of();
    }

    /** {@code true} si consume todos los argumentos restantes ({@code String...}). */
    default boolean absorbeResto() {
        return false;
    }
}
