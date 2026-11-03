package mx.unam.room404escape
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
@RunWith(AndroidJUnit4::class)
class EscapeFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    // Navegación del menú al ranking y regreso.
    @Test fun scoresAndBack() {
        compose.onNodeWithText("High Scores").performScrollTo().performClick()
        compose.onNodeWithText("7890 pts").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("← Menú principal").performScrollTo().performClick()
        compose.onNodeWithText("Jugar").performScrollTo().assertIsDisplayed()
    }
    // Cambiar de concepto actualiza la ilustración y su descripción accesible.
    @Test fun selectEnemyConcept() {
        compose.onNodeWithText("Arte conceptual").performScrollTo().performClick()
        compose.onNodeWithText("La presencia").performScrollTo().performClick()
        compose.onNodeWithContentDescription("La presencia: Una silueta de proporciones extrañas. Su diseño y comportamiento de persecución se desarrollarán en una siguiente etapa.")
            .performScrollTo().assertIsDisplayed()
    }
}
