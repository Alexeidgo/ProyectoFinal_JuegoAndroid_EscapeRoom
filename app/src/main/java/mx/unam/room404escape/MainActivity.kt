package mx.unam.room404escape

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import mx.unam.room404escape.ui.*


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { EscapeApp() }
    }
}
@Composable
fun EscapeApp(vm: EscapeViewModel = viewModel()) {
    val s = vm.state
    HorrorTheme(s.alternate) {
        BackHandler(enabled = s.screen != Screen.MENU && !s.showIntroduction) { vm.navigate(Screen.MENU) }
        Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("ARCHIVO / 404", style = MaterialTheme.typography.labelLarge)
                    TextButton(onClick = vm::toggleTheme) { Text(if (s.alternate) "Paleta: ceniza" else "Paleta: carmesí") }
                }
                when(s.screen) {
                    Screen.MENU -> {
                        Text("NO TODAS LAS PUERTAS\nDEBERÍAN ABRIRSE.", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text("ROOM 404", style = MaterialTheme.typography.headlineLarge)
                        Text("E S C A P E", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                        Illustration(R.drawable.corridor, "Pasillo oscuro hacia la puerta 404, con una silueta al fondo")
                        Text("Despertaste en una habitación desconocida. Encuentra lo que falta antes de que la oscuridad te encuentre a ti.", style = MaterialTheme.typography.bodyLarge)
                        Button(onClick = { vm.introduction(true) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), shape = RoundedCornerShape(4.dp)) { Text("Jugar") }
                        OutlinedButton(onClick = { vm.navigate(Screen.SCORES) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(4.dp)) { Text("High Scores") }
                        OutlinedButton(onClick = { vm.navigate(Screen.ART) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(4.dp)) { Text("Arte conceptual") }
                        Text("Exploración y supervivencia 2D", style = MaterialTheme.typography.bodySmall)
                    }
                    Screen.SCORES -> {
                        BackToMenu(vm)
                        Text("LOS QUE ESCAPARON", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text("High Scores", style = MaterialTheme.typography.headlineLarge)
                        Text("Datos de demostración · Sin conexión a Firebase", style = MaterialTheme.typography.bodyMedium)
                        vm.scores.forEachIndexed { index, score ->
                            Card(shape = RoundedCornerShape(4.dp)) {
                                Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${index + 1}.  ${score.player}", fontFamily = FontFamily.Monospace)
                                    Text("${score.points} pts", fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                        Text("En una siguiente etapa se mostrarán los resultados de los jugadores guardados en Firebase.", style = MaterialTheme.typography.bodyLarge)
                    }
                    Screen.ART -> {
                        BackToMenu(vm)
                        Text("REGISTROS DE LA HABITACIÓN", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text("Arte conceptual", style = MaterialTheme.typography.headlineLarge)
                        Text("Bocetos provisionales", style = MaterialTheme.typography.bodyMedium)
                        vm.concepts.forEachIndexed { i, concept ->
                            FilterChip(selected = s.conceptIndex == i, onClick = { vm.selectConcept(i) }, label = { Text(concept.title) })
                        }
                        val art = vm.concepts[s.conceptIndex]
                        val resource = when(s.conceptIndex) { 1 -> R.drawable.entity2; 2 -> R.drawable.objects; else -> R.drawable.corridor }
                        Illustration(resource, art.title + ": " + art.description)
                        Text(art.title, style = MaterialTheme.typography.headlineMedium)
                        Text(art.description, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                HorizontalDivider()
                Text("ROOM 404: ESCAPE / PROTOTIPO DE INTERFAZ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            }
        }
        if(s.showIntroduction) AlertDialog(
            onDismissRequest = { vm.introduction(false) },
            title = { Text("La puerta aún está cerrada") },
            text = { Text("Explora un escenario 2D, reúne objetos ocultos y evita a la presencia. Al reunirlos todos, encuentra la salida durante la persecución final.\n\nEsta entrega presenta la interfaz. El movimiento, los objetos y el enemigo se implementarán después.") },
            confirmButton = { TextButton(onClick = { vm.introduction(false) }) { Text("Volver al menú") } }
        )
    }
}
@Composable
private fun BackToMenu(vm: EscapeViewModel) {
    TextButton(onClick = { vm.navigate(Screen.MENU) }) { Text("← Menú principal") }
}
@Composable
private fun Illustration(resource: Int, description: String) {
    Card(shape = RoundedCornerShape(4.dp)) {
        Image(painterResource(resource), contentDescription = description, contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth().aspectRatio(400f / 240f))
    }
}
