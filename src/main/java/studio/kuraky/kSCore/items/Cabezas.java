package studio.kuraky.kSCore.items;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utilidades para construir perfiles de cabeza a partir de las
 * distintas notaciones que usan los sitios de skins (URL de textura,
 * hash y base64 crudo). Cachea el {@link ResolvableProfile} por clave
 * para no reconstruirlo en cada uso.
 */
public final class Cabezas {

    private static final String PREFIJO_TEXTURAS = "https://textures.minecraft.net/texture/";
    private static final UUID ID_ESTATICO = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final Map<String, ResolvableProfile> CACHE = new ConcurrentHashMap<>();

    private Cabezas() {}

    /**
     * Interpreta {@code textura} y devuelve el {@link ResolvableProfile}
     * correspondiente. Acepta:
     * <ul>
     *   <li>URLs completas de textura {@code https://...}</li>
     *   <li>Hashes crudos (los devueltos por minecraft-heads.com)</li>
     *   <li>Cadenas base64 largas (JSON con la propiedad {@code textures})</li>
     * </ul>
     */
    public static ResolvableProfile porTextura(String textura) {
        if (textura == null || textura.isBlank()) {
            throw new IllegalArgumentException("Textura vacía.");
        }
        return CACHE.computeIfAbsent(textura, Cabezas::construir);
    }

    /** Perfil para una URL {@code https://textures.minecraft.net/...}. */
    public static ResolvableProfile porUrl(String url) {
        return porTextura(url);
    }

    /** Perfil para un hash bruto de textura. */
    public static ResolvableProfile porHash(String hash) {
        return porTextura(PREFIJO_TEXTURAS + hash);
    }

    /** Perfil para el valor base64 exacto que aparece en minecraft-heads.com. */
    public static ResolvableProfile porBase64(String base64) {
        return porTextura(base64);
    }

    /** Perfil ligado a un jugador concreto (útil para paneles de perfil). */
    public static ResolvableProfile porJugador(OfflinePlayer jugador) {
        PlayerProfile p = jugador.getPlayerProfile();
        return ResolvableProfile.resolvableProfile(p);
    }

    /** Invalida la caché completa. */
    public static void limpiarCache() {
        CACHE.clear();
    }

    public static int tamanoCache() {
        return CACHE.size();
    }

    private static ResolvableProfile construir(String textura) {
        String base64 = resolverBase64(textura);
        PlayerProfile pp = Bukkit.createProfile(ID_ESTATICO, null);
        pp.getProperties().clear();
        pp.getProperties().add(new ProfileProperty("textures", base64));
        return ResolvableProfile.resolvableProfile(pp);
    }

    /**
     * Resuelve el valor base64 final para el {@code textures} de un
     * perfil a partir de la notación de entrada. Público a nivel de
     * paquete para poder testarse sin arrancar Bukkit.
     */
    static String resolverBase64(String textura) {
        String t = textura.trim();
        if (esBase64(t)) return t;
        String url;
        if (t.regionMatches(true, 0, "http", 0, 4)) {
            url = t;
        } else {
            url = PREFIJO_TEXTURAS + t;
        }
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + url + "\"}}}";
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Heurística: una cadena base64 útil comienza con {@code eyJ}
     * ("{\"...") tras la decodificación. Es más fiable que medir
     * longitud y evita falsos positivos con URLs cortas.
     */
    static boolean esBase64(String s) {
        if (s.length() < 40) return false;
        String head = s.substring(0, Math.min(8, s.length())).toLowerCase(Locale.ROOT);
        // "eyJ" es el prefijo estándar de base64 de "{\"", el inicio de todo JSON de skin.
        return head.startsWith("eyj");
    }
}
