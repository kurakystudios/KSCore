package studio.kuraky.kSCore.configuracion;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Traduce entre instancias anotadas con {@link Archivo}/{@link Seccion}
 * y una representación canónica {@code Map<String, Object>} usada por
 * cada formato concreto.
 */
final class Mapeador {

    private static final Map<Class<?>, List<Field>> CAMPOS = new ConcurrentHashMap<>();

    private Mapeador() {}

    /** Serializa el objeto a un mapa ordenado siguiendo el orden de declaración. */
    static Map<String, Object> aMapa(Object objeto) {
        Map<String, Object> destino = new LinkedHashMap<>();
        for (Field campo : camposDe(objeto.getClass())) {
            Object valor = leerCampo(objeto, campo);
            destino.put(claveDe(campo), aValorConfig(campo, valor));
        }
        return destino;
    }

    /**
     * Vuelca sobre {@code objeto} los valores presentes en {@code mapa}.
     * Devuelve {@code true} si al menos una clave esperada estaba ausente
     * (indicador de que conviene reescribir el archivo para auto-migrar).
     */
    static boolean aObjeto(Object objeto, Map<String, Object> mapa) {
        boolean faltantes = false;
        for (Field campo : camposDe(objeto.getClass())) {
            String clave = claveDe(campo);
            if (!mapa.containsKey(clave)) {
                faltantes = true;
                continue;
            }
            Object bruto = mapa.get(clave);
            if (campo.isAnnotationPresent(Seccion.class)) {
                Object anidado = leerCampo(objeto, campo);
                if (anidado == null) continue;
                Map<String, Object> sub = comoMapa(bruto);
                if (sub == null) {
                    faltantes = true;
                    continue;
                }
                if (aObjeto(anidado, sub)) faltantes = true;
            } else {
                Object convertido = deValorConfig(campo.getGenericType(), campo.getType(), bruto);
                if (convertido != null) escribirCampo(objeto, campo, convertido);
            }
        }
        return faltantes;
    }

    /** Extrae los comentarios declarados como {@code Map<claveJerárquica, líneas>}. */
    static Map<String, List<String>> comentarios(Object objeto) {
        Map<String, List<String>> destino = new LinkedHashMap<>();
        recolectarComentarios("", objeto, destino);
        return destino;
    }

    private static void recolectarComentarios(String prefijo, Object objeto, Map<String, List<String>> destino) {
        for (Field campo : camposDe(objeto.getClass())) {
            String clave = claveDe(campo);
            String plena = prefijo.isEmpty() ? clave : prefijo + "." + clave;
            Comentario c = campo.getAnnotation(Comentario.class);
            if (c != null && c.value().length > 0) {
                destino.put(plena, List.of(c.value()));
            }
            if (campo.isAnnotationPresent(Seccion.class)) {
                Object anidado = leerCampo(objeto, campo);
                if (anidado != null) recolectarComentarios(plena, anidado, destino);
            }
        }
    }

    private static Object aValorConfig(Field campo, Object valor) {
        if (valor == null) return null;
        if (campo.isAnnotationPresent(Seccion.class)) {
            return aMapa(valor);
        }
        return aValorPrimitivo(valor);
    }

