package studio.kuraky.kSCore.mensajes;

import studio.kuraky.kSCore.configuracion.AlRecargar;
import studio.kuraky.kSCore.configuracion.Archivo;
import studio.kuraky.kSCore.configuracion.ArchivoYaml;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Archivo YAML de traducciones/cadenas del núcleo. El contenido es
 * dinámico (claves arbitrarias definidas por el usuario), por lo que se
 * expone el mapa aplanado en lugar de mapearlo a campos fijos.
 */
@Archivo(ruta = "mensajes.yml")
public final class ConfigMensajes extends ArchivoYaml {

    private volatile Map<String, String> aplanado = Map.of();

    @Override
    protected void alCargado(Map<String, Object> bruto) {
        Map<String, String> destino = new LinkedHashMap<>();
        aplanar("", bruto, destino);
        this.aplanado = Map.copyOf(destino);
    }

    /** Devuelve el mapa {@code clave.subclave → valor} listo para {@code Mensajes.de(...)}. */
    public Map<String, String> aplanado() {
        return aplanado;
    }

    @AlRecargar
    @SuppressWarnings("unused")
    private void empujarAMensajes() {
        if (Mensajes.disponible()) {
            Mensajes.actualizarCadenas(aplanado);
        }
    }

    private static void aplanar(String prefijo, Map<String, Object> origen, Map<String, String> destino) {
        for (Map.Entry<String, Object> e : origen.entrySet()) {
            String plena = prefijo.isEmpty() ? e.getKey() : prefijo + "." + e.getKey();
            Object valor = e.getValue();
            if (valor instanceof Map<?, ?> sub) {
                @SuppressWarnings("unchecked")
                Map<String, Object> mapa = (Map<String, Object>) sub;
                aplanar(plena, mapa, destino);
            } else if (valor != null) {
                destino.put(plena, String.valueOf(valor));
            }
        }
    }

    // Punto de entrada auxiliar para escrituras dinámicas si en el futuro se
    // desea añadir cadenas por código; no se usa aún en Fase 3.
    @SuppressWarnings("unused")
    public void establecer(String clave, String valor) {
        Map<String, String> copia = new HashMap<>(aplanado);
        copia.put(clave, valor);
        this.aplanado = Map.copyOf(copia);
    }
}
