package studio.kuraky.kSCore.datos;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;

/**
 * Metadatos compilados de una columna. Los {@link MethodHandle} se
 * calculan una sola vez durante el arranque.
 */
public final class DescriptorColumna {

    private final Field campo;
    private final String nombre;
    private final Class<?> tipoJava;
    private final boolean id;
    private final boolean indice;
    private final boolean nullable;
    private final int longitud;
    private final MethodHandle getter;
    private final MethodHandle setter;

    public DescriptorColumna(Field campo, String nombre, boolean id, boolean indice, boolean nullable,
                             int longitud, MethodHandle getter, MethodHandle setter) {
        this.campo = campo;
        this.nombre = nombre;
        this.tipoJava = campo.getType();
        this.id = id;
        this.indice = indice;
        this.nullable = nullable;
        this.longitud = longitud;
        this.getter = getter;
        this.setter = setter;
    }

    public Field campo() { return campo; }
    public String nombre() { return nombre; }
    public Class<?> tipoJava() { return tipoJava; }
    public boolean esId() { return id; }
    public boolean tieneIndice() { return indice; }
    public boolean nullable() { return nullable; }
    public int longitud() { return longitud; }

    public Object leer(Object entidad) throws Throwable {
        return getter.invoke(entidad);
    }

    public void escribir(Object entidad, Object valor) throws Throwable {
        setter.invoke(entidad, valor);
    }
}
