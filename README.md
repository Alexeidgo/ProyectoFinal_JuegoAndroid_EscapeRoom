# Room 404: Escape — prototipo de interfaz

Prototipo creado por el equipo 3 en base a la primera entrega, para la conclusión posterior del mismo trabajo.


## Alcance de esta entrega

- **Menú:** Room 404: Escape, ilustración del pasillo, Jugar, High Scores y Arte conceptual. Jugar abre una introducción que informa que la partida se implementará después.
- **High Scores:** lista ficticia ordenada por puntuación descendente, explícitamente marcada como demostración sin Firebase.
- **Arte conceptual:** selección entre habitación, presencia y objetos; cambia la ilustración y su descripción.
- Dos paletas oscuras seleccionables: carmesí y ceniza.
- Botón Atrás de Android, contenido desplazable y estado preservado mediante SavedStateHandle al recrear la actividad con estado guardado.

No incluye todavía mapa jugable, joystick, personaje animado, persecución, recolección, colisiones, autenticación, persistencia permanente ni Firebase. La intención es presentar la identidad del juego y su navegación. No hay laboratorio ni acertijo de claves.

## Relación con la rúbrica

| Criterio | Implementación | Límite |
| --- | --- | --- |
| Compose, tema, color y fuentes | `MainActivity.kt`, `ui/Theme.kt`; dos paletas propias y familias serif/sans/monoespaciada del sistema | Diseño de prototipo con scroll para pantallas pequeñas |
| Imágenes | `corridor.xml`, `entity.xml`, `objects.xml`;  Bocetos provisionales; no son los sprites finales ni copias del arte del documento |
| Operaciones internas | Ordenamiento de ranking, selección de concepto y navegación | Operaciones de interfaz; las mecánicas del juego siguen pendientes |
| MVVM | Modelos/repositorio y regla de ranking en `data`; estado/acciones en `EscapeViewModel`; vistas Compose | Repositorio local sustituible al añadir Firebase |
| Pruebas | 2 unitarias y 2 de instrumentación | Incluidas, todavía no ejecutadas en un entorno Android |


## Archivos principales

- `MainActivity.kt`: vistas Compose del menú, ranking, galería e introducción modal.
- `ui/EscapeViewModel.kt`: pantalla actual, paleta, selección y diálogo. Los eventos cambian el estado y Compose recompone la interfaz.
- `data/RoomRepository.kt`: datos ficticios, descripciones y regla de ordenamiento de puntuaciones.
- `ui/Theme.kt`: paletas, tipografía y Material 3.
- `res/drawable/`: arte vectorial editable.


## Pruebas

Windows:
```bat
gradlew.bat testDebugUnitTest
gradlew.bat connectedDebugAndroidTest
```
macOS/Linux:
```sh
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
```
La instrumentación requiere teléfono o emulador encendido. `RankScoresTest` comprueba orden, empates, lista vacía e inmutabilidad de origen. `EscapeFlowTest` comprueba menú/ranking/regreso y selección de arte de la presencia.

Comprobaciones manuales: tres pantallas, Jugar/cerrar introducción, botón Atrás, cambio de paleta, rotación, texto grande y orientación horizontal. Las pantallas se adaptan mediante scroll; todavía no se ha diseñado el HUD horizontal del juego.

## Verificación de esta entrega

XML, estructura, recursos y contenido del ZIP revisados. No hay APK ni captura de ejecución real: el entorno no dispone de Android SDK/emulador y el intento previo de compilación quedó bloqueado al resolver `services.gradle.org`. La versión corregida y sus pruebas requieren compilación/ejecución en Android Studio.

## Siguiente etapa

Integrar autenticación y puntuaciones con Firebase; crear el mapa 2D y joystick; implementar movimiento, colisiones y objetos; añadir perseguidor, fase final y salida. Mantener las operaciones de juego fuera de los composables para extender MVVM.
