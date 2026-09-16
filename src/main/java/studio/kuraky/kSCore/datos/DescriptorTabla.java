package studio.kuraky.kSCore.datos;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Metadatos compilados de una tabla, calculados a partir de la clase
 * anotada con {@link Tabla} durante el arranque.
 */
public final class DescriptorTabla {

    private final Class<?> clase;
    private final String nombre;
    private final int cacheMinutos;
    private final int cacheMaximo;
    private final DescriptorColumna id;
    private final List<DescriptorColumna> columnas;
    private final Constructor<?> constructor;

    private DescriptorTabla(Class<?> clase, String nombre, int cacheMinutos, int cacheMaximo,
                            DescriptorColumna id, List<DescriptorColumna> columnas,
                            Constructor<?> constructor) {
        this.clase = clase;
        this.nombre = nombre;
        this.cacheMinutos = cacheMinutos;
        this.cacheMaximo = cacheMaximo;
        this.id = id;
        this.columnas = List.copyOf(columnas);
        this.constructor = constructor;
    }

    public Class<?> clase() { return clase; }
    public String nombre() { return nombre; }
    public int cacheMinutos() { return cacheMinutos; }
    public int cacheMaximo() { return cacheMaximo; }
    public DescriptorColumna id() { return id; }
    public List<DescriptorColumna> columnas() { return columnas; }

    public Object instanciar() throws ReflectiveOperationException {
        return constructor.newInstance();
    }

    public static DescriptorTabla de(Class<?> clase) {
        Tabla anot = clase.getAnnotation(Tabla.class);
        if (anot == null) {
            throw new IllegalArgumentException("La clase " + clase.getName() + " no tiene @Tabla.");
        }
        String nombre = anot.nombre().isEmpty() ? snakeCase(clase.getSimpleName()) : anot.nombre();

        Constructor<?> ctor;
        try {
            ctor = clase.getDeclaredConstructor();
            ctor.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("La clase " + clase.getName()
                    + " necesita un constructor sin argumentos.", e);
        }

        DescriptorColumna id = null;
        List<DescriptorColumna> columnas = new ArrayList<>();
        MethodHandles.Lookup lookup = MethodHandles.lookup();

        for (Class<?> actual = clase; actual != null && actual != Object.class; actual = actual.getSuperclass()) {
            for (Field campo : actual.getDeclaredFields()) {
                int mods = campo.getModifiers();
                if (Modifier.isStatic(mods) || Modifier.isTransient(mods)) continue;
                if (campo.isAnnotationPresent(Ignorar.class)) continue;
                if (!Modifier.isPublic(mods)) continue;

                Id anotId = campo.getAnnotation(Id.class);
                Columna anotCol = campo.getAnnotation(Columna.class);
                if (anotId == null && anotCol == null) continue;

                String nombreCol;
                boolean esId = anotId != null;
                boolean indice = anotCol != null && anotCol.indice();
                boolean nullable = anotCol == null || anotCol.nullable();
                int longitud = anotCol != null ? anotCol.longitud() : 36;
                if (esId) {
                    nombreCol = anotId.value().isEmpty() ? snakeCase(campo.getName()) : anotId.value();
                    nullable = false;
                } else {
                    nombreCol = anotCol.value().isEmpty() ? snakeCase(campo.getName()) : anotCol.value();
                }

                MethodHandle getter, setter;
                try {
                    getter = lookup.unreflectGetter(campo);
                    setter = lookup.unreflectSetter(campo);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException("No se pudo obtener acceso a " + campo, e);
                }

                DescriptorColumna dc = new DescriptorColumna(campo, nombreCol, esId, indice,
                        nullable, longitud, getter, setter);
                columnas.add(dc);
                if (esId) {
                    if (id != null) {
                        throw new IllegalStateException("Más de un @Id en " + clase.getName());
                    }
                    id = dc;
                }
            }
        }

        if (id == null) {
            throw new IllegalStateException("La clase " + clase.getName() + " no declara @Id.");
        }
        return new DescriptorTabla(clase, nombre, anot.cacheMinutos(), anot.cacheMaximo(),
                id, columnas, ctor);
    }

    static String snakeCase(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 4);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) sb.append('_');
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString().toLowerCase(Locale.ROOT);
    }
}
