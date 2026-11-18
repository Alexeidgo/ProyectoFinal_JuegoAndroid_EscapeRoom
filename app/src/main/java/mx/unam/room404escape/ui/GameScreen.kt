package mx.unam.room404escape.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

//Colisiones Y lógica Juego
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp

//MovimientoFluido
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
//Cruceta
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
//Manejar pisos
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf


private const val HOUSE_WIDTH = 12f
private const val HOUSE_HEIGHT = 17.5f
private const val MAP_Y_SCALE = HOUSE_HEIGHT / 14f
private val entrance = Offset(6f, 12.5f * MAP_Y_SCALE)

private data class Wall(val start: Offset, val end: Offset)

private fun wall(x1: Float, y1: Float, x2: Float, y2: Float) =
    Wall(
        Offset(x1, y1 * MAP_Y_SCALE),
        Offset(x2, y2 * MAP_Y_SCALE)
    )
// Los tramos separados dejan puertas de dos unidades hacia el pasillo.
//Piso1
private val walls = listOf(
    wall(0.5f, 0.5f, 11.5f, 0.5f),
    wall(0.5f, 0.5f, 0.5f, 13.5f),
    wall(11.5f, 0.5f, 11.5f, 13.5f),
    wall(0.5f, 13.5f, 11.5f, 13.5f),
    wall(0.5f, 7f, 4.5f, 7f),
    wall(7.5f, 7f, 11.5f, 7f),
    wall(4.5f, 0.5f, 4.5f, 3f),
    wall(4.5f, 5f, 4.5f, 9f),
    wall(4.5f, 11f, 4.5f, 13.5f),
    wall(7.5f, 0.5f, 7.5f, 3f),
    wall(7.5f, 5f, 7.5f, 9f),
    wall(7.5f, 11f, 7.5f, 13.5f)
)
//Piso2
private val floorTwoWalls = walls.take(4) + listOf(
    wall(0.5f, 7f, 5f, 7f),
    wall(7f, 7f, 11.5f, 7f)
)

private val flashlight = Offset(2.5f, 11f * MAP_Y_SCALE)
private val stairsUp = Offset(6f, 1.1f * MAP_Y_SCALE)
private val stairsDown = Offset(6f, 12.9f * MAP_Y_SCALE)

private fun isNear(position: Offset, target: Offset, radius: Float): Boolean {
    val dx = position.x - target.x
    val dy = position.y - target.y
    return dx * dx + dy * dy <= radius * radius
}
//Fin Piso 2
private const val PLAYER_RADIUS = 0.3f
private const val WALL_THICKNESS = 0.13f

//Velocidad del protagonista
private const val MOVE_STEP = 0.12f

private fun hitsWall(position: Offset, activeWalls: List<Wall>): Boolean {
    val clearance = PLAYER_RADIUS + WALL_THICKNESS / 2f

    return activeWalls.any { wall ->
        val nearestX = position.x.coerceIn(
            minOf(wall.start.x, wall.end.x),
            maxOf(wall.start.x, wall.end.x)
        )
        val nearestY = position.y.coerceIn(
            minOf(wall.start.y, wall.end.y),
            maxOf(wall.start.y, wall.end.y)
        )
        val dx = position.x - nearestX
        val dy = position.y - nearestY
        dx * dx + dy * dy < clearance * clearance
    }
}

@Composable
fun GameScreen(onBack: () -> Unit) {
    var playerX by rememberSaveable { mutableFloatStateOf(entrance.x) }
    var playerY by rememberSaveable { mutableFloatStateOf(entrance.y) }

    var floor by rememberSaveable { mutableIntStateOf(1) }
    var hasFlashlight by rememberSaveable { mutableStateOf(false) }

    fun move(dx: Float, dy: Float) {
        val next = Offset(playerX + dx, playerY + dy)
        val activeWalls = if (floor == 1) walls else floorTwoWalls

        if (hitsWall(next, activeWalls)) return

        playerX = next.x
        playerY = next.y

        if (floor == 1 && isNear(next, flashlight, 0.55f)) {
            hasFlashlight = true
        }

        when {
            floor == 1 && dy < 0f && isNear(next, stairsUp, 0.55f) -> {
                floor = 2
                playerX = stairsDown.x
                playerY = stairsDown.y - 1f
            }
            floor == 2 && dy > 0f && isNear(next, stairsDown, 0.55f) -> {
                floor = 1
                playerX = stairsUp.x
                playerY = stairsUp.y + 1f
            }
        }
    }

    TextButton(onClick = onBack) { Text("← Menú principal") }
    Text("Piso$floor", style = MaterialTheme.typography.headlineMedium)
    Text("Objetos: ${if (hasFlashlight) 1 else 0}/8", style = MaterialTheme.typography.labelLarge)
    val playerColor = MaterialTheme.colorScheme.primary

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(HOUSE_WIDTH / HOUSE_HEIGHT)
            .semantics { contentDescription = "Plano de la casa con cuatro habitaciones y un pasillo" }
    )

    {
        val unit = size.width / HOUSE_WIDTH
        drawRect(Color(0xFF221B20))
        val activeWalls = if (floor == 1) walls else floorTwoWalls
        activeWalls.forEach { segment ->
            drawLine(
                color = Color(0xFFA08E91),
                start = segment.start * unit,
                end = segment.end * unit,
                strokeWidth = WALL_THICKNESS * unit
            )
        }
        //Escaleras
        val stairs = if (floor == 1) stairsUp else stairsDown

        drawCircle(
            color = Color(0xFF7BC4AC),
            radius = 0.32f * unit,
            center = stairs * unit
        )

        if (floor == 1 && !hasFlashlight) {
            drawCircle(
                color = Color(0xFFFFD166),
                radius = 0.22f * unit,
                center = flashlight * unit
            )
        }
        drawCircle(
            color = playerColor,
            radius = PLAYER_RADIUS * unit,
            center = Offset(playerX, playerY) * unit
        )
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        HoldDirection("↑", "Arriba") { move(0f, -MOVE_STEP) }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            HoldDirection("←", "Izquierda") { move(-MOVE_STEP, 0f) }
            Spacer(Modifier.size(48.dp))
            HoldDirection("→", "Derecha") { move(MOVE_STEP, 0f) }
        }

        HoldDirection("↓", "Abajo") { move(0f, MOVE_STEP) }
    }
}
@Composable
private fun HoldDirection(
    arrow: String,
    label: String,
    onMove: () -> Unit
) {
    val moveNow by rememberUpdatedState(onMove)

    Surface(
        modifier = Modifier
            .size(48.dp)
            .semantics {
                contentDescription = label
                role = Role.Button
                onClick {
                    moveNow()
                    true
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        coroutineScope {
                            moveNow() // Primer paso al tocar.
                            val repeatMovement = launch {
                                delay(120L)
                                while (true) {
                                    moveNow()
                                    delay(32L)
                                }
                            }

                            try {
                                tryAwaitRelease()
                            } finally {
                                repeatMovement.cancel()
                            }
                        }
                    }
                )
            },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(arrow)
        }
    }
}
