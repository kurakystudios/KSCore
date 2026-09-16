package studio.kuraky.kSCore.nucleo;

public interface Modulo {

    void iniciar(Nucleo nucleo);

    void detener();

    default String nombre() {
        return getClass().getSimpleName();
    }
}
