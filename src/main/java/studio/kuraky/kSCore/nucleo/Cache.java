package studio.kuraky.kSCore.nucleo;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public final class Cache<K, V> {

    private final AsyncLoadingCache<K, V> caffeine;

    private Cache(AsyncLoadingCache<K, V> caffeine) {
        this.caffeine = caffeine;
    }

    public CompletableFuture<V> obtener(K clave) {
        return caffeine.get(clave);
    }

    public void poner(K clave, V valor) {
        caffeine.put(clave, CompletableFuture.completedFuture(valor));
    }

    public void invalidar(K clave) {
        caffeine.synchronous().invalidate(clave);
    }

    public void invalidarTodo() {
        caffeine.synchronous().invalidateAll();
    }

    public long tamano() {
        return caffeine.synchronous().estimatedSize();
    }

    public static <K, V> Constructor<K, V> crear() {
        return new Constructor<>();
    }

    public static final class Constructor<K, V> {

        private Duration expiraEn = Duration.ofMinutes(10);
        private long maximo = 1_000L;
        private Function<K, V> cargador = clave -> null;

        public Constructor<K, V> expiraEn(long cantidad, TimeUnit unidad) {
            this.expiraEn = Duration.ofNanos(unidad.toNanos(cantidad));
            return this;
        }

        public Constructor<K, V> expiraEn(Duration duracion) {
            this.expiraEn = duracion;
            return this;
        }

        public Constructor<K, V> maximo(long maximo) {
            if (maximo <= 0) throw new IllegalArgumentException("El tamaño máximo debe ser mayor que 0");
            this.maximo = maximo;
            return this;
        }

        public Constructor<K, V> cargador(Function<K, V> cargador) {
            this.cargador = cargador;
            return this;
        }

        public Cache<K, V> construir() {
            AsyncLoadingCache<K, V> ac = Caffeine.newBuilder()
                    .expireAfterWrite(expiraEn)
                    .maximumSize(maximo)
                    .buildAsync(cargador::apply);
            return new Cache<>(ac);
        }
    }
}
