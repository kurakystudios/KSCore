package studio.kuraky.kSCore.nucleo;

import studio.kuraky.kSCore.configuracion.Archivo;
import studio.kuraky.kSCore.configuracion.ArchivoYaml;
import studio.kuraky.kSCore.configuracion.Comentario;
import studio.kuraky.kSCore.configuracion.Seccion;

/**
 * Configuración general del núcleo. Habilita el modo debug y contiene
 * la sección de base de datos que consume {@code ModuloDatos}.
 */
@Archivo(ruta = "config.yml", copiarRecurso = false)
public final class ConfigNucleo extends ArchivoYaml {

    @Comentario({"Modo de depuración: muestra tiempos de ejecución de comandos,",
                 "cargas de archivos y stacktraces completos ante errores."})
    public boolean debug = false;

    @Seccion("base-datos")
    @Comentario("Conexión de base de datos usada por el sistema de datos.")
    public BaseDatos baseDatos = new BaseDatos();

    public static final class BaseDatos {
        @Comentario("Motor: sqlite | mariadb")
        public String tipo = "sqlite";

        @Comentario("Fichero SQLite (relativo a la carpeta del plugin). Ignorado si tipo != sqlite.")
        public String archivo = "datos.db";

        @Comentario("Host del servidor SQL. Ignorado si tipo == sqlite.")
        public String host = "localhost";
        public int puerto = 3306;
        public String usuario = "root";
        public String clave = "";

        @Comentario("Nombre de la base de datos / esquema.")
        public String nombre = "kscore";
    }
}
