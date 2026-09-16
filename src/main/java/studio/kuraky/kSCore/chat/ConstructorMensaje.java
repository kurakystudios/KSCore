package studio.kuraky.kSCore.chat;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.HoverEventSource;
import org.bukkit.inventory.ItemStack;
import studio.kuraky.kSCore.mensajes.Mensajes;

/**
 * Builder fluído para mensajes de chat interactivos.
 * <pre>{@code
 * Chat.construir()
 *     .texto("&7Haz clic ")
 *     .boton("&a[Aceptar]", Accion.ejecutar("/duelo aceptar"), Hover.texto("&7Acepta"))
 *     .enviar(jugador);
 * }</pre>
 */
public final class ConstructorMensaje {

    private Component acumulado = Component.empty();

    ConstructorMensaje() {}

    public ConstructorMensaje texto(String texto) {
        acumulado = acumulado.append(Mensajes.parsear(texto));
        return this;
    }

    public ConstructorMensaje texto(Component componente) {
        acumulado = acumulado.append(componente);
        return this;
    }

    public ConstructorMensaje boton(String texto, ClickEvent clic) {
        acumulado = acumulado.append(Mensajes.parsear(texto).clickEvent(clic));
        return this;
    }

    public ConstructorMensaje boton(String texto, ClickEvent clic, HoverEvent<?> hover) {
        acumulado = acumulado.append(
                Mensajes.parsear(texto).clickEvent(clic).hoverEvent(hover));
        return this;
    }

    public ConstructorMensaje boton(String texto, ClickEvent clic, HoverEventSource<?> fuenteHover) {
        acumulado = acumulado.append(
                Mensajes.parsear(texto).clickEvent(clic).hoverEvent(fuenteHover));
        return this;
    }

    public ConstructorMensaje enlace(String texto, String url) {
        return boton(texto, Accion.abrirUrl(url), Hover.texto("&7" + url));
    }

    /** Muestra el item completo al pasar el ratón (sin acción de clic). */
    public ConstructorMensaje item(String texto, ItemStack item) {
        acumulado = acumulado.append(Mensajes.parsear(texto).hoverEvent(item));
        return this;
    }

    /** Botón que copia texto arbitrario al portapapeles. */
    public ConstructorMensaje copiar(String texto, String contenido) {
        return boton(texto, Accion.copiar(contenido), Hover.texto("&7Copia al portapapeles"));
    }

    public ConstructorMensaje saltoLinea() {
        acumulado = acumulado.append(Component.newline());
        return this;
    }

    public Component construir() {
        return acumulado;
    }

    public void enviar(Audience destino) {
        destino.sendMessage(acumulado);
    }
}
