package studio.kuraky.kSCore.chat;

import studio.kuraky.kSCore.configuracion.Archivo;
import studio.kuraky.kSCore.configuracion.ArchivoYaml;
import studio.kuraky.kSCore.configuracion.Comentario;
import studio.kuraky.kSCore.configuracion.Ignorar;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configuración del sistema de chat. Los formatos por permiso ({@code
 * chat.formato.vip}, {@code chat.formato.admin}, ...) se leen del
 * mapa dinámico bajo {@code formatos:} vía el hook {@link
 * #alCargado(Map)}, porque no encajan en la estructura fija del
 * mapeador de configuración.
 */
@Archivo(ruta = "chat.yml")
public final class ConfigChat extends ArchivoYaml {

    @Comentario({"Formato usado cuando el jugador no tiene ningún permiso 'chat.formato.<X>'.",
                 "Placeholders internos: {nombre}, {display}, {mundo}, {mensaje}"})
    public String formatoPorDefecto = "&8[&7{nombre}&8] &f{mensaje}";

    @Comentario("Segundos entre mensajes por jugador (0 = deshabilita el cooldown).")
    public int cooldownSegundos = 2;

    @Comentario("Bloquea repetir el mismo mensaje inmediatamente.")
    public boolean bloquearRepetidos = true;

    @Comentario("Activar el sistema de menciones (@jugador).")
    public boolean menciones = true;

    @Comentario("Sonido reproducido al mencionado. Vacío = sin sonido.")
    public String sonidoMencion = "BLOCK_NOTE_BLOCK_PLING";

    @Comentario("Color aplicado a la mención en el mensaje.")
    public String colorMencion = "&e";

    @Ignorar
    public transient Map<String, String> formatosPorPermiso = new LinkedHashMap<>();

    @Ignorar
    public transient Map<String, DefinicionCanal> canales = new LinkedHashMap<>();

    @Override
    protected void alCargado(Map<String, Object> bruto) {
        formatosPorPermiso.clear();
        canales.clear();
        Object fs = bruto.get("formatos");
        if (fs instanceof Map<?, ?> mp) {
            for (Map.Entry<?, ?> e : mp.entrySet()) {
                formatosPorPermiso.put(String.valueOf(e.getKey()), String.valueOf(e.getValue()));
            }
        }
        Object cs = bruto.get("canales");
        if (cs instanceof Map<?, ?> mp) {
            for (Map.Entry<?, ?> e : mp.entrySet()) {
                if (!(e.getValue() instanceof Map<?, ?> sub)) continue;
                DefinicionCanal def = new DefinicionCanal();
                def.prefijo = valorTexto(sub, "prefijo", "");
                def.permiso = valorTexto(sub, "permiso", "");
                def.color = valorTexto(sub, "color", "&f");
                canales.put(String.valueOf(e.getKey()), def);
            }
        }
    }

    private static String valorTexto(Map<?, ?> mapa, String clave, String defecto) {
        Object v = mapa.get(clave);
        return v == null ? defecto : String.valueOf(v);
    }

    /** Definición cruda de un canal, tal como se leyó del YAML. */
    public static final class DefinicionCanal {
        public String prefijo = "";
        public String permiso = "";
        public String color = "&f";
    }
}
