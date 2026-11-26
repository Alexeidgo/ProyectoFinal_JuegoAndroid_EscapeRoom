package mx.unam.room404escape.ui

import android.content.Context
import android.os.SystemClock
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import kotlinx.coroutines.delay
import java.util.Locale
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import android.view.ViewTreeObserver
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.unit.dp
import android.util.Log
import mx.unam.room404escape.data.Partida
import mx.unam.room404escape.data.PartidasFirebaseRepository
import mx.unam.room404escape.R
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipRect
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.media.MediaPlayer
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clipToBounds

private const val MAP_WIDTH = 12f
private const val MAP_HEIGHT = 17.5f
private const val Y_SCALE = MAP_HEIGHT / 14f
private const val PLAYER_RADIUS = 0.3f
private const val WALL_THICKNESS = 0.13f
private const val MOVE_STEP = 0.12f
// Equivale a los 0.12 del control anterior cada 32 ms.
private const val MOVE_SPEED = MOVE_STEP / 0.032f
private const val JOYSTICK_DEAD_ZONE = 0.18f
private const val PORTAL_RADIUS = 0.62f
private const val REARM_DISTANCE = 1.2f

private enum class Room(val title: String, val floor: Int = 1) {
    MAIN("Sala principal"),
    LEFT("Vestíbulo izquierdo"),
    RIGHT("Vestíbulo derecho"),
    UPPER("Salón superior"),
    PASSAGE_1("Pasadizo 1"),
    PASSAGE_2("Pasadizo 2"),
    REAR("Salón trasero"),
    LIVING("Sala de estar", 2),
    ATTIC("Ático", 2),
    SHED("Cobertizo", 2),
    BEDROOM("Dormitorio", 2),
    FINAL_LOWER("Corredor final · tramo 1", 2),
    FINAL_UPPER("Corredor final · tramo 2", 2)
}

private data class Wall(val start: Offset, val end: Offset)

private fun point(x: Float, y: Float) = Offset(x, y * Y_SCALE)

private fun wall(x1: Float, y1: Float, x2: Float, y2: Float) =
    Wall(point(x1, y1), point(x2, y2))

private val entrance = point(6f, 12.5f)
private val flashlight = point(3.2f, 11.1f)
private const val TOTAL_OBJECTS = 8
private const val PICKUP_RADIUS = 0.55f

// La linterna tiene un estado independiente para su futura función de iluminación.
// Los siete objetos genéricos conservan una identidad propia aunque se vean iguales.

private data class Decoracion(
    val sala: Room,
    val imagen: Int,
    val posicion: Offset,
    val ancho: Float,
    val alto: Float,
    val anchoColision: Float = 0f,
    val altoColision: Float = 0f
)

private val decoraciones = listOf(
    // Librero en la zona superior izquierda
    Decoracion(
        Room.MAIN, R.drawable.librero,
        point(2.35f, 2.1f),
        2.8f, 2.4f,
        1.1f, 1.5f
    ),

    // Mesa en la zona superior derecha
    Decoracion(
        Room.MAIN, R.drawable.mesa,
        point(9.2f, 5.1f),
        2.7f, 2.4f,
        1.35f, 0.85f
    ),

    // Cajas en la zona inferior derecha
    Decoracion(
        Room.MAIN, R.drawable.cajas,
        point(9.6f, 10.5f),
        2.6f, 2.6f,
        1.0f, 1.2f
    ),

    // Papeles en el pasillo central (sin colision)
    Decoracion(
        Room.MAIN, R.drawable.papeles,
        point(6f, 6.2f),
        2.2f, 2.0f
    ),

    // SALA DE ESTAR

    // Alfombra central (sin colision)
    Decoracion(
    Room.LIVING, R.drawable.alfombra,
    point(6f, 4.1f),
    3.8f, 2.8f
    ),

    // Sofa antiguo
    Decoracion(
    Room.LIVING, R.drawable.sofa,
    point(6f, 2.7f),
    3.2f, 2.0f,
    1.9f, 0.9f
    ),

    // Mesa de centro
    Decoracion(
    Room.LIVING, R.drawable.mesa,
    point(6f, 4.7f),
    2.0f, 1.6f,
    1.0f, 0.7f
    ),

    // Sillon individual
    Decoracion(
    Room.LIVING, R.drawable.sillon,
    point(9.1f, 3.8f),
    1.9f, 2.1f,
    0.9f, 1.0f
    ),

    // DORMITORIO

    // Cama antigua
    Decoracion(
        Room.BEDROOM, R.drawable.cama,
        point(5.4f, 1.7f),
        2.4f, 2.0f,
        1.35f, 1.05f
    ),

    // Buro junto a la cama
    Decoracion(
        Room.BEDROOM, R.drawable.buro,
        point(7.4f, 1.65f),
        1.2f, 1.2f,
        0.55f, 0.5f
    ),

    // Armario antiguo
    Decoracion(
        Room.BEDROOM, R.drawable.armario,
        point(10.1f, 5.4f),
        2.0f, 2.4f,
        0.9f, 1.1f
    ),

    // Alfombra desgastada
    Decoracion(
        Room.BEDROOM, R.drawable.alfombra,
        point(5.4f, 8.8f),
        2.8f, 2.0f
    ),

    // COBERTIZO

    // Mesa de trabajo
    Decoracion(
        Room.SHED, R.drawable.mesa,
        point(2.1f, 2.1f),
        2.3f, 1.8f,
        1.1f, 0.65f
    ),

    // Cajas apiladas en la parte inferior
    Decoracion(
        Room.SHED, R.drawable.cajas,
        point(2.2f, 10.7f),
        2.0f, 2.0f,
        0.95f, 1.0f
    ),

    // Cajas en el extremo derecho
    Decoracion(
        Room.SHED, R.drawable.cajas,
        point(10f, 10f),
        1.8f, 1.8f,
        0.85f, 0.8f
    ),

    // Papeles tirados por el suelo
    Decoracion(
        Room.SHED, R.drawable.papeles,
        point(6.4f, 4.3f),
        1.9f, 1.6f
    ),


    // VESTIBULO IZQUIERDO

    // Librero antiguo
    Decoracion(
        Room.LEFT, R.drawable.librero,
        point(2.2f, 2.3f),
        2.5f, 2.2f,
        1.1f, 1.3f
    ),

    // Sillon abandonado
    Decoracion(
        Room.LEFT, R.drawable.sillon,
        point(2.1f, 5f),
        1.8f, 2.0f,
        0.85f, 1.0f
    ),

    // Cajas apiladas
    Decoracion(
        Room.LEFT, R.drawable.cajas,
        point(9.2f, 7.7f),
        2.0f, 2.0f,
        0.9f, 1.0f
    ),

    // Papeles esparcidos
    Decoracion(
        Room.LEFT, R.drawable.papeles,
        point(6.2f, 11.4f),
        2.0f, 1.6f
    ),

    // VESTIBULO DERECHO

    // Librero en la parte superior derecha
    Decoracion(
        Room.RIGHT, R.drawable.librero,
        point(9.6f, 2.2f),
        2.1f, 2.0f,
        0.95f, 0.95f
    ),

    // Sillon antiguo
    Decoracion(
        Room.RIGHT, R.drawable.sillon,
        point(2f, 6.3f),
        1.9f, 2.0f,
        0.85f, 1.0f
    ),

    // Cajas abandonadas
    Decoracion(
        Room.RIGHT, R.drawable.cajas,
        point(9.6f, 10.5f),
        2.2f, 2.2f,
        0.95f, 1.0f
    ),

    // Papeles tirados en el suelo
    Decoracion(
        Room.RIGHT, R.drawable.papeles,
        point(5.2f, 11.5f),
        2.0f, 1.6f
    ),

    // =========================================
    // VESTIBULO DERECHO
    // =========================================

    Decoracion(
        Room.RIGHT, R.drawable.librero,
        point(9.6f, 2.2f),
        2.1f, 2.0f,
        0.95f, 0.95f
    ),

    Decoracion(
        Room.RIGHT, R.drawable.sillon,
        point(2f, 6.3f),
        1.9f, 2.0f,
        0.85f, 1.0f
    ),

    Decoracion(
        Room.RIGHT, R.drawable.cajas,
        point(9.6f, 10.5f),
        2.2f, 2.2f,
        0.95f, 1.0f
    ),

    Decoracion(
        Room.RIGHT, R.drawable.papeles,
        point(5.2f, 11.5f),
        2.0f, 1.6f
    ),


    // =========================================
    // SALON SUPERIOR
    // =========================================

    // Papeles en el suelo
    Decoracion(
        Room.UPPER, R.drawable.papeles,
        point(4.2f, 12f),
        1.8f, 1.4f
    ),

    // Escombros abandonados
    Decoracion(
        Room.UPPER, R.drawable.escombros,
        point(10.4f, 6f),
        1.6f, 1.5f
    ),

    // Librero antiguo
    Decoracion(
        Room.UPPER, R.drawable.librero,
        point(4.6f, 6.3f),
        2.5f, 2.2f,
        1.0f, 1.0f
    ),

    // Mesa de trabajo
    Decoracion(
        Room.UPPER, R.drawable.mesa_trabajo,
        point(6.6f, 5.2f),
        2.2f, 1.7f,
        1.0f, 0.65f
    ),


    // =========================================
    // PASADIZO 1
    // =========================================

    // Papeles viejos
    Decoracion(
        Room.PASSAGE_1, R.drawable.papeles,
        point(6.3f, 2.1f),
        1.6f, 1.5f
    ),

    // Escombros en el pasillo
    Decoracion(
        Room.PASSAGE_1, R.drawable.escombros,
        point(10.1f, 6.3f),
        1.6f, 1.5f
    ),

    // Barril oxidado
    Decoracion(
        Room.PASSAGE_1, R.drawable.barril,
        point(2.1f, 6f),
        1.4f, 1.6f,
        0.7f, 0.8f
    ),

    // Cajas abandonadas
    Decoracion(
        Room.PASSAGE_1, R.drawable.cajas,
        point(6.3f, 8f),
        1.8f, 1.8f,
        0.7f, 0.8f
    ),


    // =========================================
    // PASADIZO 2
    // =========================================

    // Papeles tirados
    Decoracion(
        Room.PASSAGE_2, R.drawable.papeles,
        point(7.6f, 1.7f),
        1.7f, 1.3f
    ),

    // Escombros en el suelo
    Decoracion(
        Room.PASSAGE_2, R.drawable.escombros,
        point(4.8f, 6.5f),
        1.5f, 1.2f
    ),

    // Barril abandonado
    Decoracion(
        Room.PASSAGE_2, R.drawable.barril,
        point(1.7f, 4.1f),
        1.3f, 1.5f,
        0.55f, 0.55f
    ),

    // Mesa de herramientas
    Decoracion(
        Room.PASSAGE_2, R.drawable.mesa_trabajo,
        point(6.2f, 12.6f),
        2.2f, 1.7f,
        0.95f, 0.5f
    ),


    // =========================================
    // SALON TRASERO
    // =========================================

    // Papeles esparcidos
    Decoracion(
        Room.REAR, R.drawable.papeles,
        point(4.3f, 9.2f),
        2.0f, 1.5f
    ),

    // Armario antiguo
    Decoracion(
        Room.REAR, R.drawable.armario,
        point(2.1f, 3.4f),
        2.0f, 2.4f,
        0.85f, 1.1f
    ),

    // Silla abandonada
    Decoracion(
        Room.REAR, R.drawable.silla,
        point(8.6f, 4.6f),
        1.4f, 1.5f,
        0.55f, 0.6f
    ),

    // Baul viejo
    Decoracion(
        Room.REAR, R.drawable.baul,
        point(8.6f, 7.8f),
        1.7f, 1.5f,
        0.7f, 0.6f
    ),


    // =========================================
    // ATICO
    // =========================================

    // Papeles antiguos
    Decoracion(
        Room.ATTIC, R.drawable.papeles,
        point(5.7f, 1.5f),
        1.8f, 1.3f
    ),

    // Escombros
    Decoracion(
        Room.ATTIC, R.drawable.escombros,
        point(1.8f, 5.1f),
        1.5f, 1.5f
    ),

    // Baul antiguo
    Decoracion(
        Room.ATTIC, R.drawable.baul,
        point(3.6f, 11.55f),
        1.7f, 1.5f,
        0.65f, 0.55f
    ),

    // Mesa de trabajo
    Decoracion(
        Room.ATTIC, R.drawable.mesa_trabajo,
        point(9.9f, 9.7f),
        2.2f, 1.7f,
        0.95f, 0.6f
    ),


    // =========================================
    // CORREDOR FINAL - TRAMO 1
    // =========================================

    // Solo decoraciones sin colision
    // para mantener libre el recorrido

    Decoracion(
        Room.FINAL_LOWER, R.drawable.papeles,
        point(6.3f, 4f),
        1.8f, 1.3f
    ),

    Decoracion(
        Room.FINAL_LOWER, R.drawable.escombros,
        point(2f, 9.6f),
        1.4f, 1.4f
    ),

    Decoracion(
        Room.FINAL_LOWER, R.drawable.papeles,
        point(10.1f, 7f),
        1.3f, 1.0f
    ),


    // =========================================
    // CORREDOR FINAL - TRAMO 2
    // =========================================

    // Sin obstaculos solidos para no
    // bloquear la persecucion final

    Decoracion(
        Room.FINAL_UPPER, R.drawable.papeles,
        point(10.1f, 9f),
        1.4f, 1.1f
    ),

    Decoracion(
        Room.FINAL_UPPER, R.drawable.escombros,
        point(6.35f, 5f),
        1.4f, 1.2f
    ),

    Decoracion(
        Room.FINAL_UPPER, R.drawable.papeles,
        point(1.65f, 10.8f),
        1.4f, 1.1f
    ),

    Decoracion(
        Room.FINAL_UPPER, R.drawable.escombros,
        point(2f, 5.05f),
        1.4f, 1.3f
    )



)

