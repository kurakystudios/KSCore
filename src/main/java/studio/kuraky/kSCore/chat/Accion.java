package studio.kuraky.kSCore.chat;

import net.kyori.adventure.text.event.ClickEvent;

/**
 * Factorías de acciones de clic para componentes de chat. Simple
 * envoltorio sobre {@link ClickEvent} para hacer el código de las
 * llamadas más legible en español.
 */
public final class Accion {

    private Accion() {}

    /** Ejecuta un comando como si el jugador lo hubiera tecleado. */
    public static ClickEvent ejecutar(String comando) {
        return ClickEvent.runCommand(normalizar(comando));
    }

    /** Rellena el chat con un comando (el jugador lo puede editar antes de enviar). */
    public static ClickEvent sugerir(String comando) {
        return ClickEvent.suggestCommand(normalizar(comando));
    }

    /** Copia texto arbitrario al portapapeles del cliente. */
    public static ClickEvent copiar(String texto) {
        return ClickEvent.copyToClipboard(texto);
    }

    /** Abre una URL en el navegador tras confirmación del cliente. */
    public static ClickEvent abrirUrl(String url) {
        return ClickEvent.openUrl(url);
    }

    /** Cambia a otra página del libro (útil en contexto de libros). */
    public static ClickEvent cambiarPagina(int pagina) {
        return ClickEvent.changePage(pagina);
    }

    private static String normalizar(String cmd) {
        return cmd.startsWith("/") ? cmd : "/" + cmd;
    }
}
