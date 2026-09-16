package studio.kuraky.kSCore.nucleo;

import studio.kuraky.kSCore.chat.Chat;
import studio.kuraky.kSCore.comandos.Arg;
import studio.kuraky.kSCore.comandos.Comando;
import studio.kuraky.kSCore.comandos.Comandos;
import studio.kuraky.kSCore.comandos.Completar;
import studio.kuraky.kSCore.comandos.Contexto;
import studio.kuraky.kSCore.comandos.DescriptorMetodo;
import studio.kuraky.kSCore.comandos.Principal;
import studio.kuraky.kSCore.comandos.Sub;
import studio.kuraky.kSCore.configuracion.ArchivoBase;
import studio.kuraky.kSCore.configuracion.Archivos;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Comando(nombre = "core", permiso = "kscore.admin",
        descripcion = "Utilidades administrativas del núcleo KSCore.")
public final class ComandoCore {

    @Principal
    public void raiz(Contexto ctx) {
        ctx.enviarDe("core.uso");
    }

    @Sub(nombre = "recargar", permiso = "kscore.admin.recargar")
    public void recargar(Contexto ctx, @Arg(opcional = true, defecto = "") String archivo) {
        if (archivo == null || archivo.isBlank()) {
            Archivos.recargarTodo();
            ctx.enviarDe("core.recargado");
            return;
        }
        for (ArchivoBase a : Archivos.registrados()) {
            if (a.rutaRelativa().equalsIgnoreCase(archivo)) {
                a.recargar();
                ctx.enviarDe("core.recargado");
                return;
            }
        }
        ctx.enviarDe("core.archivo-desconocido", Map.of("archivo", archivo));
    }

    @Completar(sub = "recargar", arg = "archivo")
    @SuppressWarnings("unused")
    public List<String> completarArchivos(Contexto ctx, String parcial) {
        String p = parcial.toLowerCase(Locale.ROOT);
        return Archivos.registrados().stream()
                .map(ArchivoBase::rutaRelativa)
                .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(p))
                .toList();
    }

    @Sub(nombre = "debug", permiso = "kscore.admin.debug")
    public void debug(Contexto ctx) {
        Depurador d = Nucleo.actual().depurador();
        boolean nuevo = !d.activo();
        d.activar(nuevo);
        if (Archivos.estaRegistrado(ConfigNucleo.class)) {
            ConfigNucleo cfg = Archivos.obtener(ConfigNucleo.class);
            cfg.debug = nuevo;
            cfg.guardar();
        }
        ctx.enviarDe(nuevo ? "core.debug-activado" : "core.debug-desactivado");
    }

    @Sub(nombre = "info", permiso = "kscore.admin.info")
    public void info(Contexto ctx) {
        Nucleo n = Nucleo.actual();
        Runtime rt = Runtime.getRuntime();
        long usadaMb = (rt.totalMemory() - rt.freeMemory()) / (1024L * 1024L);
        long totalMb = rt.totalMemory() / (1024L * 1024L);
        ctx.enviarDe("core.info-cabecera");
        ctx.enviarDe("core.info-linea", Map.of("clave", "RAM", "valor", usadaMb + " / " + totalMb + " MB"));
        ctx.enviarDe("core.info-linea", Map.of("clave", "Hilos JVM", "valor", String.valueOf(Thread.activeCount())));
        ctx.enviarDe("core.info-linea", Map.of("clave", "Comandos", "valor", String.valueOf(Comandos.descriptores().size())));
        ctx.enviarDe("core.info-linea", Map.of("clave", "Archivos", "valor", String.valueOf(Archivos.registrados().size())));
        ctx.enviarDe("core.info-linea", Map.of("clave", "Listeners",
                "valor", String.valueOf(n.eventos().cantidadRegistrados())));
        ctx.enviarDe("core.info-linea", Map.of("clave", "Menús",
                "valor", String.valueOf(n.guis().nombresRegistrados().size())));
        ctx.enviarDe("core.info-linea", Map.of("clave", "Efectos activos",
                "valor", String.valueOf(n.efectos().cantidadActivos())));
        try {
            ctx.enviarDe("core.info-linea", Map.of("clave", "Canales chat",
                    "valor", String.valueOf(Chat.canales().size())));
        } catch (IllegalStateException ignored) {}
        ctx.enviarDe("core.info-linea", Map.of("clave", "Debug",
                "valor", n.depurador().activo() ? "activo" : "inactivo"));
    }

    @Sub(nombre = "comandos", permiso = "kscore.admin.info")
    public void listaComandos(Contexto ctx) {
        List<DescriptorMetodo> descriptores = Comandos.descriptores();
        ctx.enviarDe("core.info-cabecera");
        for (DescriptorMetodo m : descriptores) {
            String clase = m.metodo().getDeclaringClass().getSimpleName();
            ctx.enviarDe("core.info-linea", Map.of(
                    "clave", clase,
                    "valor", m.rutaLegible()));
        }
    }
}
