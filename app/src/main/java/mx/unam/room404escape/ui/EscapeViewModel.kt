
package mx.unam.room404escape.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

import mx.unam.room404escape.data.*
import com.google.firebase.auth.FirebaseAuth

enum class Screen {
    MENU, SCORES, ART, GAME, CREDITS
}

data class EscapeUiState(
    val screen: Screen = Screen.MENU,
    val alternate: Boolean = false,
    val conceptIndex: Int = 0,
    val showIntroduction: Boolean = false
)

class EscapeViewModel(
    private val saved: SavedStateHandle
) : ViewModel() {

    // Repositorio original: arte conceptual
    private val repository = RoomRepository()

    val concepts = repository.concepts

    // Nuevo repositorio de Firebase
    private val firebaseRepository = PartidasFirebaseRepository()
    private val auth = FirebaseAuth.getInstance()

    // Puntuaciones obtenidas desde Firebase
    var scores by mutableStateOf<List<Score>>(emptyList())
        private set

    // Estado de la consulta
    var cargandoPuntuaciones by mutableStateOf(false)
        private set

    var errorPuntuaciones by mutableStateOf<String?>(null)
        private set

    // Estado original de la interfaz
    var state by mutableStateOf(
        EscapeUiState(
            screen = Screen.valueOf(saved["screen"] ?: "MENU"),
            alternate = saved["alternate"] ?: false,
            conceptIndex = saved["concept"] ?: 0,
            showIntroduction = saved["intro"] ?: false
        )
    )
        private set

    private fun update(next: EscapeUiState) {
        state = next

        saved["screen"] = next.screen.name
        saved["alternate"] = next.alternate
        saved["concept"] = next.conceptIndex
        saved["intro"] = next.showIntroduction
    }

    // Navegación
    fun navigate(screen: Screen) {
        update(
            state.copy(
                screen = screen,
                showIntroduction = false
            )
        )

        // Consultar Firebase al abrir High Scores
        if (screen == Screen.SCORES) {
            cargarPuntuaciones()
        }
    }

    // Obtener las puntuaciones reales
    fun cargarPuntuaciones() {
        cargandoPuntuaciones = true
        errorPuntuaciones = null

        if (auth.currentUser != null) {
            consultarPartidas()
        } else {
            auth.signInAnonymously()
                .addOnSuccessListener {
                    consultarPartidas()
                }
                .addOnFailureListener { error ->
                    cargandoPuntuaciones = false
                    errorPuntuaciones =
                        error.localizedMessage
                            ?: "Error de autenticación"
                }
        }
    }

    // Consultar la colección partidas
    private fun consultarPartidas() {
        firebaseRepository.obtenerPartidas(
            onSuccess = { partidas ->

                // Convertimos las partidas al modelo
                // utilizado actualmente por High Scores
                scores = partidas.map { partida ->
                    Score(
                        player = partida.jugador,
                        points = partida.puntuacion
                            .coerceIn(
                                0L,
                                Int.MAX_VALUE.toLong()
                            )
                            .toInt()
                    )
                }

                cargandoPuntuaciones = false
                errorPuntuaciones = null
            },
            onError = { error ->
                scores = emptyList()
                cargandoPuntuaciones = false
                errorPuntuaciones =
                    error.localizedMessage
                        ?: "No se pudieron cargar las partidas"
            }
        )
    }

    // Cambiar paleta
    fun toggleTheme() {
        update(
            state.copy(
                alternate = !state.alternate
            )
        )
    }

    // Seleccionar arte conceptual
    fun selectConcept(index: Int) {
        if (index in concepts.indices) {
            update(
                state.copy(
                    conceptIndex = index
                )
            )
        }
    }

    // Mostrar introducción
    fun introduction(show: Boolean) {
        update(
            state.copy(
                showIntroduction = show
            )
        )
    }
}
