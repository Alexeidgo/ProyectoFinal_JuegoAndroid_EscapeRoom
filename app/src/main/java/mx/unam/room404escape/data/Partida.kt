package mx.unam.room404escape.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class Partida(
    var jugador: String = "",
    var puntuacion: Long = 0L,
    var tiempoSegundos: Long = 0L,
    var objetos: Long = 0L,
    var resultado: String = "",
    @get:ServerTimestamp
    var fecha: Timestamp? = null
)
