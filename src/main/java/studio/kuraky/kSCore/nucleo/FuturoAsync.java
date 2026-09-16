package studio.kuraky.kSCore.nucleo;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

public final class FuturoAsync<T> {

    private final CompletableFuture<T> delegado;
    private final Tareas tareas;

    public FuturoAsync(CompletableFuture<T> delegado, Tareas tareas) {
        this.delegado = delegado;
        this.tareas = tareas;
    }

    public FuturoAsync<T> enHiloPrincipal(Consumer<T> consumidor) {
        delegado.thenAccept(valor -> tareas.sync(() -> consumidor.accept(valor)));
        return this;
    }

    public FuturoAsync<T> alFallar(Consumer<Throwable> consumidor) {
        delegado.exceptionally(t -> {
            tareas.sync(() -> consumidor.accept(t));
            return null;
        });
        return this;
    }

    public <R> FuturoAsync<R> mapear(Function<T, R> mapeador) {
        return new FuturoAsync<>(delegado.thenApply(mapeador), tareas);
    }

    public CompletableFuture<T> futuro() {
        return delegado;
    }
}
