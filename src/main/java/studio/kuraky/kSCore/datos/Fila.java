package studio.kuraky.kSCore.datos;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Fila cruda devuelta por {@code Datos.consultaCruda(...)}. Envuelve
 * un mapa columna→valor inmutable.
 */
public final class Fila {

    private final Map<String, Object> valores;

    public Fila(Map<String, Object> valores) {
        this.valores = Map.copyOf(valores);
    }

    public Object obtener(String columna) {
        return valores.get(columna);
    }

    public String texto(String columna) {
        Object v = valores.get(columna);
        return v == null ? null : v.toString();
    }

    public int entero(String columna) {
        return ((Number) Objects.requireNonNull(valores.get(columna), columna)).intValue();
    }

    public long entero64(String columna) {
        return ((Number) Objects.requireNonNull(valores.get(columna), columna)).longValue();
    }

    public double decimal(String columna) {
        return ((Number) Objects.requireNonNull(valores.get(columna), columna)).doubleValue();
    }

    public boolean booleano(String columna) {
        Object v = valores.get(columna);
        if (v instanceof Boolean b) return b;
        if (v instanceof Number n) return n.intValue() != 0;
        throw new IllegalArgumentException("Columna " + columna + " no es booleana: " + v);
    }

    public Set<String> columnas() {
        return valores.keySet();
    }

    public Map<String, Object> comoMapa() {
        return valores;
    }
}
