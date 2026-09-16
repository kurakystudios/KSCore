/**
 * <h2>API pública estable de KSCore</h2>
 *
 * Este es el <b>único</b> paquete cuya superficie se compromete estable.
 * Todo lo demás (paquetes internos {@code nucleo}, {@code comandos},
 * {@code datos}, {@code eventos}, {@code items}, {@code guis},
 * {@code efectos}, {@code chat}, {@code configuracion}, {@code mensajes},
 * {@code utilidades}) puede cambiar sin previo aviso entre versiones.
 *
 * <p>La forma recomendada de acceder a los sistemas del núcleo es a
 * través de {@link studio.kuraky.kSCore.api.Api}, que expone un método
 * por cada subsistema y evita depender directamente de las clases de
 * implementación:
 * <pre>{@code
 * studio.kuraky.kSCore.api.Api.mensajes().enviar(jugador, "&aHola");
 * studio.kuraky.kSCore.api.Api.comandos().registrar(new MiComando());
 * }</pre>
 *
 * <p>Alternativamente, las fachadas de cada subsistema
 * ({@code Mensajes}, {@code Comandos}, {@code Eventos},
 * {@code Archivos}, {@code Datos}, {@code Items}, {@code Efectos},
 * {@code Guis}, {@code Chat}) siguen siendo accesibles por su nombre
 * simple: son parte del contrato público y no se moverán.
 *
 * <h3>Anotaciones estables</h3>
 * Las anotaciones {@code @Comando}, {@code @Sub}, {@code @Arg},
 * {@code @Completar}, {@code @Cooldown}, {@code @Escuchador},
 * {@code @Evento}, {@code @Archivo}, {@code @Seccion},
 * {@code @Comentario}, {@code @Clave}, {@code @Tabla}, {@code @Id},
 * {@code @Columna}, {@code @Item}, {@code @Menu} y {@code @Efecto} son
 * parte de la API pública.
 */
package studio.kuraky.kSCore.api;
