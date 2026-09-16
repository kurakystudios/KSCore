package studio.kuraky.kSCore.configuracion;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementación de {@link ArchivoBase} sobre {@link YamlConfiguration}
 * de Bukkit. Preserva los comentarios superiores por clave mediante
 * {@code setComments}.
 */
public class ArchivoYaml extends ArchivoBase {

    @Override
    protected Map<String, Object> leerDisco() throws IOException {
        YamlConfiguration cfg = new YamlConfiguration();
        try {
            cfg.load(ruta().toFile());
        } catch (InvalidConfigurationException e) {
            throw new IOException("YAML inválido en " + rutaRelativa(), e);
        }
        return aMapa(cfg);
    }

    @Override
    protected void escribirDisco(Map<String, Object> valores,
                                 Map<String, List<String>> comentarios) throws IOException {
        YamlConfiguration cfg = new YamlConfiguration();
        volcar("", cfg, valores);
        for (Map.Entry<String, List<String>> e : comentarios.entrySet()) {
            cfg.setComments(e.getKey(), e.getValue());
        }
        EscrituraAtomica.escribirTexto(ruta(), cfg.saveToString());
    }

    private static void volcar(String prefijo, YamlConfiguration cfg, Map<String, Object> valores) {
        for (Map.Entry<String, Object> e : valores.entrySet()) {
            String ruta = prefijo.isEmpty() ? e.getKey() : prefijo + "." + e.getKey();
            Object valor = e.getValue();
            if (valor instanceof Map<?, ?> sub) {
                @SuppressWarnings("unchecked")
                Map<String, Object> subMapa = (Map<String, Object>) sub;
                volcar(ruta, cfg, subMapa);
            } else {
                cfg.set(ruta, valor);
            }
        }
    }

    private static Map<String, Object> aMapa(ConfigurationSection seccion) {
        Map<String, Object> destino = new LinkedHashMap<>();
        for (String clave : seccion.getKeys(false)) {
            Object valor = seccion.get(clave);
            if (valor instanceof ConfigurationSection sub) {
                destino.put(clave, aMapa(sub));
            } else if (valor instanceof List<?> lista) {
                destino.put(clave, new ArrayList<>(lista));
            } else {
                destino.put(clave, valor);
            }
        }
        return destino;
    }
}
