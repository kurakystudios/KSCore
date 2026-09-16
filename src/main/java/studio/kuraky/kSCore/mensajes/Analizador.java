package studio.kuraky.kSCore.mensajes;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class Analizador {

    private final LoadingCache<String, Component> cache;

    public Analizador() {
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(30))
                .maximumSize(2_000L)
                .build(this::construir);
    }

    public Component analizar(String texto) {
        if (texto == null || texto.isEmpty()) return Component.empty();
        return cache.get(texto);
    }

    public Component analizar(String texto, Map<String, ?> variables) {
        if (texto == null || texto.isEmpty()) return Component.empty();
        if (variables == null || variables.isEmpty()) return analizar(texto);
        String sustituido = sustituirVariables(texto, variables);
        return cache.get(sustituido);
    }

    public String sinFormato(String texto) {
        return PlainTextComponentSerializer.plainText().serialize(analizar(texto));
    }

    public long entradasCacheadas() {
        return cache.estimatedSize();
    }

    public void invalidarCache() {
        cache.invalidateAll();
    }

    private String sustituirVariables(String texto, Map<String, ?> variables) {
        if (texto.indexOf('%') < 0) return texto;
        StringBuilder sb = new StringBuilder(texto.length());
        int i = 0;
        int n = texto.length();
        while (i < n) {
            char c = texto.charAt(i);
            if (c == '%') {
                int fin = texto.indexOf('%', i + 1);
                if (fin > i + 1) {
                    String clave = texto.substring(i + 1, fin);
                    Object valor = variables.get(clave);
                    if (valor != null) {
                        sb.append(valor);
                        i = fin + 1;
                        continue;
                    }
                }
            }
            sb.append(c);
            i++;
        }
        return sb.toString();
    }

    private Component construir(String texto) {
        TextComponent.Builder salida = Component.text();
        StringBuilder buffer = new StringBuilder();
        List<TextColor> paradas = new ArrayList<>(2);
        Style estilo = Style.empty();
        int i = 0;
        int n = texto.length();

        while (i < n) {
            char c = texto.charAt(i);

            if (c == '\\' && i + 1 < n && texto.charAt(i + 1) == 'n') {
                volcar(salida, buffer, paradas, estilo);
                salida.append(Component.newline());
                i += 2;
                continue;
            }

            if ((c == '&' || c == '§') && i + 1 < n) {
                char codigo = Character.toLowerCase(texto.charAt(i + 1));
                if (esColorLegado(codigo)) {
                    volcar(salida, buffer, paradas, estilo);
                    estilo = (codigo == 'r') ? Style.empty() : estilo.color(colorLegado(codigo));
                    i += 2;
                    continue;
                }
                if (esFormatoLegado(codigo)) {
                    if (paradas.isEmpty() && !buffer.isEmpty()) {
                        salida.append(Component.text(buffer.toString(), estilo));
                        buffer.setLength(0);
                    }
                    estilo = aplicarFormato(estilo, codigo);
                    i += 2;
                    continue;
                }
            }

            if (c == '#' && i + 6 < n && esHex(texto, i + 1, 6)) {
                volcar(salida, buffer, paradas, estilo);
                estilo = estilo.color(TextColor.fromHexString("#" + texto.substring(i + 1, i + 7)));
                i += 7;
                continue;
            }

            if (c == '<' && i + 7 < n && texto.charAt(i + 7) == '>' && esHex(texto, i + 1, 6)) {
                paradas.add(TextColor.fromHexString("#" + texto.substring(i + 1, i + 7)));
                i += 8;
                continue;
            }

            buffer.append(c);
            i++;
        }

        volcar(salida, buffer, paradas, estilo);
        return salida.build();
    }

    private static void volcar(TextComponent.Builder salida, StringBuilder buffer,
                               List<TextColor> paradas, Style estilo) {
        if (buffer.isEmpty()) {
            paradas.clear();
            return;
        }
        if (paradas.isEmpty()) {
            salida.append(Component.text(buffer.toString(), estilo));
        } else if (paradas.size() == 1) {
            salida.append(Component.text(buffer.toString(), estilo.color(paradas.get(0))));
        } else {
            salida.append(Gradiente.aplicar(buffer.toString(), paradas, estilo));
        }
        buffer.setLength(0);
        paradas.clear();
    }

    private static boolean esColorLegado(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || c == 'r';
    }

    private static boolean esFormatoLegado(char c) {
        return c == 'k' || c == 'l' || c == 'm' || c == 'n' || c == 'o';
    }

    private static boolean esHex(String s, int desde, int longitud) {
        if (desde + longitud > s.length()) return false;
        for (int j = desde; j < desde + longitud; j++) {
            char c = s.charAt(j);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'))) {
                return false;
            }
        }
        return true;
    }

    private static Style aplicarFormato(Style base, char codigo) {
        return switch (codigo) {
            case 'l' -> base.decoration(TextDecoration.BOLD, true);
            case 'o' -> base.decoration(TextDecoration.ITALIC, true);
            case 'n' -> base.decoration(TextDecoration.UNDERLINED, true);
            case 'm' -> base.decoration(TextDecoration.STRIKETHROUGH, true);
            case 'k' -> base.decoration(TextDecoration.OBFUSCATED, true);
            default -> base;
        };
    }

    private static TextColor colorLegado(char c) {
        return switch (c) {
            case '0' -> NamedTextColor.BLACK;
            case '1' -> NamedTextColor.DARK_BLUE;
            case '2' -> NamedTextColor.DARK_GREEN;
            case '3' -> NamedTextColor.DARK_AQUA;
            case '4' -> NamedTextColor.DARK_RED;
            case '5' -> NamedTextColor.DARK_PURPLE;
            case '6' -> NamedTextColor.GOLD;
            case '7' -> NamedTextColor.GRAY;
            case '8' -> NamedTextColor.DARK_GRAY;
            case '9' -> NamedTextColor.BLUE;
            case 'a' -> NamedTextColor.GREEN;
            case 'b' -> NamedTextColor.AQUA;
            case 'c' -> NamedTextColor.RED;
            case 'd' -> NamedTextColor.LIGHT_PURPLE;
            case 'e' -> NamedTextColor.YELLOW;
            case 'f' -> NamedTextColor.WHITE;
            default -> NamedTextColor.WHITE;
        };
    }
}
