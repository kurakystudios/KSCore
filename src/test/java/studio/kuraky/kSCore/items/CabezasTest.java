package studio.kuraky.kSCore.items;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CabezasTest {

    private static String decodificar(String base64) {
        return new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
    }

    @Test
    void url_completa_se_envuelve_en_json() {
        String url = "https://textures.minecraft.net/texture/abcdefghijkl123456";
        String base64 = Cabezas.resolverBase64(url);
        String json = decodificar(base64);
        assertTrue(json.contains(url), "El JSON debería incluir la URL: " + json);
        assertTrue(json.startsWith("{\"textures\":{\"SKIN\":{\"url\":\""));
    }

    @Test
    void hash_bruto_se_convierte_a_url_de_textures() {
        String hash = "abcdefghijkl123456789";
        String base64 = Cabezas.resolverBase64(hash);
        String json = decodificar(base64);
        assertTrue(json.contains("https://textures.minecraft.net/texture/" + hash),
                "El JSON debería anteponer el prefijo de texturas: " + json);
    }

    @Test
    void base64_se_devuelve_sin_transformar() {
        // Un base64 real de minecraft-heads.com comienza con "eyJ..." (JSON encoded).
        String jsonOriginal = "{\"textures\":{\"SKIN\":{\"url\":\"https://textures.minecraft.net/texture/aaaa\"}}}";
        String base64 = Base64.getEncoder().encodeToString(jsonOriginal.getBytes(StandardCharsets.UTF_8));
        assertEquals(base64, Cabezas.resolverBase64(base64));
    }

    @Test
    void heuristica_base64_positiva() {
        // "eyJ..." es el prefijo típico
        String jsonOriginal = "{\"textures\":{\"SKIN\":{\"url\":\"https://textures.minecraft.net/texture/xxxxxxxxxxxxx\"}}}";
        String base64 = Base64.getEncoder().encodeToString(jsonOriginal.getBytes(StandardCharsets.UTF_8));
        assertTrue(Cabezas.esBase64(base64));
    }

    @Test
    void heuristica_base64_negativa_para_url_corta() {
        assertFalse(Cabezas.esBase64("https://ejemplo.com/x"));
    }

    @Test
    void heuristica_base64_negativa_para_texto_corto() {
        assertFalse(Cabezas.esBase64("abc"));
    }
}
