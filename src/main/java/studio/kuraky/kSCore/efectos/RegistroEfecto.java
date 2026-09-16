package studio.kuraky.kSCore.efectos;

import studio.kuraky.kSCore.datos.Columna;
import studio.kuraky.kSCore.datos.Id;
import studio.kuraky.kSCore.datos.Tabla;

import java.util.UUID;

/**
 * Entidad persistente para efectos con {@code @Efecto(persistente = true)}.
 * Un registro por (jugador, tipo). La clave es un compuesto textual
 * {@code uuid|tipo} porque nuestros descriptores actuales sólo
 * soportan una columna @Id.
 */
@Tabla(nombre = "kscore_efectos", cacheMinutos = 5, cacheMaximo = 500)
public final class RegistroEfecto {

    @Id
    public String clave;

    @Columna(indice = true)
    public UUID jugador;

    @Columna(indice = true)
    public String tipo;

    @Columna
    public int nivel;

    @Columna
    public long ticksRestantes;

    @Columna
    public long guardadoEn;

    public static String claveDe(UUID jugador, String tipo) {
        return jugador + "|" + tipo;
    }
}
