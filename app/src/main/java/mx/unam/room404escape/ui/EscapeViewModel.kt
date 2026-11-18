package mx.unam.room404escape.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import mx.unam.room404escape.data.*

enum class Screen { MENU, SCORES, ART , GAME}
data class EscapeUiState(
    val screen: Screen = Screen.MENU,
    val alternate: Boolean = false,
    val conceptIndex: Int = 0,
    val showIntroduction: Boolean = false
)
class EscapeViewModel(private val saved: SavedStateHandle) : ViewModel() {
    private val repository = RoomRepository()
    val scores = RankScores()(repository.scores)
    val concepts = repository.concepts
    var state by mutableStateOf(EscapeUiState(
        screen = Screen.valueOf(saved["screen"] ?: "MENU"),
        alternate = saved["alternate"] ?: false,
        conceptIndex = saved["concept"] ?: 0,
        showIntroduction = saved["intro"] ?: false
    ))
        private set
    private fun update(next: EscapeUiState) {
        state = next
        saved["screen"] = next.screen.name
        saved["alternate"] = next.alternate
        saved["concept"] = next.conceptIndex
        saved["intro"] = next.showIntroduction
    }
    fun navigate(screen: Screen) = update(state.copy(screen = screen, showIntroduction = false))
    fun toggleTheme() = update(state.copy(alternate = !state.alternate))
    fun selectConcept(index: Int) {
        if (index in concepts.indices) update(state.copy(conceptIndex = index))
    }
    fun introduction(show: Boolean) = update(state.copy(showIntroduction = show))
}
