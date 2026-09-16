package studio.kuraky.kSCore.datos;

import studio.kuraky.kSCore.nucleo.FuturoAsync;

import java.util.List;
import java.util.Optional;

/**
 * Capa de acceso a datos por tipo. Todas las operaciones son
 * asíncronas y devuelven {@link FuturoAsync}, que permite regresar al
 * hilo principal con {@code .enHiloPrincipal(...)}.
 */
public interface Repositorio<T, ID> {

    FuturoAsync<Optional<T>> obtener(ID id);

    FuturoAsync<Void> guardar(T entidad);

    FuturoAsync<Void> eliminar(ID id);

    FuturoAsync<List<T>> buscar(Filtro filtro, Orden orden, int limite);

    FuturoAsync<Long> contar();

    FuturoAsync<List<T>> todos();

    /** Invalida la entrada de caché para {@code id}. Útil tras una escritura externa. */
    void invalidarCache(ID id);

    /** Vacía la caché entera del repositorio. */
    void invalidarCacheTodo();

    /** Devuelve el descriptor de la tabla asociada (útil para tooling). */
    DescriptorTabla descriptor();
}