private data class Collectible(val id: Int, val room: Room, val position: Offset)
private val collectibles = listOf(
    Collectible(0, Room.MAIN, point(1.35f, 6.25f)),
    Collectible(1, Room.UPPER, point(7.7f, 3.9f)),
    Collectible(2, Room.RIGHT, point(5.55f, 5.35f)),
    Collectible(3, Room.REAR, point(4.45f, 5.65f)),
    Collectible(4, Room.ATTIC, point(6f, 9.55f)),
    Collectible(5, Room.SHED, point(10.2f, 12.1f)),
    Collectible(6, Room.BEDROOM, point(9.8f, 11.95f))
)
private val outline = listOf(
    wall(0.5f, 0.5f, 11.5f, 0.5f),
    wall(0.5f, 0.5f, 0.5f, 13.5f),
    wall(11.5f, 0.5f, 11.5f, 13.5f),
    wall(0.5f, 13.5f, 11.5f, 13.5f)
)

private val roomWalls = mapOf(
    Room.MAIN to outline + listOf(
        wall(0.5f, 7f, 4.5f, 7f),
        wall(7.5f, 7f, 11.5f, 7f),
        wall(4.5f, 0.5f, 4.5f, 3f),
        wall(4.5f, 5f, 4.5f, 9f),
        wall(4.5f, 11f, 4.5f, 13.5f),
        wall(7.5f, 0.5f, 7.5f, 3f),
        wall(7.5f, 5f, 7.5f, 9f),
        wall(7.5f, 11f, 7.5f, 13.5f)
    ),
    Room.LEFT to outline + listOf(
        wall(4.6f, 3.4f, 11.5f, 3.4f),
        wall(0.5f, 6.2f, 7.4f, 6.2f),
        wall(3.9f, 9.65f, 11.5f, 9.65f)
    ),
    Room.RIGHT to outline + listOf(
        wall(0.5f, 4.15f, 7.3f, 4.15f),
        wall(7.3f, 4.15f, 7.3f, 9.55f),
        wall(4f, 9.55f, 7.3f, 9.55f)
    ),
    Room.UPPER to outline + listOf(
        wall(2.3f, 2.6f, 2.3f, 10.05f),
        wall(2.3f, 10.05f, 9.05f, 10.05f),
        wall(9.05f, 2.6f, 9.05f, 10.05f),
        wall(5.25f, 2.6f, 9.05f, 2.6f)
    ),
    Room.PASSAGE_1 to outline + listOf(
        wall(4.4f, 0.5f, 4.4f, 10.2f),
        wall(8.1f, 3.8f, 8.1f, 13.5f)
    ),
    Room.PASSAGE_2 to outline + listOf(
        wall(3.55f, 2.9f, 11.5f, 2.9f),
        wall(3.55f, 2.9f, 3.55f, 5.3f),
        wall(3.55f, 5.3f, 11.5f, 5.3f),
        wall(0.5f, 7.95f, 8.05f, 7.95f),
        wall(8.05f, 7.95f, 8.05f, 10.3f),
        wall(0.5f, 10.3f, 8.05f, 10.3f)
    ),
    Room.REAR to outline + listOf(
        wall(6f, 1.9f, 6f, 8.7f),
        wall(2.25f, 10.15f, 9.65f, 10.15f)
    ),
    // Segundo piso: las coordenadas siguen el plano del documento.
    Room.LIVING to outline + listOf(
        wall(0.5f, 7.1f, 4.2f, 7.1f),
        wall(7.8f, 7.1f, 11.5f, 7.1f)
    ),
    Room.ATTIC to outline + listOf(
        wall(3.2f, 3.1f, 8.8f, 3.1f),
        wall(3.2f, 3.1f, 3.2f, 8.35f),
        wall(3.2f, 8.35f, 7.75f, 8.35f),
        wall(5.65f, 5.9f, 11.5f, 5.9f),
        wall(7.75f, 5.9f, 7.75f, 10.75f),
        wall(3.2f, 10.75f, 7.75f, 10.75f)
    ),
    Room.SHED to outline + listOf(
        wall(3.65f, 2.5f, 8.9f, 2.5f),
        wall(8.9f, 2.5f, 8.9f, 7.15f),
        wall(0.5f, 7.15f, 8.9f, 7.15f),
        wall(0.5f, 5.2f, 6.2f, 5.2f),
        wall(4.3f, 9.45f, 4.3f, 13.5f),
        wall(8.05f, 9.45f, 8.05f, 13.5f)
    ),
    Room.BEDROOM to outline + listOf(
        wall(0.5f, 3.25f, 8.9f, 3.25f),
        wall(3.1f, 10.25f, 11.5f, 10.25f)
    ),
    Room.FINAL_LOWER to outline + listOf(
        wall(3.1f, 2.15f, 11.5f, 2.15f),
        wall(3.1f, 2.15f, 3.1f, 3.3f),
        wall(3.1f, 3.3f, 11.5f, 3.3f),
        wall(0.5f, 4.8f, 8.8f, 4.8f),
        wall(8.8f, 4.8f, 8.8f, 5.9f),
        wall(0.5f, 5.9f, 8.8f, 5.9f),
        wall(3.1f, 7.55f, 11.5f, 7.55f),
        wall(3.1f, 7.55f, 3.1f, 8.7f),
        wall(3.1f, 8.7f, 11.5f, 8.7f),
        wall(0.5f, 10.6f, 8.8f, 10.6f),
        wall(8.8f, 10.6f, 8.8f, 11.8f),
        wall(0.5f, 11.8f, 8.8f, 11.8f)
    ),
    Room.FINAL_UPPER to outline + listOf(
        wall(2.2f, 0.5f, 2.2f, 1.65f),
        wall(2.2f, 1.65f, 5.1f, 1.65f),
        wall(5.1f, 0.5f, 5.1f, 11.6f),
        wall(2.5f, 11.6f, 5.1f, 11.6f),
        wall(0.5f, 3.1f, 3.1f, 3.1f),
        wall(3.1f, 3.1f, 3.1f, 4.35f),
        wall(0.5f, 4.35f, 3.1f, 4.35f),
        wall(2.2f, 6f, 5.1f, 6f),
        wall(2.2f, 6f, 2.2f, 7f),
        wall(2.2f, 7f, 5.1f, 7f),
        wall(0.5f, 8.6f, 3.1f, 8.6f),
        wall(3.1f, 8.6f, 3.1f, 9.8f),
        wall(0.5f, 9.8f, 3.1f, 9.8f),
        wall(7.65f, 2.35f, 7.65f, 13.5f),
        wall(7.65f, 2.35f, 8.95f, 2.35f),
        wall(8.95f, 2.35f, 8.95f, 13.5f)
    )
)

