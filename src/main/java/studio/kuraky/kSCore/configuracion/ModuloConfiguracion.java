package studio.kuraky.kSCore.configuracion;

import studio.kuraky.kSCore.nucleo.Modulo;
import studio.kuraky.kSCore.nucleo.Nucleo;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

/**
 * Módulo de arranque para el sistema de configuración. Se registra el
 * primero en el {@link Nucleo} y descubre automáticamente todas las
 * clases anotadas con {@link Archivo}, instanciándolas mediante su
 * constructor sin argumentos.
 */
public final class ModuloConfiguracion implements Modulo {

    private Nucleo nucleo;

    @Override
    public void iniciar(Nucleo nucleo) {
        this.nucleo = nucleo;
        File carpeta = nucleo.plugin().getDataFolder();
        if (!carpeta.exists() && !carpeta.mkdirs()) {
            nucleo.registro().aviso("No se pudo crear la carpeta de datos del plugin.");
        }
        Archivos.inicializar(carpeta.toPath(), nucleo.tareas(), nucleo.registro(),
                nucleo.plugin().getClass().getClassLoader());

        for (Class<?> clase : nucleo.escaner().conAnotacion(Archivo.class)) {
            if (!ArchivoBase.class.isAssignableFrom(clase)) {
                nucleo.registro().aviso("La clase " + clase.getName()
                        + " tiene @Archivo pero no extiende ArchivoBase; se ignora.");
                continue;
            }
            if (Modifier.isAbstract(clase.getModifiers())) continue;
            try {
                @SuppressWarnings("unchecked")
                Constructor<? extends ArchivoBase> ctor =
                        (Constructor<? extends ArchivoBase>) clase.getDeclaredConstructor();
                ctor.setAccessible(true);
                ArchivoBase archivo = ctor.newInstance();
                Archivos.registrar(archivo);
                archivo.cargar();
                nucleo.depurador().log("Archivos", () -> "Cargado " + archivo.rutaRelativa());
            } catch (NoSuchMethodException e) {
                nucleo.registro().error("La clase " + clase.getName()
                        + " necesita un constructor sin argumentos para ser cargada como @Archivo.");
            } catch (ReflectiveOperationException e) {
                nucleo.registro().error("No se pudo instanciar " + clase.getName(), e);
            }
        }
    }

    @Override
    public void detener() {
        Archivos.desinicializar();
        this.nucleo = null;
    }
}
