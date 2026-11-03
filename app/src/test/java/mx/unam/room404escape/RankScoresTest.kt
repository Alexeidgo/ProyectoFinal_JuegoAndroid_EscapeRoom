package mx.unam.room404escape
import mx.unam.room404escape.data.*
import org.junit.Assert.assertEquals
import org.junit.Test
class RankScoresTest {
    // El ranking muestra primero el mayor puntaje sin modificar la lista de origen.
    @Test fun descendingRanking() {
        val original = listOf(Score("A", 100), Score("B", 300), Score("C", 200))
        assertEquals(listOf("B", "C", "A"), RankScores()(original).map { it.player })
        assertEquals("A", original.first().player)
    }
    // Empates deterministas y lista vacía sin excepciones.
    @Test fun tiesAndEmptyRanking() {
        assertEquals(emptyList<Score>(), RankScores()(emptyList()))
        assertEquals(listOf("AAA", "ZZZ"), RankScores()(listOf(Score("ZZZ", 50), Score("AAA", 50))).map { it.player })
    }
}
