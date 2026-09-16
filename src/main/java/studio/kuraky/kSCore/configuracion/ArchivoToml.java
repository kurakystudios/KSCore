package studio.kuraky.kSCore.configuracion;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.electronwill.nightconfig.toml.TomlParser;
import com.electronwill.nightconfig.toml.TomlWriter;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementación de {@link ArchivoBase} sobre night-config para TOML.
 * Los comentarios se preservan mediante {@link CommentedConfig}.
 */
public class ArchivoToml extends ArchivoBase {

    @Override
    protected Map<String, Object> leerDisco() throws IOException {
        TomlParser parser = TomlFormat.instance().createParser();
        try (Reader r = Files.newBufferedReader(ruta(), StandardCharsets.UTF_8)) {
            Config config = parser.parse(r);
            return aMapa(config);
        }
    }

    @Override
    protected void escribirDisco(Map<String, Object> valores,
                                 Map<String, List<String>> comentarios) throws IOException {
        CommentedConfig config = CommentedConfig.inMemory();
        volcar(config, valores);
        for (Map.Entry<String, List<String>> e : comentarios.entrySet()) {
            config.setComment(e.getKey(), String.join("\n", e.getValue()));
        }
        TomlWriter writer = TomlFormat.instance().createWriter();
        EscrituraAtomica.escribirTexto(ruta(), writer.writeToString(config));
    }

    private static void volcar(Config destino, Map<String, Object> valores) {
        for (Map.Entry<String, Object> e : valores.entrySet()) {
            Object valor = e.getValue();
            if (valor instanceof Map<?, ?> sub) {
                @SuppressWarnings("unchecked")
                Map<String, Object> subMapa = (Map<String, Object>) sub;
                Config subConfig = destino.createSubConfig();
                volcar(subConfig, subMapa);
                destino.set(List.of(e.getKey()), subConfig);
            } else {
                destino.set(List.of(e.getKey()), valor);
            }
        }
    }

    private static Map<String, Object> aMapa(Config config) {
        Map<String, Object> destino = new LinkedHashMap<>();
        for (Config.Entry entrada : config.entrySet()) {
            Object valor = entrada.getValue();
            if (valor instanceof Config sub) {
                destino.put(entrada.getKey(), aMapa(sub));
            } else {
                destino.put(entrada.getKey(), valor);
            }
        }
        return destino;
    }
}