private val roomCollisionWalls = roomWalls.mapValues { (sala, paredes) ->

    val obstaculos = decoraciones
        .filter {
            it.sala == sala &&
                    it.anchoColision > 0f &&
                    it.altoColision > 0f
        }
        .flatMap { objeto ->

            val x1 = objeto.posicion.x - objeto.anchoColision / 2f
            val x2 = objeto.posicion.x + objeto.anchoColision / 2f
            val y1 = objeto.posicion.y - objeto.altoColision / 2f
            val y2 = objeto.posicion.y + objeto.altoColision / 2f

            listOf(
                Wall(Offset(x1, y1), Offset(x2, y1)),
                Wall(Offset(x2, y1), Offset(x2, y2)),
                Wall(Offset(x2, y2), Offset(x1, y2)),
                Wall(Offset(x1, y2), Offset(x1, y1))
            )
        }

    paredes + obstaculos
}

private enum class PortalKind(val color: Color) {
    ROOM(Color(0xFFADFAB5)),
    STAIRS(Color(0xFF37B7FB)),
    FINAL_ACCESS(Color(0xFFFF4242))
}
private enum class Doorway(
    val room: Room,
    val position: Offset,
    val arrival: Offset,
    val kind: PortalKind = PortalKind.ROOM
) {
    MAIN_LEFT(Room.MAIN, point(1.3f, 11.9f), point(2.2f, 11.6f)),
    LEFT_MAIN(Room.LEFT, point(10.45f, 11.9f), point(9.5f, 11.6f)),
    MAIN_RIGHT(Room.MAIN, point(10.6f, 2.15f), point(9.65f, 2.45f)),
    RIGHT_MAIN(Room.RIGHT, point(1.55f, 2.15f), point(2.5f, 2.45f)),
    LEFT_UPPER(Room.LEFT, point(9.7f, 1.5f), point(8.75f, 1.75f)),
    UPPER_LEFT(Room.UPPER, point(9.7f, 12.5f), point(8.75f, 12.25f)),
    LEFT_PASSAGE(Room.LEFT, point(1.75f, 12.85f), point(2.7f, 12.6f)),
    PASSAGE_LEFT(Room.PASSAGE_1, point(1.75f, 1.3f), point(2.7f, 1.55f)),
    RIGHT_PASSAGE(Room.RIGHT, point(10.2f, 12.85f), point(9.25f, 12.6f)),
    PASSAGE_RIGHT(Room.PASSAGE_2, point(10.2f, 1.35f), point(9.25f, 1.6f)),
    PASSAGE_REAR(Room.PASSAGE_2, point(1.55f, 11.9f), point(2.5f, 12.15f)),
    REAR_PASSAGE(Room.REAR, point(10.2f, 11.9f), point(9.25f, 12.15f)),
    PASSAGE_1_REAR(Room.PASSAGE_1, point(10.45f, 11.55f), point(9.5f, 11.6f)),
    REAR_PASSAGE_1(Room.REAR, point(1.3f, 11.9f), point(2.25f, 12.15f)),

    // Escaleras azules: cada pareja permite subir y bajar.
    MAIN_LIVING(Room.MAIN, point(6f, 1.2f), point(6f, 2.1f), PortalKind.STAIRS),
    LIVING_MAIN(Room.LIVING, point(6f, 13f), point(6f, 12.1f), PortalKind.STAIRS),
    UPPER_ATTIC(Room.UPPER, point(7.7f, 9f), point(7.7f, 8.1f), PortalKind.STAIRS),
    ATTIC_UPPER(Room.ATTIC, point(6f, 7.4f), point(5.05f, 7.4f), PortalKind.STAIRS),
    RIGHT_BEDROOM(Room.RIGHT, point(5.65f, 8.1f), point(4.7f, 8.1f), PortalKind.STAIRS),
    BEDROOM_RIGHT(Room.BEDROOM, point(6f, 7f), point(5.05f, 7f), PortalKind.STAIRS),

    LIVING_ATTIC(Room.LIVING, point(1.3f, 6.3f), point(2.25f, 6.3f)),
    ATTIC_LIVING(Room.ATTIC, point(10.55f, 6.85f), point(9.6f, 6.85f)),
    LIVING_SHED(Room.LIVING, point(10.6f, 6.3f), point(9.65f, 6.3f)),
    SHED_LIVING(Room.SHED, point(1.55f, 6.3f), point(2.5f, 6.3f)),
    SHED_BEDROOM(Room.SHED, point(2.1f, 12.7f), point(2.1f, 11.8f)),
    BEDROOM_SHED(Room.BEDROOM, point(2.1f, 2f), point(3.05f, 2f)),

    // El acceso rojo exige los siete objetos genéricos y la linterna.
    LIVING_FINAL(Room.LIVING, point(1.3f, 1.15f), point(2.25f, 1.15f), PortalKind.FINAL_ACCESS),
    FINAL_LIVING(Room.FINAL_LOWER, point(1.3f, 12.5f), point(2.25f, 12.5f), PortalKind.FINAL_ACCESS),
    FINAL_NEXT(Room.FINAL_LOWER, point(10.6f, 1.4f), point(9.65f, 1.4f)),
    FINAL_PREVIOUS(Room.FINAL_UPPER, point(10.6f, 12.7f), point(9.65f, 12.7f));

    fun destination(): Doorway = when (this) {
        MAIN_LEFT -> LEFT_MAIN
        LEFT_MAIN -> MAIN_LEFT
        MAIN_RIGHT -> RIGHT_MAIN
        RIGHT_MAIN -> MAIN_RIGHT
        LEFT_UPPER -> UPPER_LEFT
        UPPER_LEFT -> LEFT_UPPER
        LEFT_PASSAGE -> PASSAGE_LEFT
        PASSAGE_LEFT -> LEFT_PASSAGE
        RIGHT_PASSAGE -> PASSAGE_RIGHT
        PASSAGE_RIGHT -> RIGHT_PASSAGE
        PASSAGE_REAR -> REAR_PASSAGE
        REAR_PASSAGE -> PASSAGE_REAR
        PASSAGE_1_REAR -> REAR_PASSAGE_1
        REAR_PASSAGE_1 -> PASSAGE_1_REAR
        MAIN_LIVING -> LIVING_MAIN
        LIVING_MAIN -> MAIN_LIVING
        UPPER_ATTIC -> ATTIC_UPPER
        ATTIC_UPPER -> UPPER_ATTIC
        RIGHT_BEDROOM -> BEDROOM_RIGHT
        BEDROOM_RIGHT -> RIGHT_BEDROOM
        LIVING_ATTIC -> ATTIC_LIVING
        ATTIC_LIVING -> LIVING_ATTIC
        LIVING_SHED -> SHED_LIVING
        SHED_LIVING -> LIVING_SHED
        SHED_BEDROOM -> BEDROOM_SHED
        BEDROOM_SHED -> SHED_BEDROOM
        LIVING_FINAL -> FINAL_LIVING
        FINAL_LIVING -> LIVING_FINAL
        FINAL_NEXT -> FINAL_PREVIOUS
        FINAL_PREVIOUS -> FINAL_NEXT
    }
}

private val roomDoorways = Doorway.entries.groupBy { it.room }

// Meta naranja: llegar aquí con los ocho objetos completa la partida.
private val finalMarker = point(1.3f, 1.15f)

private fun isNear(position: Offset, target: Offset, radius: Float): Boolean {
    val dx = position.x - target.x
    val dy = position.y - target.y
    return dx * dx + dy * dy <= radius * radius
}

private fun hitsWall(
    position: Offset,
    activeWalls: List<Wall>,
    clearance: Float = PLAYER_RADIUS + WALL_THICKNESS / 2f
): Boolean {
    return activeWalls.any { wall ->
        val closest = Offset(
            position.x.coerceIn(minOf(wall.start.x, wall.end.x), maxOf(wall.start.x, wall.end.x)),
            position.y.coerceIn(minOf(wall.start.y, wall.end.y), maxOf(wall.start.y, wall.end.y))
        )
        isNear(position, closest, clearance)
    }
}

// Un solo monstruo, con la misma aparición inicial; recorre puertas verdes y escaleras.
private val monsterSpawnRoom = Room.UPPER
private val monsterSpawn = point(10.1f, 1.4f)
private const val MONSTER_PATROL_BASE = 2.8f
private const val MONSTER_PATROL_PER_OBJECT = 0.12f
private const val MONSTER_CHASE_BASE = 3.6f
private const val MONSTER_CHASE_PER_OBJECT = 0.12f
private const val MONSTER_SIGHT_RANGE = 12f
private const val MONSTER_CAPTURE_DISTANCE = 0.58f
private const val MONSTER_DOOR_DELAY = 0.30f

private fun monsterChaseSpeed(objects: Int) =
    MONSTER_CHASE_BASE + objects.coerceIn(0, TOTAL_OBJECTS) * MONSTER_CHASE_PER_OBJECT

private fun monsterPatrolSpeed(objects: Int) =
    if (objects >= 6) monsterChaseSpeed(objects)
    else MONSTER_PATROL_BASE + objects.coerceIn(0, TOTAL_OBJECTS) * MONSTER_PATROL_PER_OBJECT

private const val FINAL_PURSUIT_DELAY_MS = 4_000L
private fun isFinalRoom(room: Room) = room == Room.FINAL_LOWER || room == Room.FINAL_UPPER


private fun nextMonsterWarpDelay(objects: Int): Long =
    if (objects >= 6) {
        kotlin.random.Random.nextLong(5_000L, 8_001L)
    } else {
        kotlin.random.Random.nextLong(8_000L, 12_001L)
    }

