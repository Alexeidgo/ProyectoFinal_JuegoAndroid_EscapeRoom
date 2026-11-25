
package mx.unam.room404escape.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PartidasFirebaseRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Obtener las 10 mejores partidas ganadas
    fun obtenerPartidas(
        onSuccess: (List<Partida>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("partidas")
            .whereEqualTo("resultado", "victoria")
            .get()
            .addOnSuccessListener { documentos ->

                val partidas = documentos
                    .map { it.toObject(Partida::class.java) }
                    .sortedByDescending { it.puntuacion }
                    .take(10)

                onSuccess(partidas)
            }
            .addOnFailureListener { error ->
                onError(error)
            }
    }

    // Guardar una partida terminada
    fun guardarPartida(
        partida: Partida,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        fun guardarEnFirestore() {
            db.collection("partidas")
                .add(partida)
                .addOnSuccessListener {
                    onSuccess()
                }
                .addOnFailureListener { error ->
                    onError(error)
                }
        }

        if (auth.currentUser != null) {
            guardarEnFirestore()
        } else {
            auth.signInAnonymously()
                .addOnSuccessListener {
                    guardarEnFirestore()
                }
                .addOnFailureListener { error ->
                    onError(error)
                }
        }
    }
}
