
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable

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
    val isPlaying = s.screen == Screen.GAME
    var gameMusicTrack by rememberSaveable(s.screen) {
        mutableStateOf<Int?>(R.raw.musica_menu)
    }

    val musicTrack = if (isPlaying) gameMusicTrack else R.raw.musica_menu

    if (musicTrack != null) {
        val volume = if (musicTrack == R.raw.musica_menu) 0.45f else 0.22f
        BackgroundMusic(track = musicTrack, volume = volume)
    }

    HorrorTheme(s.alternate) {

        BackHandler(
            enabled = s.screen != Screen.MENU &&
                    !s.showIntroduction
        ) {
            vm.navigate(Screen.MENU)
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->

            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(if (isPlaying) 8.dp else 24.dp),
                verticalArrangement = Arrangement.spacedBy(
                    if (isPlaying) 8.dp else 20.dp
                )
            ) {

                if (!isPlaying) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "ARCHIVO / 404",
                            style = MaterialTheme.typography.labelLarge
                        )

                        TextButton(onClick = vm::toggleTheme) {
                            Text(
                                if (s.alternate)
                                    "Paleta: ceniza"
                                else
                                    "Paleta: carmesí"
                            )
                        }
                    }
                }

                when (s.screen) {

                    Screen.GAME -> {
                        GameScreen(
                            onBack = { vm.navigate(Screen.MENU) },
                            onMusicTrackChanged = { gameMusicTrack = it }
                        )
                    }

                    Screen.MENU -> {

                        Text(
                            "NO TODAS LAS PUERTAS\nDEBERÍAN ABRIRSE.",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            "ROOM 404",
                            style = MaterialTheme.typography.headlineLarge
                        )

                        Text(
                            "E S C A P E",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        Illustration(
                            R.drawable.corridor,
                            "Pasillo oscuro hacia la puerta 404, con una silueta al fondo"
                        )

                        Text(
                            "Despertaste en una habitación desconocida. Encuentra lo que falta antes de que la oscuridad te encuentre a ti.",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Button(
                            onClick = {
                                vm.navigate(Screen.GAME)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text("Jugar")
                        }

                        OutlinedButton(
                            onClick = {
                                vm.navigate(Screen.SCORES)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text("High Scores")
                        }

                        OutlinedButton(
                            onClick = {
                                vm.navigate(Screen.ART)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text("Arte conceptual")
                        }


                        OutlinedButton(
                            onClick = {
                                vm.navigate(Screen.CREDITS)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text("Créditos")
                        }


                        Text(
                            "Exploración y supervivencia 2D",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    // PUNTUACIONES REALES DE FIREBASE
                    Screen.SCORES -> {

                        BackToMenu(vm)

                        Text(
                            "LOS QUE ESCAPARON",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            "Puntuaciones",
                            style = MaterialTheme.typography.headlineLarge
                        )

                        Text(
                            "Resultados guardados en Firebase",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        when {

                            // Esperando respuesta de Firebase
                            vm.cargandoPuntuaciones -> {
                                CircularProgressIndicator()

                                Text(
                                    "Cargando puntuaciones..."
                                )
                            }

                            // Error de conexión
                            vm.errorPuntuaciones != null -> {
                                Text(
                                    "No se pudieron cargar las puntuaciones.",
                                    color = MaterialTheme.colorScheme.error
                                )

                                Text(
                                    vm.errorPuntuaciones ?: "",
                                    style = MaterialTheme.typography.bodySmall
                                )

                                Button(
                                    onClick = {
                                        vm.cargarPuntuaciones()
                                    }
                                ) {
                                    Text("Reintentar")
                                }
                            }

                            // Sin registros
                            vm.scores.isEmpty() -> {
                                Text(
                                    "Todavía no hay victorias registradas."
                                )
                            }

                            // Mostrar puntuaciones reales
                            else -> {

                                vm.scores.forEachIndexed { index, score ->

                                    Card(
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Row(
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(20.dp),
                                            horizontalArrangement =
                                                Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                "${index + 1}.  ${score.player}",
                                                fontFamily = FontFamily.Monospace
                                            )

                                            Text(
                                                "${score.points} pts",
                                                fontFamily = FontFamily.Monospace,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        vm.cargarPuntuaciones()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Actualizar puntuaciones")
                                }
                            }
                        }
                    }



                    Screen.CREDITS -> {

                        BackToMenu(vm)

                        val uriHandler = LocalUriHandler.current

                        Text(
                            "ARCHIVO / CRÉDITOS",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            "Créditos",
                            style = MaterialTheme.typography.headlineLarge
                        )

                        Text(
                            "ROOM 404: ESCAPE",
                            style = MaterialTheme.typography.headlineMedium
                        )

                        HorizontalDivider()

                        Text(
                            "DESARROLLO",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            "Videojuego desarrollado como proyecto académico " +
                                    "para las materias de Temas Selectos de Programación" +
                                    "y Técnicas de Programación.",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Text(
                            "EQUIPO DE DESARROLLO",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            "Desarrollado por:\n\n" +
                                    "• Escamilla García Diego Alexei\n" +
                                    "• Nieto Corona Daniel\n" +
                                    "• Rojas Castañeda Adolfo\n" +
                                    "• San Miguel Lara Fernando Elihu\n" +
                                    "• Valdéz Zúñiga Montserrat",
                            style = MaterialTheme.typography.bodyLarge
                        )


                        HorizontalDivider()

                        Text(
                            "MÚSICA",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            "Anywhere Ever Ending Today - Stage 4 - H1 - Living Hell Segment",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            "Por Lexeidgs [Diego Alexei]",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Text(
                            "Cover CandyMan piano",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            "Por Diego Alexei",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        HorizontalDivider()

                        Text(
                            "RECURSOS Y AGRADECIMIENTOS",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            "Los efectos de sonido utilizados en Room 404: Escape " +
                                    "fueron obtenidos de Freesound (freesound.org). " +
                                    "Los siguientes recursos se identifican con licencia " +
                                    "Creative Commons 0 (CC0). Agradecemos a sus " +
                                    "creadores por compartirlos con la comunidad.",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        val sonidos = listOf(
                            Triple(
                                "Picking Up A Small Item",
                                "qubodup",
                                "https://freesound.org/s/822565/"
                            ),
                            Triple(
                                "door open",
                                "saha213131",
                                "https://freesound.org/s/683434/"
                            ),
                            Triple(
                                "Footstep_Run_HardSurface_9.wav",
                                "nicholasdaryl",
                                "https://freesound.org/s/563476/"
                            ),
                            Triple(
                                "Heart Beating 2",
                                "CAT-FOX_ALEX",
                                "https://freesound.org/s/829801/"
                            ),
                            Triple(
                                "Monster scream",
                                "Marvin_Chiriguaya",
                                "https://freesound.org/s/705784/"
                            ),
                            Triple(
                                "jp_tinypadlock01.flac",
                                "JPolito",
                                "https://freesound.org/s/391704/"
                            )
                        )

                        sonidos.forEach { (titulo, autor, enlace) ->

                            Text(
                                titulo,
                                style = MaterialTheme.typography.titleMedium
                            )

                            Text(
                                "Autor: $autor",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Text(
                                "Licencia: Creative Commons 0 (CC0)",
                                style = MaterialTheme.typography.bodySmall
                            )

                            TextButton(
                                onClick = {
                                    uriHandler.openUri(enlace)
                                }
                            ) {
                                Text("Ver recurso en Freesound")
                            }
                        }

                        HorizontalDivider()

                        Text(
                            "Agradecemos a los autores y a la comunidad de " +
                                    "Freesound por facilitar recursos sonoros " +
                                    "para el desarrollo de este proyecto académico.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }



                    Screen.ART -> {

                        BackToMenu(vm)

                        Text(
                            "REGISTROS DE LA HABITACIÓN",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            "Arte conceptual",
                            style = MaterialTheme.typography.headlineLarge
                        )

                        Text(
                            "Bocetos provisionales",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        vm.concepts.forEachIndexed { i, concept ->
                            FilterChip(
                                selected = s.conceptIndex == i,
                                onClick = {
                                    vm.selectConcept(i)
                                },
                                label = {
                                    Text(concept.title)
                                }
                            )
                        }

                        val art = vm.concepts[s.conceptIndex]

                        val resource = when (s.conceptIndex) {
                            1 -> R.drawable.entity2
                            2 -> R.drawable.objects
                            else -> R.drawable.corridor
                        }

                        Illustration(
                            resource,
                            art.title + ": " + art.description
                        )

                        Text(
                            art.title,
                            style = MaterialTheme.typography.headlineMedium
                        )

                        Text(
                            art.description,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                if (!isPlaying) {
                    HorizontalDivider()

                    Text(
                        "ROOM 404: ESCAPE / PROTOTIPO DE INTERFAZ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Composable
private fun BackToMenu(vm: EscapeViewModel) {
    TextButton(
        onClick = {
            vm.navigate(Screen.MENU)
        }
    ) {
        Text("← Menú principal")
    }
}

@Composable
private fun Illustration(
    resource: Int,
    description: String
) {
    Card(
        shape = RoundedCornerShape(4.dp)
    ) {
        Image(
            painter = painterResource(resource),
            contentDescription = description,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(400f / 240f)
        )
    }
}