// Primera puerta de la ruta más corta entre salas. También respeta el bloqueo rojo.
private fun nextDoorToward(from: Room, goal: Room, objects: Int): Doorway? {
    if (from == goal) return null
    val visited = mutableSetOf(from)
    val queue = java.util.ArrayDeque<Pair<Room, Doorway?>>()
    queue.add(from to null)
    while (!queue.isEmpty()) {
        val (room, firstDoor) = queue.removeFirst()
        for (door in roomDoorways.getValue(room)) {
            if (door.kind == PortalKind.FINAL_ACCESS && objects < TOTAL_OBJECTS) continue
            val destination = door.destination().room
            if (!visited.add(destination)) continue
            val first = firstDoor ?: door
            if (destination == goal) return first
            queue.add(destination to first)
        }
    }
    return null
}

// Se muestrea más fino que el grosor de una pared, tanto para la vista como para las rutas.
private fun clearSegment(from: Offset, to: Offset, walls: List<Wall>, clearance: Float): Boolean {
    val delta = to - from
    val steps = kotlin.math.ceil(delta.getDistance() / 0.06f).toInt().coerceAtLeast(1)
    for (i in 0..steps) {
        if (hitsWall(from + delta * (i.toFloat() / steps), walls, clearance)) return false
    }
    return true
}

// Una cuadrícula pequeña se construye una vez; BFS evita que la patrulla se atasque
// al volver de una persecución. No se recalcula la ruta en cada fotograma.
private class MonsterBrain(room: Room) {
    private val walls = roomCollisionWalls.getValue(room)
    private val clearance = PLAYER_RADIUS + WALL_THICKNESS / 2f
    private val columns = 31
    private val rows = 45
    private val nodes = Array(columns * rows) { index ->
        Offset((index % columns) * 0.4f, (index / columns) * 0.4f)
    }
    private val free = BooleanArray(nodes.size) { index ->
        val p = nodes[index]
        p.x in 0.9f..11.1f && p.y in 1f..16.5f && !hitsWall(p, walls)
    }
    private val neighbours = Array(nodes.size) { index ->
        if (!free[index]) emptyList<Int>() else {
            val x = index % columns
            val y = index / columns
            listOfNotNull(
                if (x > 0) index - 1 else null,
                if (x < columns - 1) index + 1 else null,
                if (y > 0) index - columns else null,
                if (y < rows - 1) index + columns else null
            ).filter { free[it] && clearSegment(nodes[index], nodes[it], walls, clearance) }
        }
    }
    var path: List<Offset> = emptyList()
    var index = 0
    var age = 1f

    fun reset() {
        path = emptyList()
        index = 0
        age = 1f
    }

    fun route(from: Offset, to: Offset): List<Offset> {
        if (clearSegment(from, to, walls, clearance)) return listOf(to)
        fun nearestVisible(position: Offset): Int? = nodes.indices.asSequence()
            .filter { free[it] }
            .sortedBy { (nodes[it] - position).getDistanceSquared() }
            .firstOrNull { clearSegment(position, nodes[it], walls, clearance) }
        val start = nearestVisible(from) ?: return emptyList()
        val goal = nearestVisible(to) ?: return emptyList()
        val previous = IntArray(nodes.size) { -1 }
        val queue = java.util.ArrayDeque<Int>()
        previous[start] = start
        queue.add(start)
        while (!queue.isEmpty() && previous[goal] == -1) {
            val current = queue.removeFirst()
            neighbours[current].forEach { next ->
                if (previous[next] == -1) {
                    previous[next] = current
                    queue.add(next)
                }
            }
        }
        if (previous[goal] == -1) return emptyList()
        val result = mutableListOf<Offset>()
        var current = goal
        while (current != start) {
            result.add(nodes[current])
            current = previous[current]
        }
        result.add(nodes[start])
        result.reverse()
        result.add(to)
        return result
    }
}

// El tiempo se calcula con un reloj monotónico, no contando fotogramas.
private fun formatGameTime(milliseconds: Long): String {
    val seconds = milliseconds.coerceAtLeast(0L) / 1000L
    return "%02d:%02d".format(Locale.ROOT, seconds / 60L, seconds % 60L)
}

// Se descuentan 100 puntos por segundo completo, con un mínimo de cero.
private fun calculateScore(objects: Int, escaped: Boolean, milliseconds: Long): Long {
    val escapeBonus = if (escaped) 100_000L else 0L
    val objectBonus = objects * 30_000L
    val timePenalty = (milliseconds.coerceAtLeast(0L) / 1000L) * 100L
    return (escapeBonus + objectBonus - timePenalty).coerceAtLeast(0L)
}


private class AudioJuego(context: Context) {

    private val soundPool = SoundPool.Builder()
        .setMaxStreams(5)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private var sonidoRecoger = 0
    private var sonidoPuerta = 0

    private var sonidoPasos = 0
    private var pasosListos = false
    private var ultimoPasoMs = 0L

    private var recogerListo = false
    private var puertaLista = false

    private var sonidoLatido = 0
    private var latidoListo = false
    private var ultimoLatidoMs = 0L
    private var estadoLatidoAnterior = 0

    private var sonidoPuertaCerrada = 0
    private var puertaCerradaLista = false
    private var ultimoIntentoPuertaCerrada = 0L

    init {
        soundPool.setOnLoadCompleteListener { _, id, estado ->
            if (estado == 0) {
                if (id == sonidoRecoger) {
                    recogerListo = true
                }

                if (id == sonidoPuerta) {
                    puertaLista = true
                }
                if (id == sonidoPasos) {
                    pasosListos = true
                }

                if (id == sonidoLatido && estado == 0) {
                    latidoListo = true
                }

                if (id == sonidoPuertaCerrada) {
                    puertaCerradaLista = true
                }

            }

        }

        sonidoRecoger = soundPool.load(
            context, R.raw.recoger, 1
        )

        sonidoPuerta = soundPool.load(
            context, R.raw.puerta, 1
        )

        sonidoPasos = soundPool.load(
            context, R.raw.pasos, 1
        )

        sonidoLatido = soundPool.load(
            context, R.raw.latido, 1
        )

        sonidoPuertaCerrada = soundPool.load(
            context, R.raw.puerta_cerrada, 1
        )
    }

    fun recoger() {
        if (recogerListo) {
            soundPool.play(
                sonidoRecoger,
                1f, 1f,
                1, 0, 1f
            )
        }
    }

    fun abrirPuerta() {
        if (puertaLista) {
            soundPool.play(
                sonidoPuerta,
                1f, 1f,
                1, 0, 1f
            )
        }
    }


    fun caminar() {
        val ahora = SystemClock.elapsedRealtime()

        // Esperar 360 ms entre pisadas
        if (!pasosListos || ahora - ultimoPasoMs < 360L) {
            return
        }

        ultimoPasoMs = ahora

        soundPool.play(
            sonidoPasos,
            0.30f, 0.30f,
            1, 0, 1f
        )
    }

    fun actualizarLatido(estado: Int) {

        // 0 = silencio
        // 1 = latidos lentos
        // 2 = latidos rapidos

        if (estado != estadoLatidoAnterior) {
            estadoLatidoAnterior = estado
            ultimoLatidoMs = 0L
        }

        if (estado == 0 || !latidoListo) return

        val ahora = SystemClock.elapsedRealtime()

        val intervalo = if (estado == 2) {
            500L
        } else {
            950L
        }

        if (ahora - ultimoLatidoMs < intervalo) return

        ultimoLatidoMs = ahora

        val volumen = if (estado == 2) {
            0.85f
        } else {
            0.40f
        }

        soundPool.play(
            sonidoLatido,
            volumen, volumen,
            1, 0, 1f
        )
    }

    fun puertaCerrada() {
        if (!puertaCerradaLista) return

        val ahora = SystemClock.elapsedRealtime()

        // Evita repetir el sonido constantemente
        if (ahora - ultimoIntentoPuertaCerrada < 1000L) {
            return
        }

        ultimoIntentoPuertaCerrada = ahora

        soundPool.play(
            sonidoPuertaCerrada,
            1f, 1f,
            1, 0, 1f
        )
    }

    fun liberar() {
        soundPool.release()
    }
}

@Composable

