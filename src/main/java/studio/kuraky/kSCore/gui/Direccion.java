package studio.kuraky.kSCore.gui;

/** Dirección de navegación en un menú paginado. */
public enum Direccion {
    ANTERIOR(-1),
    SIGUIENTE(1);

    private final int delta;

    Direccion(int delta) {
        this.delta = delta;
    }

    public int delta() {
        return delta;
    }
}
