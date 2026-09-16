package studio.kuraky.kSCore.efectos;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Base de un efecto personalizado. Los métodos son opcionales; sólo
 * hay que sobreescribir los relevantes para la lógica.
 */
public abstract class EfectoPersonalizado {

    /** Se invoca una vez al aplicar el efecto sobre el jugador. */
    public void alAplicar(Player jugador, int nivel) {}

    /**
     * Se invoca periódicamente según {@code @Efecto.intervaloTicks}
     * mientras el efecto esté activo.
     */
    public void cadaTick(Player jugador, int nivel, long ticksRestantes) {}

    /** Se invoca cuando el efecto termina (agotado, retirado o desconexión). */
    public void alExpirar(Player jugador) {}

    /** Se invoca si el jugador golpea a otra entidad mientras tiene este efecto. */
    public void alGolpear(Player jugador, LivingEntity objetivo, EntityDamageByEntityEvent evento) {}
}
