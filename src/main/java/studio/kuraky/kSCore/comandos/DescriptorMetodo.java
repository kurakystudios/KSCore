package studio.kuraky.kSCore.comandos;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Metadatos de un método de comando anotado con {@link Principal} o
 * {@link Sub}. La lista de parámetros excluye el {@code Contexto}
 * inicial, que se inyecta siempre.
 */
public final class DescriptorMetodo {

    private final Object instancia;
    private final Method metodo;
    private final String[] partesRuta;
    private final List<DescriptorParametro> parametros;
    private final String permiso;
    private final boolean soloJugador;
    private final String uso;
    private final int segundosCooldown;
    private final String claveCooldown;
    private final boolean async;

    public DescriptorMetodo(Object instancia, Method metodo, String[] partesRuta,
                            List<DescriptorParametro> parametros, String permiso,
                            boolean soloJugador, String uso, int segundosCooldown,
                            String claveCooldown, boolean async) {
        this.instancia = instancia;
        this.metodo = metodo;
        this.partesRuta = partesRuta;
        this.parametros = List.copyOf(parametros);
        this.permiso = permiso;
        this.soloJugador = soloJugador;
        this.uso = uso;
        this.segundosCooldown = segundosCooldown;
        this.claveCooldown = claveCooldown;
        this.async = async;
    }

    public Object instancia() { return instancia; }
    public Method metodo() { return metodo; }
    public String[] partesRuta() { return partesRuta; }
    public List<DescriptorParametro> parametros() { return parametros; }
    public String permiso() { return permiso; }
    public boolean soloJugador() { return soloJugador; }
    public String uso() { return uso; }
    public int segundosCooldown() { return segundosCooldown; }
    public String claveCooldown() { return claveCooldown; }
    public boolean async() { return async; }

    /** Devuelve la ruta como texto legible, usada en logs y errores. */
    public String rutaLegible() {
        return partesRuta.length == 0 ? "<principal>" : String.join(" ", partesRuta);
    }
}
