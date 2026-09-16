package studio.kuraky.kSCore.comandos;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import studio.kuraky.kSCore.mensajes.Mensajes;
import studio.kuraky.kSCore.utilidades.Tiempos;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Registro global de convertidores. Cargado con los tipos estándar de
 * Bukkit/Paper y ampliable con {@link #registrar(Class, Convertidor)}.
 */
public final class Convertidores {

    private static final Map<Class<?>, Convertidor<?>> registro = new ConcurrentHashMap<>();

    static {
        registrarPredeterminados();
    }

    private Convertidores() {}

    public static <T> void registrar(Class<T> tipo, Convertidor<T> convertidor) {
        registro.put(tipo, convertidor);
    }

    @SuppressWarnings("unchecked")
    public static <T> Convertidor<T> obtener(Class<T> tipo) {
        Convertidor<?> c = registro.get(tipo);
        if (c == null && tipo.isEnum()) {
            c = convertidorEnum(tipo);
            registro.put(tipo, c);
        }
        return (Convertidor<T>) c;
    }

    public static boolean soportado(Class<?> tipo) {
        return registro.containsKey(tipo) || tipo.isEnum() || tipo.isArray();
    }

    private static void registrarPredeterminados() {
        registrar(String.class, (ctx, t) -> t);

        registrar(int.class, Convertidores::convertirInt);
        registrar(Integer.class, Convertidores::convertirInt);
        registrar(long.class, Convertidores::convertirLong);
        registrar(Long.class, Convertidores::convertirLong);
        registrar(double.class, Convertidores::convertirDouble);
        registrar(Double.class, Convertidores::convertirDouble);
        registrar(float.class, (ctx, t) -> (float) (double) convertirDouble(ctx, t));
        registrar(Float.class, (ctx, t) -> (float) (double) convertirDouble(ctx, t));
        registrar(boolean.class, Convertidores::convertirBool);
        registrar(Boolean.class, Convertidores::convertirBool);

        registrar(UUID.class, (ctx, t) -> {
            try {
                return UUID.fromString(t);
            } catch (IllegalArgumentException e) {
                throw errorArgumento("UUID no válido: " + t);
            }
        });

        registrar(Duration.class, (ctx, t) -> Tiempos.parsear(t)
                .orElseThrow(() -> errorArgumento("Duración no válida: " + t)));

        registrar(Material.class, new Convertidor<Material>() {
            @Override public Material convertir(Contexto ctx, String t) {
                Material m = Material.matchMaterial(t);
                if (m == null) throw errorArgumento("Material desconocido: " + t);
                return m;
            }
            @Override public List<String> sugerencias(Contexto ctx, String parcial) {
                String necesita = parcial.toLowerCase(Locale.ROOT);
                List<String> salida = new ArrayList<>();
                for (Material m : Material.values()) {
                    if (m.isLegacy()) continue;
                    String nombre = m.getKey().getKey();
                    if (nombre.startsWith(necesita) || m.name().toLowerCase(Locale.ROOT).startsWith(necesita)) {
                        salida.add(nombre);
                        if (salida.size() >= 50) break;
                    }
                }
                return salida;
            }
        });

        registrar(World.class, new Convertidor<World>() {
            @Override public World convertir(Contexto ctx, String t) {
                World w = Bukkit.getWorld(t);
                if (w == null) throw errorArgumento(Mensajes.disponible()
                        ? Mensajes.de("comun.mundo-no-encontrado").replace("%mundo%", t)
                        : "Mundo no encontrado: " + t);
                return w;
            }
            @Override public List<String> sugerencias(Contexto ctx, String parcial) {
                return Bukkit.getWorlds().stream()
                        .map(World::getName)
                        .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(parcial.toLowerCase(Locale.ROOT)))
                        .collect(Collectors.toList());
            }
        });

        registrar(Player.class, new Convertidor<Player>() {
            @Override public Player convertir(Contexto ctx, String t) {
                Player p = Bukkit.getPlayerExact(t);
                if (p == null) throw errorArgumento(Mensajes.disponible()
                        ? Mensajes.de("comun.jugador-no-encontrado").replace("%jugador%", t)
                        : "Jugador no encontrado: " + t);
                return p;
            }
            @Override public List<String> sugerencias(Contexto ctx, String parcial) {
                String p = parcial.toLowerCase(Locale.ROOT);
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(p))
                        .limit(50)
                        .collect(Collectors.toList());
            }
        });

        registrar(OfflinePlayer.class, new Convertidor<OfflinePlayer>() {
            @Override public OfflinePlayer convertir(Contexto ctx, String t) {
                OfflinePlayer op = Bukkit.getOfflinePlayerIfCached(t);
                if (op == null) throw errorArgumento(Mensajes.disponible()
                        ? Mensajes.de("comun.jugador-no-encontrado").replace("%jugador%", t)
                        : "Jugador no encontrado: " + t);
                return op;
            }
            @Override public List<String> sugerencias(Contexto ctx, String parcial) {
                String p = parcial.toLowerCase(Locale.ROOT);
                return Arrays.stream(Bukkit.getOfflinePlayers())
                        .map(OfflinePlayer::getName)
                        .filter(n -> n != null && n.toLowerCase(Locale.ROOT).startsWith(p))
                        .limit(50)
                        .collect(Collectors.toList());
            }
        });

        // String... — se registra bajo la clase array; la construcción del árbol
        // detecta parámetros varargs y usa GreedyString.
        registrar(String[].class, new Convertidor<String[]>() {
            @Override public String[] convertir(Contexto ctx, String texto) {
                if (texto == null || texto.isBlank()) return new String[0];
                return texto.trim().split("\\s+");
            }
            @Override public boolean absorbeResto() { return true; }
        });
    }

    private static Integer convertirInt(Contexto ctx, String t) {
        try {
            return Integer.parseInt(t);
        } catch (NumberFormatException e) {
            throw errorArgumento("Número entero no válido: " + t);
        }
    }

    private static Long convertirLong(Contexto ctx, String t) {
        try {
            return Long.parseLong(t);
        } catch (NumberFormatException e) {
            throw errorArgumento("Número entero no válido: " + t);
        }
    }

    private static Double convertirDouble(Contexto ctx, String t) {
        try {
            return Double.parseDouble(t);
        } catch (NumberFormatException e) {
            throw errorArgumento("Número decimal no válido: " + t);
        }
    }

    private static Boolean convertirBool(Contexto ctx, String t) {
        String lower = t.toLowerCase(Locale.ROOT);
        if (lower.equals("true") || lower.equals("si") || lower.equals("yes")) return true;
        if (lower.equals("false") || lower.equals("no")) return false;
        throw errorArgumento("Valor booleano no válido: " + t);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Convertidor<?> convertidorEnum(Class<?> tipo) {
        Class<? extends Enum> claseEnum = (Class<? extends Enum>) tipo;
        Object[] valores = claseEnum.getEnumConstants();
        return new Convertidor<Object>() {
            @Override public Object convertir(Contexto ctx, String texto) {
                try {
                    return Enum.valueOf(claseEnum, texto.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    throw errorArgumento("Valor no válido para " + tipo.getSimpleName() + ": " + texto);
                }
            }
            @Override public List<String> sugerencias(Contexto ctx, String parcial) {
                String p = parcial.toLowerCase(Locale.ROOT);
                List<String> salida = new ArrayList<>(valores.length);
                for (Object v : valores) {
                    String n = ((Enum<?>) v).name().toLowerCase(Locale.ROOT);
                    if (n.startsWith(p)) salida.add(((Enum<?>) v).name());
                }
                return salida;
            }
        };
    }

    private static ErrorArgumento errorArgumento(String mensaje) {
        return new ErrorArgumento("&c" + mensaje);
    }
}
