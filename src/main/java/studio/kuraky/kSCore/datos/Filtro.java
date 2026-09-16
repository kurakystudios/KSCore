package studio.kuraky.kSCore.datos;

import java.util.ArrayList;
import java.util.List;

/**
 * Constructor fluído de cláusulas {@code WHERE}. Se admiten operadores
 * básicos e igualdad de columnas con parámetros posicionales.
 *
 * <pre>{@code
 * Filtro f = Filtro.donde("nivel").mayorQue(10)
 *                   .y("monedas").menorQue(500);
 * }</pre>
 */
public final class Filtro {

    private final List<Termino> terminos = new ArrayList<>();
    private String pendienteColumna;
    private boolean pendienteEsOr;

    private Filtro() {}

    public static Filtro donde(String columna) {
        Filtro f = new Filtro();
        f.pendienteColumna = columna;
        f.pendienteEsOr = false;
        return f;
    }

    public Filtro y(String columna) {
        this.pendienteColumna = columna;
        this.pendienteEsOr = false;
        return this;
    }

    public Filtro o(String columna) {
        this.pendienteColumna = columna;
        this.pendienteEsOr = true;
        return this;
    }

    public Filtro igualA(Object valor) { return terminar("=", valor); }
    public Filtro distintoDe(Object valor) { return terminar("<>", valor); }
    public Filtro mayorQue(Object valor) { return terminar(">", valor); }
    public Filtro mayorOIgual(Object valor) { return terminar(">=", valor); }
    public Filtro menorQue(Object valor) { return terminar("<", valor); }
    public Filtro menorOIgual(Object valor) { return terminar("<=", valor); }
    public Filtro pareceA(String patron) { return terminar("LIKE", patron); }

    private Filtro terminar(String operador, Object valor) {
        if (pendienteColumna == null) {
            throw new IllegalStateException("No hay columna pendiente; usa donde()/y()/o() antes de un operador.");
        }
        terminos.add(new Termino(pendienteColumna, operador, valor, pendienteEsOr));
        pendienteColumna = null;
        return this;
    }

    public boolean estaVacio() {
        return terminos.isEmpty();
    }

    public SqlParametros aSql(DialectoSql dialecto) {
        if (terminos.isEmpty()) return new SqlParametros("", List.of());
        StringBuilder sb = new StringBuilder(" WHERE ");
        List<Object> params = new ArrayList<>(terminos.size());
        for (int i = 0; i < terminos.size(); i++) {
            Termino t = terminos.get(i);
            if (i > 0) sb.append(t.esOr ? " OR " : " AND ");
            sb.append(dialecto.citar(t.columna))
              .append(' ').append(t.operador).append(" ?");
            params.add(t.valor);
        }
        return new SqlParametros(sb.toString(), params);
    }

    /** Registro público con SQL y parámetros. */
    public record SqlParametros(String sql, List<Object> parametros) {}

    private record Termino(String columna, String operador, Object valor, boolean esOr) {}
}
