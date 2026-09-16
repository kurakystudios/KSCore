package studio.kuraky.kSCore.comandos;

import java.lang.reflect.Method;

/**
 * Metadatos de un parámetro de un método de comando.
 */
public final class DescriptorParametro {

    private final String nombre;
    private final Class<?> tipo;
    private final Convertidor<?> convertidor;
    private final boolean opcional;
    private final String defecto;
    private final boolean absorbeResto;
    private volatile Method completador;

    public DescriptorParametro(String nombre, Class<?> tipo, Convertidor<?> convertidor,
                               boolean opcional, String defecto, boolean absorbeResto) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.convertidor = convertidor;
        this.opcional = opcional;
        this.defecto = defecto;
        this.absorbeResto = absorbeResto;
    }

    public String nombre() { return nombre; }
    public Class<?> tipo() { return tipo; }
    public Convertidor<?> convertidor() { return convertidor; }
    public boolean opcional() { return opcional; }
    public String defecto() { return defecto; }
    public boolean absorbeResto() { return absorbeResto; }
    public Method completador() { return completador; }

    public void completadorPersonalizado(Method metodo) {
        this.completador = metodo;
    }
}