fun GameScreen(onBack: () -> Unit) {
    var gameStarted by rememberSaveable { mutableStateOf(false) }
    var nameDraft by rememberSaveable { mutableStateOf("") }
    var playerName by rememberSaveable { mutableStateOf("") }
    var startedAt by rememberSaveable { mutableLongStateOf(0L) }
    var elapsedMillis by rememberSaveable { mutableLongStateOf(0L) }
    var personalBest by rememberSaveable { mutableStateOf<Long?>(null) }
    val keyboard = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    val audioJuego = remember(context) {
        AudioJuego(context.applicationContext)
    }

    DisposableEffect(audioJuego) {
        onDispose {
            audioJuego.liberar()
        }
    }

    val partidasRepository = remember {
        PartidasFirebaseRepository()
    }
    val records = remember(context) {
        context.getSharedPreferences("escape_room_records", Context.MODE_PRIVATE)

    }

    var roomName by rememberSaveable { mutableStateOf(Room.MAIN.name) }
    var playerX by rememberSaveable { mutableFloatStateOf(entrance.x) }
    var playerY by rememberSaveable { mutableFloatStateOf(entrance.y) }
    var playerFacing by rememberSaveable { mutableStateOf("abajo") }
    var hasFlashlight by rememberSaveable { mutableStateOf(false) }
    var collectedMask by rememberSaveable { mutableIntStateOf(0) }
    var hasWon by rememberSaveable { mutableStateOf(false) }
    var hasLost by rememberSaveable { mutableStateOf(false) }
    var mostrarScreamer by rememberSaveable {
        mutableStateOf(false)
    }
    var monsterX by rememberSaveable { mutableFloatStateOf(monsterSpawn.x) }
    var monsterY by rememberSaveable { mutableFloatStateOf(monsterSpawn.y) }
    var monsterFacing by rememberSaveable {
        mutableStateOf("abajo")
    }
    var monsterRoomName by rememberSaveable { mutableStateOf(monsterSpawnRoom.name) }
    var monsterArrivalDoor by rememberSaveable { mutableStateOf<String?>(null) }
    var monsterRoamDoor by rememberSaveable { mutableStateOf<String?>(null) }
    var monsterFollowDoor by rememberSaveable { mutableStateOf<String?>(null) }
    var monsterDoorWait by rememberSaveable { mutableFloatStateOf(0f) }
    var monsterChasing by remember { mutableStateOf(false) }
    var monsterNavigationTarget by rememberSaveable { mutableStateOf<String?>(null) }
    var nextMonsterWarpAt by rememberSaveable { mutableLongStateOf(0L) }
    var finalEntryAt by rememberSaveable { mutableLongStateOf(0L) }
    var finalRushHandled by rememberSaveable { mutableStateOf(false) }
    var monsterVisits by remember {
        mutableStateOf(mapOf(monsterSpawnRoom.name to 1))
    }
    // Las cuadrículas se construyen al visitar cada sala y luego se reutilizan.
    val monsterBrains = remember { mutableMapOf<Room, MonsterBrain>() }
    val monsterRoom = Room.valueOf(monsterRoomName)
    fun brainFor(room: Room) = monsterBrains.getOrPut(room) { MonsterBrain(room) }
    var destinationDoorName by rememberSaveable { mutableStateOf<String?>(null) }

    var joystickDirection by remember { mutableStateOf(Offset.Zero) }
    val view = LocalView.current
    var windowFocused by remember(view) { mutableStateOf(view.hasWindowFocus()) }
    DisposableEffect(view) {
        val observer = view.viewTreeObserver
        val listener = ViewTreeObserver.OnWindowFocusChangeListener { focused ->
            windowFocused = focused
            if (!focused) joystickDirection = Offset.Zero
        }
        observer.addOnWindowFocusChangeListener(listener)
        onDispose {
            if (observer.isAlive) observer.removeOnWindowFocusChangeListener(listener)
        }
    }
    val collectedCount = Integer.bitCount(collectedMask) + if (hasFlashlight) 1 else 0

    fun restart() {
        roomName = Room.MAIN.name
        playerX = entrance.x
        playerY = entrance.y
        hasFlashlight = false
        collectedMask = 0
        hasWon = false
        hasLost = false
        mostrarScreamer = false
        monsterX = monsterSpawn.x
        monsterY = monsterSpawn.y
        monsterRoomName = monsterSpawnRoom.name
        monsterArrivalDoor = null
        monsterRoamDoor = null
        monsterFollowDoor = null
        monsterDoorWait = 0f
        monsterChasing = false
        monsterNavigationTarget = null
        nextMonsterWarpAt = SystemClock.elapsedRealtime() + nextMonsterWarpDelay(0)
        finalEntryAt = 0L
        finalRushHandled = false
        monsterVisits = mapOf(monsterSpawnRoom.name to 1)
        monsterBrains.values.forEach { it.reset() }
        destinationDoorName = null
        joystickDirection = Offset.Zero
        elapsedMillis = 0L
        personalBest = null
        startedAt = SystemClock.elapsedRealtime()
        gameStarted = true
    }

    fun startGame(name: String) {
        keyboard?.hide()
        playerName = name.trim()
        restart()
    }


    fun finishGame(escaped: Boolean) {
        if (hasWon || hasLost) return

        // Calcular tiempo, objetos y puntuación
        elapsedMillis = (
                SystemClock.elapsedRealtime() - startedAt
                ).coerceAtLeast(0L)

        val objetosObtenidos =
            Integer.bitCount(collectedMask) +
                    (if (hasFlashlight) 1 else 0)

        val puntosObtenidos = calculateScore(
            objetosObtenidos,
            escaped,
            elapsedMillis
        )

        // Conservar el récord personal local
        if (escaped && playerName.isNotBlank()) {
            val key = "best:$playerName"
            val best = maxOf(
                records.getLong(key, puntosObtenidos),
                puntosObtenidos
            )

            personalBest = best
            records.edit().putLong(key, best).apply()
        }

        // Finalizar la partida

        joystickDirection = Offset.Zero
        hasWon = escaped
        hasLost = !escaped

// Mostrar screamer solamente cuando perdemos
        mostrarScreamer = !escaped


        // Preparar los datos para Firebase
        val partida = Partida(
            jugador = playerName.trim()
                .take(40)
                .ifBlank { "Invitado" },
            puntuacion = puntosObtenidos,
            tiempoSegundos = elapsedMillis / 1000L,
            objetos = objetosObtenidos.toLong(),
            resultado = if (escaped) "victoria"
            else "derrota"
        )

        // Guardar automáticamente en Firestore
        partidasRepository.guardarPartida(
            partida = partida,
            onSuccess = {
                Log.d(
                    "FirebasePartidas",
                    "Partida guardada correctamente"
                )
            },
            onError = { error ->
                Log.e(
                    "FirebasePartidas",
                    "Error al guardar partida",
                    error
                )
            }
        )
    }


    fun checkCapture() {
        if (roomName == monsterRoomName &&
            isNear(Offset(playerX, playerY), Offset(monsterX, monsterY), MONSTER_CAPTURE_DISTANCE) &&
            clearSegment(Offset(playerX, playerY), Offset(monsterX, monsterY),
                roomCollisionWalls.getValue(Room.valueOf(monsterRoomName)), WALL_THICKNESS / 2f)) {
            finishGame(false)
        }
    }

    fun relocateMonster(arrival: Doorway) {
        monsterBrains[Room.valueOf(monsterRoomName)]?.reset()
        monsterRoomName = arrival.room.name
        monsterX = arrival.arrival.x
        monsterY = arrival.arrival.y
        monsterArrivalDoor = arrival.name
        monsterRoamDoor = null
        monsterFollowDoor = null
        monsterDoorWait = 0f
        monsterChasing = false
        monsterNavigationTarget = null
        monsterVisits = monsterVisits + (arrival.room.name to
            ((monsterVisits[arrival.room.name] ?: 0) + 1))
        brainFor(arrival.room).reset()
        checkCapture()
    }

    LaunchedEffect(gameStarted, hasWon, hasLost, startedAt) {
        if (!gameStarted || hasWon || hasLost) return@LaunchedEffect
        while (true) {
            if (!hasWon && !hasLost) elapsedMillis = (SystemClock.elapsedRealtime() - startedAt).coerceAtLeast(0L)
            delay(200L)
        }
    }

    if (!gameStarted) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Antes de entrar…", style = MaterialTheme.typography.headlineMedium)
            Text("Escribe tu nombre para ver tu puntuación y tu récord al escapar.",
                style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            OutlinedTextField(
                value = nameDraft,
                onValueChange = { value ->
                    nameDraft = value.filter { !it.isISOControl() }.take(24)
                },
                label = { Text("Nombre de usuario") },
                supportingText = { Text("Hasta 24 caracteres. No necesitas una cuenta.") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (nameDraft.isNotBlank()) startGame(nameDraft)
                }),
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { startGame(nameDraft) },
                enabled = nameDraft.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Empezar") }
            OutlinedButton(onClick = { startGame("") }, modifier = Modifier.fillMaxWidth()) {
                Text("Jugar sin usuario")
            }
            TextButton(onClick = { keyboard?.hide(); onBack() }) { Text("← Menú principal") }
        }
        return
    }



    if (hasLost && mostrarScreamer) {

        // Reproducir el sonido del screamer
        DisposableEffect(context) {

            val reproductor = MediaPlayer.create(
                context,
                R.raw.screamer
            )

            if (reproductor != null) {

                // Cuando termina el audio,
                // desaparece la imagen
                reproductor.setOnCompletionListener {
                    mostrarScreamer = false
                }

                reproductor.setOnErrorListener { _, _, _ ->
                    mostrarScreamer = false
                    true
                }

                reproductor.start()

            } else {
                mostrarScreamer = false
            }

            onDispose {
                reproductor?.release()
            }
        }

        // Mostrar imagen en pantalla

        val movimientoVertical by rememberInfiniteTransition(
            label = "Screamer"
        ).animateFloat(
            initialValue = -50f,
            targetValue = 50f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 100,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "Movimiento vertical"
        )

        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clipToBounds()
            ) {
                Image(
                    bitmap = ImageBitmap.imageResource(
                        R.drawable.screamer_entidad
                    ),
                    contentDescription = "La entidad te ha atrapado",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationY = movimientoVertical
                            scaleX = 1.15f
                            scaleY = 1.15f
                        },
                    contentScale = ContentScale.Crop,
                    filterQuality = FilterQuality.None
                )
            }
        }
    }


    if (hasWon || hasLost) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(min = 400.dp).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically)
        ) {
            Text(if (hasWon) "¡Has ganado!" else "Game Over", style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
            Text(if (hasWon) "Recuperaste los 8 objetos y escapaste de la casa." else "El monstruo te atrapó.",
                style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            Text("Objetos: $collectedCount/8" + if (hasFlashlight) " · Linterna recuperada" else "")
            // Invitados conservan la victoria anterior; en derrota también se ve su puntuación.
            if (playerName.isNotBlank() || hasLost) {
                val score = calculateScore(collectedCount, hasWon, elapsedMillis)
                if (playerName.isNotBlank()) {
                    Text("Jugador: $playerName", style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center)
                }
                Text("Tiempo: ${formatGameTime(elapsedMillis)}")
                Text("Puntuación: ${String.format(Locale.US, "%,d", score)} puntos",
                    style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                personalBest?.let { best ->
                    Text("Récord personal: ${String.format(Locale.US, "%,d", best)} puntos",
                        textAlign = TextAlign.Center)
                }
            }
            Button(onClick = { restart() }, modifier = Modifier.fillMaxWidth()) {
                Text(if (hasWon) "Volver a jugar" else "Intentar de nuevo")
            }
            TextButton(onClick = onBack) { Text("Menú principal") }
        }
        return
    }

    val room = Room.valueOf(roomName)
    val activeWalls = roomWalls.getValue(room)
    val doorways = roomDoorways.getValue(room)

    fun move(dx: Float, dy: Float) {
        if (!gameStarted || hasWon || hasLost || !windowFocused) return
        val currentRoom = Room.valueOf(roomName)
        val walls = roomCollisionWalls.getValue(currentRoom)
        val previous = Offset(playerX, playerY)
        // Se resuelve cada eje por separado para deslizarse junto a las paredes.
        var next = previous
        val horizontal = next + Offset(dx, 0f)
        if (!hitsWall(horizontal, walls)) next = horizontal
        val vertical = next + Offset(0f, dy)
        if (!hitsWall(vertical, walls)) next = vertical

        if (next == previous) return
        val movimiento = next - previous

// Reproducir pisadas cuando el personaje avanza
        audioJuego.caminar()


        playerFacing = if (
            kotlin.math.abs(movimiento.x) > kotlin.math.abs(movimiento.y)
        ) {
            if (movimiento.x > 0f) "derecha" else "izquierda"
        } else {
            if (movimiento.y > 0f) "abajo" else "arriba"
        }

        playerX = next.x
        playerY = next.y
        checkCapture()
        if (hasLost) return

        if (
            currentRoom == Room.MAIN &&
            !hasFlashlight &&
            isNear(next, flashlight, PICKUP_RADIUS)
        ) {
            hasFlashlight = true
            audioJuego.recoger()
        }



        collectibles.forEach { item ->
            if (
                item.room == currentRoom &&
                (collectedMask and (1 shl item.id)) == 0 &&
                isNear(next, item.position, PICKUP_RADIUS)
            ) {
                collectedMask = collectedMask or (1 shl item.id)

                // Reproducir sonido al recoger el objeto
                audioJuego.recoger()
            }
        }

        // Se consulta el estado actual: recoger repetidamente un objeto no suma más veces.
        val countNow = Integer.bitCount(collectedMask) + if (hasFlashlight) 1 else 0
        if (currentRoom == Room.FINAL_UPPER && countNow == TOTAL_OBJECTS &&
            isNear(next, finalMarker, PORTAL_RADIUS)) {
            finishGame(true)
            return
        }

        val arrivalDoor = destinationDoorName?.let { Doorway.valueOf(it) }
        if (arrivalDoor != null && !isNear(next, arrivalDoor.position, REARM_DISTANCE)) {
            destinationDoorName = null
        }

        val doorway = roomDoorways.getValue(currentRoom).firstOrNull {
            it.name != destinationDoorName && isNear(next, it.position, PORTAL_RADIUS)
        }
        if (doorway != null) {

            if (
                doorway.kind == PortalKind.FINAL_ACCESS &&
                countNow < TOTAL_OBJECTS
            ) {
                audioJuego.puertaCerrada()
                return
            }

            // Solo recuerda la puerta si ha visto al jugador cruzarla.
            // No conoce la ubicación del jugador a través de paredes u otras salas.
            val monsterPosition = Offset(monsterX, monsterY)
            val witnessed = currentRoom.name == monsterRoomName &&
                isNear(monsterPosition, next, MONSTER_SIGHT_RANGE) &&
                clearSegment(monsterPosition, next, walls, WALL_THICKNESS / 2f)
            if (witnessed) {
                monsterFollowDoor = doorway.name
                monsterRoamDoor = null
                monsterDoorWait = 0f
                brainFor(currentRoom).reset()
                monsterChasing = true
            }

            val destination = doorway.destination()

            // Sonido al atravesar una puerta o escalera
            audioJuego.abrirPuerta()

            destinationDoorName = destination.name
            roomName = destination.room.name
            playerX = destination.arrival.x
            playerY = destination.arrival.y

            if (isFinalRoom(destination.room) && !isFinalRoom(currentRoom)) {
                finalEntryAt = SystemClock.elapsedRealtime()
                finalRushHandled = false
            } else if (!isFinalRoom(destination.room)) {
                finalEntryAt = 0L
                finalRushHandled = false
            }
            checkCapture()
        }
    }

    fun updateMonster(seconds: Float) {
        if (!gameStarted || hasWon || hasLost || !windowFocused) return
        val now = SystemClock.elapsedRealtime()
        val count = Integer.bitCount(collectedMask) + if (hasFlashlight) 1 else 0
        val playerRoom = Room.valueOf(roomName)
        var currentRoom = Room.valueOf(monsterRoomName)

        // Una intervención por entrada al área final, a los cuatro segundos.
        // Si ya está en cualquiera de los dos corredores, conserva su posición y su persecución.
        if (isFinalRoom(playerRoom) && finalEntryAt > 0L && !finalRushHandled &&
            now - finalEntryAt >= FINAL_PURSUIT_DELAY_MS) {
            finalRushHandled = true
            if (!isFinalRoom(currentRoom)) {
                val door = nextDoorToward(currentRoom, Room.FINAL_LOWER, count)
                val far = door != null && (door.destination().room != Room.FINAL_LOWER ||
                    !isNear(Offset(monsterX, monsterY), door.position, 4f))
                if (far) {
                    relocateMonster(Doorway.FINAL_LIVING)
                    currentRoom = Room.valueOf(monsterRoomName)
                    if (hasLost) return
                }
            }
        }

        // Reaparición en una sala VECINA, nunca encima del jugador ni dentro del área final.
        // Se desactiva en los corredores para no interrumpir la persecución final.
        if (now >= nextMonsterWarpAt) {
            nextMonsterWarpAt = now + nextMonsterWarpDelay(count)
            if (!isFinalRoom(playerRoom) && !isFinalRoom(currentRoom) && currentRoom != playerRoom) {
                val nearbyArrivals = roomDoorways.getValue(playerRoom)
                    .filter { it.kind != PortalKind.FINAL_ACCESS || count == TOTAL_OBJECTS }
                    .map { it.destination() }
                    .filter { !isFinalRoom(it.room) && it.room != currentRoom }
                nearbyArrivals.randomOrNull()?.let { arrival ->
                    relocateMonster(arrival)
                    currentRoom = Room.valueOf(monsterRoomName)
                }
            }
        }
        val walls = roomCollisionWalls.getValue(currentRoom)
        val brain = brainFor(currentRoom)
        var position = Offset(monsterX, monsterY)
        val player = Offset(playerX, playerY)
        val knowsPosition = count == TOTAL_OBJECTS
        val tracksPlayerHere = knowsPosition && playerRoom == currentRoom
        val seesPlayer = roomName == monsterRoomName &&
            isNear(position, player, MONSTER_SIGHT_RANGE) &&
            clearSegment(position, player, walls, WALL_THICKNESS / 2f)
        val detectaCercania = playerRoom == currentRoom &&
                isNear(position, player, 4f)
        val allowedDoors = roomDoorways.getValue(currentRoom).filter {
            it.kind != PortalKind.FINAL_ACCESS || count == TOTAL_OBJECTS
        }
        // Antes de 8 objetos solo sigue puertas presenciadas; con los 8 usa la posición exacta.
        val followDoor = monsterFollowDoor?.let { name ->
            allowedDoors.firstOrNull { it.name == name }
        }
        if (monsterFollowDoor != null && followDoor == null) monsterFollowDoor = null
        val pursuing = knowsPosition || seesPlayer ||
                detectaCercania || followDoor != null
        if (pursuing != monsterChasing) {
            brain.reset()
            monsterDoorWait = 0f
        }
        monsterChasing = pursuing
        if (seesPlayer || detectaCercania || tracksPlayerHere) {
            monsterFollowDoor = null
            monsterRoamDoor = null
            monsterDoorWait = 0f
        }

        var targetDoor: Doorway? = null
        if (!seesPlayer && !detectaCercania && !tracksPlayerHere) {
            targetDoor = if (knowsPosition) nextDoorToward(currentRoom, playerRoom, count) else followDoor
            if (targetDoor == null) {
                targetDoor = allowedDoors.firstOrNull { it.name == monsterRoamDoor }
                if (targetDoor == null) {
                    // Prefiere salas poco visitadas y evita volver por la puerta de llegada,
                    // salvo en un callejón sin salida. Los empates se eligen al azar.
                    val options = allowedDoors.filter { it.name != monsterArrivalDoor }
                        .ifEmpty { allowedDoors }
                    val leastVisits = options.minOfOrNull {
                        monsterVisits[it.destination().room.name] ?: 0
                    }
                    targetDoor = options.filter {
                        (monsterVisits[it.destination().room.name] ?: 0) == leastVisits
                    }.randomOrNull()
                    monsterRoamDoor = targetDoor?.name
                    brain.reset()
                }
            }
        }
        val tracksMovingPlayer = seesPlayer ||
                detectaCercania || tracksPlayerHere
        val target = if (tracksMovingPlayer) player else targetDoor?.position ?: return
        val targetId = if (tracksMovingPlayer) "PLAYER" else targetDoor?.name
        if (targetId != monsterNavigationTarget) {
            monsterNavigationTarget = targetId
            brain.reset()
            monsterDoorWait = 0f
        }
        brain.age += seconds
        if ((tracksMovingPlayer && brain.age >= 0.35f) || brain.index >= brain.path.size) {
            brain.path = brain.route(position, target)
            brain.index = 0
            brain.age = 0f
        }
        val speed = if (pursuing) monsterChaseSpeed(count) else monsterPatrolSpeed(count)
        var remaining = seconds * speed
        while (remaining > 0f && brain.index < brain.path.size) {
            val waypoint = brain.path[brain.index]
            val delta = waypoint - position
            val distance = delta.getDistance()
            if (distance < 0.001f) {
                brain.index++
                continue
            }
            val step = minOf(remaining, distance)
            val next = position + delta * (step / distance)
            if (!clearSegment(position, next, walls, PLAYER_RADIUS + WALL_THICKNESS / 2f)) {
                brain.reset()
                break
            }
            position = next
            remaining -= step
            if (step >= distance) brain.index++
        }

        val movimientoMonstruo = position - Offset(monsterX, monsterY)

        if (movimientoMonstruo.getDistance() > 0.002f) {
            monsterFacing = if (
                kotlin.math.abs(movimientoMonstruo.x) >
                kotlin.math.abs(movimientoMonstruo.y)
            ) {
                if (movimientoMonstruo.x > 0f) "derecha" else "izquierda"
            } else {
                if (movimientoMonstruo.y > 0f) "abajo" else "arriba"
            }
        }
        monsterX = position.x
        monsterY = position.y
        checkCapture()
        if (hasLost) return

        // El monstruo usa exactamente la pareja de puertas del jugador, con una breve transición.
        val exit = targetDoor
        if (exit != null && isNear(position, exit.position, 0.35f)) {
            monsterDoorWait += seconds
            if (monsterDoorWait >= MONSTER_DOOR_DELAY) {
                relocateMonster(exit.destination())
            }
        } else {
            monsterDoorWait = 0f
        }
    }

    val moveNow by rememberUpdatedState(newValue = { dx: Float, dy: Float -> move(dx, dy) })
    val monsterNow by rememberUpdatedState(newValue = { seconds: Float -> updateMonster(seconds) })
    // El monstruo y la captura se actualizan incluso si el joystick está quieto.
    LaunchedEffect(windowFocused, startedAt) {
        if (!windowFocused) return@LaunchedEffect
        var previousFrame = withFrameNanos { it }
        while (true) {
            val frame = withFrameNanos { it }
            val seconds = ((frame - previousFrame) / 1_000_000_000f).coerceIn(0f, 0.05f)
            previousFrame = frame
            val direction = joystickDirection
            if (direction != Offset.Zero) {
                moveNow(direction.x * MOVE_SPEED * seconds, direction.y * MOVE_SPEED * seconds)
            }
            monsterNow(seconds)


// Calcular distancia entre jugador y monstruo
            val personaje = Offset(playerX, playerY)
            val monstruo = Offset(monsterX, monsterY)

            val distancia = (personaje - monstruo).getDistance()

// Comprobar si estan en la misma sala
// y pueden verse sin paredes de por medio


            val mismaSala = roomName == monsterRoomName

            val estadoLatido = when {

                // El monstruo está en otra habitación
                !gameStarted || hasWon || hasLost || !mismaSala -> 0

                // El monstruo nos persigue y está muy cerca
                monsterChasing && distancia < 4f -> 2

                // El monstruo está en nuestra habitación,
                // pero todavía no está lo suficientemente cerca
                else -> 1
            }

// Reproducir latidos según el peligro
            audioJuego.actualizarLatido(estadoLatido)


        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onBack) { Text("← Menú principal") }
        Text("Tiempo: ${formatGameTime(elapsedMillis)}",
            style = MaterialTheme.typography.labelLarge)
    }
    Text(room.title, style = MaterialTheme.typography.headlineMedium, maxLines = 1)
    Text(
        "Objetos: $collectedCount/$TOTAL_OBJECTS · Piso ${room.floor}" +
            if (hasFlashlight) " · Linterna ✓" else "",
        style = MaterialTheme.typography.labelLarge
    )

    val playerColor = MaterialTheme.colorScheme.primary
    val spriteLinterna = ImageBitmap.imageResource(R.drawable.linterna)
    val spritesDecoracion = mapOf(
        R.drawable.librero to ImageBitmap.imageResource(R.drawable.librero),
        R.drawable.mesa to ImageBitmap.imageResource(R.drawable.mesa),
        R.drawable.cajas to ImageBitmap.imageResource(R.drawable.cajas),
        R.drawable.papeles to ImageBitmap.imageResource(R.drawable.papeles),
        R.drawable.sofa to ImageBitmap.imageResource(R.drawable.sofa),
        R.drawable.sillon to ImageBitmap.imageResource(R.drawable.sillon),
        R.drawable.alfombra to ImageBitmap.imageResource(R.drawable.alfombra),
        R.drawable.cama to ImageBitmap.imageResource(R.drawable.cama),
        R.drawable.buro to ImageBitmap.imageResource(R.drawable.buro),
        R.drawable.armario to ImageBitmap.imageResource(R.drawable.armario),
        R.drawable.barril to ImageBitmap.imageResource(R.drawable.barril),
        R.drawable.baul to ImageBitmap.imageResource(R.drawable.baul),
        R.drawable.mesa_trabajo to ImageBitmap.imageResource(R.drawable.mesa_trabajo),
        R.drawable.silla to ImageBitmap.imageResource(R.drawable.silla),
        R.drawable.escombros to ImageBitmap.imageResource(R.drawable.escombros)

    )
    val spriteSuelo = ImageBitmap.imageResource(R.drawable.suelo_piedra)
    val spriteParedHorizontal =
        ImageBitmap.imageResource(R.drawable.pared_horizontal)
    val spriteParedVertical =
        ImageBitmap.imageResource(R.drawable.pared_vertical)
    val spriteAbajo = ImageBitmap.imageResource(R.drawable.personaje_abajo)
    val spriteArriba = ImageBitmap.imageResource(R.drawable.personaje_arriba)
    val spriteIzquierda = ImageBitmap.imageResource(R.drawable.personaje_izquierda)
    val spriteDerecha = ImageBitmap.imageResource(R.drawable.personaje_derecha)
    val monstruoAbajo = ImageBitmap.imageResource(R.drawable.monstruo_abajo)
    val monstruoArriba = ImageBitmap.imageResource(R.drawable.monstruo_arriba)
    val monstruoIzquierda = ImageBitmap.imageResource(R.drawable.monstruo_izquierda)
    val monstruoDerecha = ImageBitmap.imageResource(R.drawable.monstruo_derecha)
    val spritePuerta1 = ImageBitmap.imageResource(R.drawable.puerta1)
    val spritePuerta2 = ImageBitmap.imageResource(R.drawable.puerta2)
    val spritePuerta3 = ImageBitmap.imageResource(R.drawable.puerta3)
    val spritesObjetos = listOf(
        ImageBitmap.imageResource(R.drawable.llave),
        ImageBitmap.imageResource(R.drawable.bateria),
        ImageBitmap.imageResource(R.drawable.nota),
        ImageBitmap.imageResource(R.drawable.engranaje),
        ImageBitmap.imageResource(R.drawable.destornillador),
        ImageBitmap.imageResource(R.drawable.tarjeta),
        ImageBitmap.imageResource(R.drawable.fusible)
    )

    val nearFinalDoor = doorways.any {
        it.kind == PortalKind.FINAL_ACCESS &&
                isNear(
                    Offset(playerX, playerY),
                    it.position,
                    1.7f
                )
    }

// Reproducir sonido al acercarse a la puerta bloqueada
    LaunchedEffect(nearFinalDoor, collectedCount < TOTAL_OBJECTS) {
        if (nearFinalDoor && collectedCount < TOTAL_OBJECTS) {
            audioJuego.puertaCerrada()
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(MAP_WIDTH / MAP_HEIGHT)
                .semantics { contentDescription = "Plano de ${room.title}, piso ${room.floor}" }
        ) {
            val unit = size.width / MAP_WIDTH
            drawRect(Color(0xFF221B20))

            val tamanoBaldosa = (4f * unit)
                .roundToInt()
                .coerceAtLeast(1)

// Dibujar el suelo antes de las paredes y los objetos.
            clipRect {
                var y = 0

                while (y < size.height) {
                    var x = 0

                    while (x < size.width) {
                        drawImage(
                            image = spriteSuelo,
                            dstOffset = IntOffset(x, y),
                            dstSize = IntSize(
                                tamanoBaldosa,
                                tamanoBaldosa
                            ),
                            filterQuality = FilterQuality.None
                        )

                        x += tamanoBaldosa
                    }

                    y += tamanoBaldosa
                }
            }


            val grosorVisual = (0.38f * unit)
                .roundToInt().coerceAtLeast(1)

            val largoBloque = 0.8f * unit

            activeWalls.forEach { wall ->

                val inicio = wall.start * unit
                val final = wall.end * unit

                val horizontal =
                    kotlin.math.abs(inicio.y - final.y) < 0.5f

                val imagen = if (horizontal) {
                    spriteParedHorizontal
                } else {
                    spriteParedVertical
                }

                val desde = if (horizontal) {
                    minOf(inicio.x, final.x)
                } else {
                    minOf(inicio.y, final.y)
                }

                val hasta = if (horizontal) {
                    maxOf(inicio.x, final.x)
                } else {
                    maxOf(inicio.y, final.y)
                }

                val centro = if (horizontal) inicio.y else inicio.x

                var posicion = desde

                while (posicion < hasta - 0.001f) {

                    val siguiente = minOf(
                        posicion + largoBloque,
                        hasta
                    )

                    val inicioPx = posicion.roundToInt()
                    val largoPx = (siguiente.roundToInt() - inicioPx)
                        .coerceAtLeast(1)

                    val offset = if (horizontal) {
                        IntOffset(
                            inicioPx,
                            (centro - grosorVisual / 2f).roundToInt()
                        )
                    } else {
                        IntOffset(
                            (centro - grosorVisual / 2f).roundToInt(),
                            inicioPx
                        )
                    }

                    val dimensiones = if (horizontal) {
                        IntSize(largoPx, grosorVisual)
                    } else {
                        IntSize(grosorVisual, largoPx)
                    }

                    drawImage(
                        image = imagen,
                        dstOffset = offset,
                        dstSize = dimensiones,
                        filterQuality = FilterQuality.None
                    )

                    posicion = siguiente
                }
            }


            decoraciones.filter { it.sala == room }.forEach { objeto ->

                val imagen = spritesDecoracion.getValue(objeto.imagen)
                val centro = objeto.posicion * unit

                val ancho = (objeto.ancho * unit)
                    .roundToInt().coerceAtLeast(1)

                val alto = (objeto.alto * unit)
                    .roundToInt().coerceAtLeast(1)

                drawImage(
                    image = imagen,
                    dstOffset = IntOffset(
                        (centro.x - ancho / 2f).roundToInt(),
                        (centro.y - alto / 2f).roundToInt()
                    ),
                    dstSize = IntSize(ancho, alto),
                    filterQuality = FilterQuality.None
                )
            }


            doorways.forEach { doorway ->

                val centro = doorway.position * unit

                val imagen = when (doorway.kind) {
                    PortalKind.ROOM -> spritePuerta1
                    PortalKind.STAIRS -> spritePuerta3
                    PortalKind.FINAL_ACCESS -> spritePuerta2
                }

                // Puerta un poco más grande.
                val ancho = (1.8f * unit)
                    .roundToInt()
                    .coerceAtLeast(1)

                val alto = (
                        ancho * imagen.height.toFloat() /
                                imagen.width
                        ).roundToInt().coerceAtLeast(1)

                drawImage(
                    image = imagen,
                    dstOffset = IntOffset(
                        (centro.x - ancho / 2f).roundToInt(),
                        (centro.y - alto / 2f).roundToInt()
                    ),
                    dstSize = IntSize(ancho, alto),
                    filterQuality = FilterQuality.None
                )
            }


            if (room == Room.FINAL_UPPER) {
                drawCircle(Color(0xFFFF7B22), 0.32f * unit, finalMarker * unit)
            }

            collectibles.forEach { item ->
                if (item.room == room &&
                    (collectedMask and (1 shl item.id)) == 0
                ) {
                    val imagen = spritesObjetos[item.id]
                    val centro = item.position * unit

                    val ancho = (0.85f * unit)
                        .roundToInt()
                        .coerceAtLeast(1)

                    val alto = (
                            ancho * imagen.height.toFloat() /
                                    imagen.width
                            ).roundToInt().coerceAtLeast(1)

                    val radioAura = 0.55f * unit

                    // Aura roja difuminada
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0x88FF3B3B),
                                Color(0x44FF3B3B),
                                Color(0x10FF3B3B),
                                Color.Transparent
                            ),
                            center = centro,
                            radius = radioAura
                        ),
                        radius = radioAura,
                        center = centro
                    )

                    // Núcleo un poco más intenso
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0x55FF5A5A),
                                Color.Transparent
                            ),
                            center = centro,
                            radius = 0.45f * unit
                        ),
                        radius = 0.45f * unit,
                        center = centro
                    )

                    drawImage(
                        image = imagen,
                        dstOffset = IntOffset(
                            (centro.x - ancho / 2f).roundToInt(),
                            (centro.y - alto / 2f).roundToInt()
                        ),
                        dstSize = IntSize(ancho, alto),
                        filterQuality = FilterQuality.None
                    )
                }
            }


            if (room == Room.MAIN && !hasFlashlight) {
                val centro = flashlight * unit

                val ancho = (0.9f * unit).roundToInt()
                    .coerceAtLeast(1)

                val alto = (
                        ancho * spriteLinterna.height.toFloat() /
                                spriteLinterna.width
                        ).roundToInt().coerceAtLeast(1)

                drawImage(
                    image = spriteLinterna,
                    dstOffset = IntOffset(
                        (centro.x - ancho / 2f).roundToInt(),
                        (centro.y - alto / 2f).roundToInt()
                    ),
                    dstSize = IntSize(ancho, alto),
                    filterQuality = FilterQuality.None
                )
            }


            if (room == monsterRoom) {

                val imagenMonstruo = when (monsterFacing) {
                    "arriba" -> monstruoArriba
                    "izquierda" -> monstruoIzquierda
                    "derecha" -> monstruoDerecha
                    else -> monstruoAbajo
                }

                val centro = Offset(monsterX, monsterY) * unit

                val ancho = (1.0f * unit)
                    .roundToInt()
                    .coerceAtLeast(1)

                val alto = (
                        ancho * imagenMonstruo.height.toFloat() /
                                imagenMonstruo.width
                        ).roundToInt().coerceAtLeast(1)

                drawImage(
                    image = imagenMonstruo,
                    dstOffset = IntOffset(
                        (centro.x - ancho / 2f).roundToInt(),
                        (centro.y - alto / 2f).roundToInt()
                    ),
                    dstSize = IntSize(ancho, alto),
                    filterQuality = FilterQuality.None
                )
            }



            if (hasFlashlight) {

                val origen = Offset(playerX, playerY) * unit

                val direccion = when (playerFacing) {
                    "arriba" -> Offset(0f, -1f)
                    "abajo" -> Offset(0f, 1f)
                    "izquierda" -> Offset(-1f, 0f)
                    else -> Offset(1f, 0f)
                }

                val lateral = Offset(-direccion.y, direccion.x)

                val inicio = origen + direccion * (0.3f * unit)
                val fin = origen + direccion * (2.0f * unit)
                val ancho = 1.2f * unit

                // Forma del haz de la linterna.
                val haz = Path().apply {
                    moveTo(inicio.x, inicio.y)

                    val lado1 = fin + lateral * ancho
                    val lado2 = fin - lateral * ancho

                    lineTo(lado1.x, lado1.y)
                    lineTo(lado2.x, lado2.y)
                    close()
                }

                // Luz tenue que se desvanece a distancia.
                drawPath(
                    path = haz,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0x70FFF4C4),
                            Color(0x08FFF4C4)
                        ),
                        start = inicio,
                        end = fin
                    )
                )

                // Difuminación en la parte delantera.
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x45FFF1BC),
                            Color(0x15FFE7A0),
                            Color.Transparent
                        ),
                        center = fin,
                        radius = 1.6f * unit
                    ),
                    radius = 1.6f * unit,
                    center = fin
                )
            }

            val imagenJugador = when (playerFacing) {
                "arriba" -> spriteArriba
                "izquierda" -> spriteIzquierda
                "derecha" -> spriteDerecha
                else -> spriteAbajo
            }

            val centroJugador = Offset(playerX, playerY) * unit
            val tamanoJugador = (1.2f * unit)
                .roundToInt()
                .coerceAtLeast(1)

            drawImage(
                image = imagenJugador,
                dstOffset = IntOffset(
                    (centroJugador.x - tamanoJugador / 2f).roundToInt(),
                    (centroJugador.y - tamanoJugador / 2f).roundToInt()
                ),
                dstSize = IntSize(tamanoJugador, tamanoJugador),
                filterQuality = FilterQuality.None
            )

        }
    
        if (room == monsterRoom) {
            Surface(
                modifier = Modifier.align(Alignment.TopCenter).padding(8.dp),
                color = Color(0xDD221B20),
                contentColor = if (monsterChasing) Color(0xFFFF7474) else Color(0xFFE8CBD0),
                shape = MaterialTheme.shapes.small
            ) {
                Text(if (collectedCount == TOTAL_OBJECTS) "¡Sabe dónde estás!"
                    else if (monsterChasing) "¡Te está persiguiendo!" else "Monstruo: merodeando",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelMedium)
            }
        }
        // Sobre el mapa: el aviso no desplaza el joystick ni cambia el tamaño del plano.
        if (nearFinalDoor) {
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
                color = if (collectedCount < TOTAL_OBJECTS) Color(0xFF6B1830) else Color(0xFF184C39),
                contentColor = Color.White,
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = if (collectedCount < TOTAL_OBJECTS)
                        "Necesitas los 8 objetos para escapar" else "Salida desbloqueada",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        VirtualJoystick(
            enabled = windowFocused,
            onDirectionChange = { joystickDirection = it },
            onNudge = { direction -> move(direction.x * MOVE_STEP, direction.y * MOVE_STEP) }
        )
    }
}

