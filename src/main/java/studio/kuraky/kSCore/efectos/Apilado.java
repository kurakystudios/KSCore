package studio.kuraky.kSCore.efectos;

/** Estrategia al aplicar un efecto que ya está activo sobre el mismo jugador. */
public enum Apilado {

    /** Sustituye el efecto anterior por el nuevo (nivel y duración del nuevo). */
    REEMPLAZAR,

    /** Se queda con el mayor de los niveles y la mayor duración. */
    MAYOR_NIVEL,

    /** Suma la duración; el nivel se mantiene si el existente es mayor. */
    SUMAR_TIEMPO
}