    private static Object aValorPrimitivo(Object valor) {
        if (valor == null) return null;
        if (valor instanceof Enum<?> e) return e.name();
        if (valor instanceof UUID u) return u.toString();
        if (valor instanceof Instant i) return i.toString();
        if (valor instanceof Collection<?> col) {
            List<Object> salida = new ArrayList<>(col.size());
            for (Object elemento : col) salida.add(aValorPrimitivo(elemento));
            return salida;
        }
        if (valor instanceof Map<?, ?> mapa) {
            Map<String, Object> salida = new LinkedHashMap<>(mapa.size());
            for (Map.Entry<?, ?> e : mapa.entrySet()) {
                salida.put(String.valueOf(e.getKey()), aValorPrimitivo(e.getValue()));
            }
            return salida;
        }
        return valor;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object deValorConfig(Type genericType, Class<?> tipo, Object bruto) {
        if (bruto == null) return null;

        if (tipo == String.class) return bruto.toString();
        if (tipo == boolean.class || tipo == Boolean.class) return coercerBool(bruto);
        if (tipo == int.class || tipo == Integer.class) return coercerLong(bruto).intValue();
        if (tipo == long.class || tipo == Long.class) return coercerLong(bruto);
        if (tipo == double.class || tipo == Double.class) return coercerDouble(bruto);
        if (tipo == float.class || tipo == Float.class) return coercerDouble(bruto).floatValue();
        if (tipo == short.class || tipo == Short.class) return coercerLong(bruto).shortValue();
        if (tipo == byte.class || tipo == Byte.class) return coercerLong(bruto).byteValue();

        if (tipo == UUID.class) return UUID.fromString(bruto.toString());
        if (tipo == Instant.class) return Instant.parse(bruto.toString());

        if (tipo.isEnum()) {
            String nombre = bruto.toString().toUpperCase(Locale.ROOT);
            return Enum.valueOf((Class<Enum>) tipo, nombre);
        }

        if (List.class.isAssignableFrom(tipo)) {
            if (!(bruto instanceof Collection<?> col)) return null;
            Class<?> tipoElem = tipoDeParametro(genericType);
            List<Object> salida = new ArrayList<>(col.size());
            for (Object elemento : col) {
                if (tipoElem != null) {
                    salida.add(deValorConfig(tipoElem, tipoElem, elemento));
                } else {
                    salida.add(elemento);
                }
            }
            return salida;
        }

        if (Map.class.isAssignableFrom(tipo)) {
            Map<String, Object> convertido = comoMapa(bruto);
            return convertido != null ? new LinkedHashMap<>(convertido) : null;
        }

        return bruto;
    }

    private static Class<?> tipoDeParametro(Type generic) {
        if (generic instanceof ParameterizedType pt) {
            Type[] args = pt.getActualTypeArguments();
            if (args.length > 0 && args[0] instanceof Class<?> c) return c;
        }
        return null;
    }

    private static Number coercerLong(Object bruto) {
        if (bruto instanceof Number n) return n.longValue();
        return Long.parseLong(bruto.toString());
    }

    private static Number coercerDouble(Object bruto) {
        if (bruto instanceof Number n) return n.doubleValue();
        return Double.parseDouble(bruto.toString());
    }

    private static Boolean coercerBool(Object bruto) {
        if (bruto instanceof Boolean b) return b;
        return Boolean.parseBoolean(bruto.toString());
    }

    /**
     * Devuelve el valor como {@code Map<String, Object>} si es compatible.
     * Acepta tanto mapas nativos como estructuras específicas de cada
     * formato que exponen sus entradas mediante {@code valueMap()}.
     */
    @SuppressWarnings("unchecked")
    static Map<String, Object> comoMapa(Object bruto) {
        if (bruto instanceof Map<?, ?> m) {
            Map<String, Object> salida = new LinkedHashMap<>(m.size());
            for (Map.Entry<?, ?> e : m.entrySet()) {
                salida.put(String.valueOf(e.getKey()), e.getValue());
            }
            return salida;
        }
        return null;
    }

    private static List<Field> camposDe(Class<?> clase) {
        return CAMPOS.computeIfAbsent(clase, c -> {
            List<Field> lista = new ArrayList<>();
            for (Class<?> actual = c; actual != null && actual != Object.class; actual = actual.getSuperclass()) {
                Field[] declarados = actual.getDeclaredFields();
                Arrays.sort(declarados, (a, b) -> a.getName().compareTo(b.getName()));
                for (Field f : declarados) {
                    int mods = f.getModifiers();
                    if (Modifier.isStatic(mods) || Modifier.isTransient(mods)) continue;
                    if (!Modifier.isPublic(mods)) continue;
                    if (f.isAnnotationPresent(Ignorar.class)) continue;
                    lista.add(f);
                }
            }
            return List.copyOf(lista);
        });
    }

    private static String claveDe(Field campo) {
        Clave clave = campo.getAnnotation(Clave.class);
        if (clave != null) return clave.value();
        Seccion sec = campo.getAnnotation(Seccion.class);
        if (sec != null && !sec.value().isEmpty()) return sec.value();
        return campo.getName();
    }

    private static Object leerCampo(Object objeto, Field campo) {
        try {
            return campo.get(objeto);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("No se pudo leer el campo " + campo.getName(), e);
        }
    }

    private static void escribirCampo(Object objeto, Field campo, Object valor) {
        try {
            campo.set(objeto, valor);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("No se pudo escribir el campo " + campo.getName(), e);
        }
    }
}
