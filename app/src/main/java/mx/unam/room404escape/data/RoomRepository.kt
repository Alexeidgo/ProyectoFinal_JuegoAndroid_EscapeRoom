package mx.unam.room404escape.data

data class Score(val player: String, val points: Int)
data class Concept(val title: String, val description: String)

class RoomRepository {
    // Datos ficticios para presentar la interfaz, sin conexión a Firebase.
    val scores = listOf(Score("DAG", 3211), Score("AAA", 7890), Score("DLS", 7500), Score("MRK", 7120))
    val concepts = listOf(
        Concept("La habitación", "Puerta 404, paredes deterioradas y una luz que apenas alcanza el pasillo. El escenario final será un mapa 2D explorable."),
        Concept("La presencia", "Una silueta de proporciones extrañas. Su diseño y comportamiento de persecución se desarrollarán en una siguiente etapa."),
        Concept("Los objetos", "Llaves y objetos ocultos que el jugador deberá reunir. Al completar la colección comenzará la persecución final.")
    )
}

class RankScores {
    operator fun invoke(scores: List<Score>): List<Score> =
        scores.sortedWith(compareByDescending<Score> { it.points }.thenBy { it.player })
}
