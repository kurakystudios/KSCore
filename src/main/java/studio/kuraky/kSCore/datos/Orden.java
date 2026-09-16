package studio.kuraky.kSCore.datos;

import java.util.ArrayList;
import java.util.List;

/** Cláusula {@code ORDER BY} con múltiples niveles. */
public final class Orden {

    private final List<Termino> terminos = new ArrayList<>();

    private Orden() {}

    public static Orden asc(String columna) {
        Orden o = new Orden();
        o.terminos.add(new Termino(columna, false));
        return o;
    }

    public static Orden desc(String columna) {
        Orden o = new Orden();
        o.terminos.add(new Termino(columna, true));
        return o;
    }

    public Orden luegoAsc(String columna) {
        terminos.add(new Termino(columna, false));
        return this;
    }

    public Orden luegoDesc(String columna) {
        terminos.add(new Termino(columna, true));
        return this;
    }

    public boolean estaVacio() {
        return terminos.isEmpty();
    }

    public String aSql(DialectoSql dialecto) {
        if (terminos.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(" ORDER BY ");
        for (int i = 0; i < terminos.size(); i++) {
            Termino t = terminos.get(i);
            if (i > 0) sb.append(", ");
            sb.append(dialecto.citar(t.columna)).append(t.descendente ? " DESC" : " ASC");
        }
        return sb.toString();
    }

    private record Termino(String columna, boolean descendente) {}
}