// Devuelve una dirección unitaria: inclinar en diagonal no aumenta la velocidad.
private fun joystickDirection(displacement: Offset): Offset {
    val distance = displacement.getDistance()
    return if (distance <= JOYSTICK_DEAD_ZONE) Offset.Zero else displacement / distance
}

@Composable
private fun VirtualJoystick(
    enabled: Boolean,
    onDirectionChange: (Offset) -> Unit,
    onNudge: (Offset) -> Unit
) {
    var thumb by remember { mutableStateOf(Offset.Zero) }
    val directionNow by rememberUpdatedState(onDirectionChange)
    val nudgeNow by rememberUpdatedState(onNudge)
    val knobColor = MaterialTheme.colorScheme.primary
    val baseColor = MaterialTheme.colorScheme.surfaceVariant

    Canvas(
        modifier = Modifier.size(144.dp)
            .semantics {
                contentDescription = "Joystick de movimiento. Arrastra para caminar y suelta para detenerte."
                customActions = listOf(
                    CustomAccessibilityAction("Mover arriba") {
                        if (enabled) nudgeNow(Offset(0f, -1f))
                        enabled
                    },
                    CustomAccessibilityAction("Mover abajo") {
                        if (enabled) nudgeNow(Offset(0f, 1f))
                        enabled
                    },
                    CustomAccessibilityAction("Mover izquierda") {
                        if (enabled) nudgeNow(Offset(-1f, 0f))
                        enabled
                    },
                    CustomAccessibilityAction("Mover derecha") {
                        if (enabled) nudgeNow(Offset(1f, 0f))
                        enabled
                    }
                )
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                val center = Offset(size.width / 2f, size.height / 2f)
                val travel = minOf(size.width, size.height) * 0.28f
                // Solo se sigue el dedo que inició el gesto, aunque otro toque la pantalla.
                fun update(position: Offset) {
                    val displacement = (position - center) / travel
                    val distance = displacement.getDistance()
                    thumb = if (distance > 1f) displacement / distance else displacement
                    directionNow(joystickDirection(thumb))
                }
                try {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        update(down.position)
                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) {
                                    change.consume()
                                    break
                                }
                                change.consume()
                                update(change.position)
                            }
                        } finally {
                            thumb = Offset.Zero
                            directionNow(Offset.Zero)
                        }
                    }
                } finally {
                    // También al cancelar el gesto, perder el foco o abandonar la pantalla.
                    thumb = Offset.Zero
                    directionNow(Offset.Zero)
                }
            }
    ) {
        val radius = size.minDimension * 0.47f
        val knobRadius = size.minDimension * 0.19f
        val travel = radius - knobRadius
        drawCircle(baseColor.copy(alpha = 0.75f), radius)
        drawCircle(knobColor.copy(alpha = 0.4f), radius, style = Stroke(2.dp.toPx()))
        drawCircle(knobColor.copy(alpha = 0.15f), travel * JOYSTICK_DEAD_ZONE)
        drawCircle(knobColor, knobRadius, center + thumb * travel)
        drawCircle(Color.White.copy(alpha = 0.35f), 3.dp.toPx(), center + thumb * travel)
    }
}
