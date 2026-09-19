// Nombre del paquete para identificar la aplicación dentro del sistema Android
package com.example.iudigitalradio

// Importaciones de librerías nativas del sistema operativo Android (Contexto, Imágenes, Hardware)
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.Manifest

// Importaciones de la actividad principal y lanzadores de contratos (Cámara y Permisos)
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts

// Importaciones de Jetpack Compose para diseño visual, contenedores e imágenes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.grid.GridCells
//import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
//import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
// Importaciones de Material Design 3 (Botones, Tarjetas, Temas)
import androidx.compose.material3.*

// Importaciones para manejo del estado (remember, mutableStateOf, efectos de ciclo de vida)
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable

// Importaciones para alineación, colores, dimensiones y vista previa
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Importaciones de Media3 ExoPlayer para la reproducción de audio en línea
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

// =========================================================================
// CONFIGURACIÓN DE COLORES GLOBALES Y DATOS DE PRUEBA
// =========================================================================

// Color de fondo oscuro principal de la aplicación (Formato Hexadecimal ARGB)
val DarkBackground = Color(0xFF101014)

// Color para las tarjetas contenedoras de información
val DarkCard = Color(0xFF1C1C24)

// Color morado de acento para destacar botones activos y detalles
val PurpleAccent = Color(0xFF6C5CE7)

// Color gris para textos secundarios e informativos
val TextGray = Color(0xFFA0A0AB)

// Lista de emisoras simuladas para el catálogo con ID, Nombre, Género y Enlace de Streaming
val sampleStations = listOf(
    Station(1, "Caracol Radio", "100.9 FM / 810 AM • Noticias", "https://playerservices.streamtheworld.com/api/livestream-redirect/CARACOL_RADIOAAC.aac"),
    Station(2, "Olímpica Stereo", "104.9 FM", "https://playerservices.streamtheworld.com/api/livestream-redirect/OLP_MEDELLINAAC.aac"),
    Station(3, "Mix (Medellín)", "Live • Rock", "https://playerservices.streamtheworld.com/api/livestream-redirect/MIX_MEDELLINAAC.aac"),
    Station(4, "Los 40 Principales", " Popular", "https://playerservices.streamtheworld.com/api/livestream-redirect/LOS40_COLOMBIAAAC_SC"),
    Station(5, "Salsa Capital", "SALSA", "https://stream.integracionvirtual.com/proxy/capitalsalsa?mp=/stream"),
    Station(6, "Baladas Rs", "BALADAS", "https://stream.zeno.fm/fxzt1r5rp2zuv")
)

// =========================================================================
// ACTIVIDAD PRINCIPAL (PUNTO DE ENTRADA DE LA APLICACIÓN)
// =========================================================================

// Clase principal que hereda de ComponentActivity para usar Jetpack Compose
class MainActivity : ComponentActivity() {

    // Método onCreate: primer método ejecutado al iniciar la pantalla
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // setContent: conecta la lógica de Kotlin con la interfaz gráfica declarativa
        setContent {
            // Aplica el tema oscuro personalizado a toda la aplicación
            MaterialTheme(colorScheme = darkColorScheme(background = DarkBackground)) {
                // Surface: contenedor base que pinta el fondo de la pantalla
                Surface(
                    modifier = Modifier.fillMaxSize(), // Ocupa el 100% del alto y ancho de la pantalla
                    color = DarkBackground             // Asigna el color de fondo oscuro
                ) {
                    // Llama al componente principal de la interfaz
                    RadioAppUI()
                }
            }
        }
    }
}

// =========================================================================
// COMPONENTE PRINCIPAL (GESTIÓN DE ESTADO Y ESTRUCTURA)
// =========================================================================

// @Composable: indica que esta función construye elementos visuales en pantalla
@Composable
fun RadioAppUI() {
    // Obtiene el contexto actual del sistema operativo para acceder a servicios nativos
    val context = LocalContext.current
    // Convierte el contexto genérico en ComponentActivity para poder cerrar la aplicación nativamente
    val activity = context as? ComponentActivity

    // Detecta si la pantalla se está dibujando en el editor de vista previa de Android Studio
    val isPreview = LocalInspectionMode.current

    // remember: mantiene en memoria la instancia del reproductor de audio ExoPlayer
    val exoPlayer = remember(context) {
        // Si NO está en modo vista previa, crea el reproductor real; si está en vista previa, asigna null
        if (!isPreview) ExoPlayer.Builder(context).build() else null
    }

    // DisposableEffect: gestiona el ciclo de vida de ExoPlayer para liberar la memoria al cerrar la app
    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer?.release() // Destruye el reproductor al salir para liberar memoria RAM
        }
    }

    // Variable de estado para guardar en memoria la imagen tomada por la cámara
    var profileBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Lanzador 1: Contrato para capturar la foto usando la cámara nativa
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview() // Genera una imagen pequeña de vista previa
    ) { bitmap ->
        // Si la foto fue tomada correctamente, actualiza la variable de estado
        if (bitmap != null) profileBitmap = bitmap
    }

    // Lanzador 2: Contrato para solicitar el permiso de uso de la cámara al usuario
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Si el usuario concede el permiso en la ventana emergente, intenta abrir la cámara
        if (isGranted) {
            try {
                cameraLauncher.launch(null) // Ejecuta la cámara
            } catch (e: Exception) {
                e.printStackTrace() // Captura errores en consola para evitar cierres
            }
        }
    }

    // Estado guardable (rememberSaveable): conserva el ID de la emisora elegida tras girar la pantalla
    var selectedStationId by rememberSaveable { mutableStateOf(sampleStations[0].id) }

    // Estado guardable: controla si la música se encuentra en reproducción o pausada
    var isPlaying by rememberSaveable { mutableStateOf(false) }

    // Estado guardable para controlar si el reproductor está silenciado
    var isMuted by rememberSaveable { mutableStateOf(false) }

    // LaunchedEffect para actualizar el volumen del reproductor cuando cambia el estado isMuted
    LaunchedEffect(isMuted) {
        exoPlayer?.volume = if (isMuted) 0f else 1f
    }

    // Busca en la lista de emisoras cuál coincide con el ID seleccionado actualmente
    val currentStation = sampleStations.find { it.id == selectedStationId } ?: sampleStations[0]

    // LaunchedEffect: reacciona cada vez que el usuario cambia la emisora seleccionada
    LaunchedEffect(selectedStationId) {
        // Solo carga el audio si estamos en un dispositivo real y el reproductor existe
        if (!isPreview && exoPlayer != null) {
            val mediaItem = MediaItem.fromUri(currentStation.streamUrl) // Convierte la URL a item ejecutable
            exoPlayer.setMediaItem(mediaItem)                            // Asigna la emisora al reproductor
            exoPlayer.prepare()                                          // Prepara el búfer de reproducción
            if (isPlaying) exoPlayer.play()                              // Inicia el sonido si el estado es 'reproduciendo'
        }
    }

    // Box: contenedor superpuesto (permite colocar la barra inferior encima del contenido)
    Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {

        // Column: organiza sus elementos hijos verticalmente uno debajo del otro
        Column(
            modifier = Modifier
                .fillMaxSize()                                   // Ocupa toda la pantalla
                .padding(horizontal = 16.dp, vertical = 12.dp)  // Márgenes laterales y verticales
                .padding(bottom = 80.dp)                         // Espacio libre para no tapar con la barra inferior
        ) {
            // Cabecera superior de perfil
            ProfileHeader(
                bitmap = profileBitmap,
                onTakePhoto = {
                    if (!isPreview) triggerVibration(context) // Ejecuta vibración háptica al pulsar
                    // Solicita el permiso de cámara antes de abrirla
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            )

            // Separador vertical de 16 píxeles de densidad
            Spacer(modifier = Modifier.height(16.dp))

            // Tarjeta central del reproductor principal
            PlayerCardSection(
                station = currentStation,
                isPlaying = isPlaying,
                isMuted = isMuted,
                onPlayToggle = {
                    if (!isPreview) triggerVibration(context) // Vibración háptica
                    // Alterna entre reproducir y pausar el audio
                    if (isPlaying) {
                        exoPlayer?.pause()
                        isPlaying = false
                    } else {
                        exoPlayer?.play()
                        isPlaying = true
                    }
                },
                onMuteToggle = {
                    isMuted = !isMuted
                }
            )

            // Separador vertical de 20 píxeles de densidad
            Spacer(modifier = Modifier.height(20.dp))

            // Títulos de la sección del catálogo
            Text(
                text = "Emisoras",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Tus estaciones favoritas",
                color = TextGray,
                fontSize = 14.sp
            )

            // Separador vertical de 12 píxeles de densidad
            Spacer(modifier = Modifier.height(12.dp))

            // Cuadrícula con la lista de emisoras disponibles
            CatalogList(
                selectedStationId = selectedStationId,
                onStationSelect = { station ->
                    if (!isPreview) triggerVibration(context) // Vibración háptica
                    selectedStationId = station.id           // Cambia la emisora activa
                    isPlaying = true                         // Activa la reproducción automática
                }
            )
        }

        // Barra de navegación ubicada en la parte inferior sobrepuesta
        BottomNavigationBar(
            isPlaying = isPlaying,
            onPlayClick = {
                if (!isPreview) triggerVibration(context) // Vibración háptica
                if (isPlaying) {
                    exoPlayer?.pause()
                    isPlaying = false
                } else {
                    exoPlayer?.play()
                    isPlaying = true
                }
            },
            onExitClick = {
                // Ejecuta respuesta háptica al presionar el botón de Salir
                if (!isPreview) triggerVibration(context)

                // 1. Detiene la transmisión de audio para liberar el reproductor
                exoPlayer?.stop()

                // 2. Finaliza la actividad actual y remueve la app del menú de multitarea
                activity?.finishAndRemoveTask()
            },
            modifier = Modifier.align(Alignment.BottomCenter) // Alinea la barra al fondo central
        )
    }
}

// =========================================================================
// SECCIÓN 1: CABECERA Y FOTO DE PERFIL
// =========================================================================

@Composable
fun ProfileHeader(
    bitmap: Bitmap?,             // Foto tomada (si existe)
    onTakePhoto: () -> Unit      // Evento al presionar el botón de foto
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Columna izquierda: Avatar del usuario
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Foto de perfil",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF3A3A48)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "👤", fontSize = 24.sp)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Foto", color = TextGray, fontSize = 11.sp)
            }

            // Columna central: Nombre y Estado del oyente
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Oyente",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Estado: Conectado",
                    color = TextGray,
                    fontSize = 12.sp
                )
            }

            // Botón derecho: Acción de la cámara con fondo morado
            IconButton(
                onClick = onTakePhoto,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(PurpleAccent)
            ) {
                Text(text = "📷", fontSize = 20.sp)
            }
        }
    }
}
// =========================================================================
// SECCIÓN 2: TARJETA DEL REPRODUCTOR EN VIVO (DISEÑO RADIO)
// =========================================================================
// @Composable: dibuja la tarjeta del reproductor central con sus controles interactivos
@Composable
fun PlayerCardSection(
    station: Station,             // Emisora actual en pantalla
    isPlaying: Boolean,           // Estado de reproducción (reproduciendo / pausado)
    isMuted: Boolean,             // Estado de silencio (muteado / con sonido)
    onPlayToggle: () -> Unit,      // Acción al presionar el botón de reproducción/pausa
    onMuteToggle: () -> Unit       // Acción al presionar el botón de silencio (mute)
) {
    // Obtiene el contexto para ejecutar la vibración háptica al presionar el botón de Mute
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Fila superior: Botón Play, Datos de la emisora, Badge "EN VIVO" y Botón Mute
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón principal de reproducción
                Surface(
                    shape = CircleShape,
                    color = PurpleAccent,
                    modifier = Modifier
                        .size(56.dp)
                        .clickable { onPlayToggle() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (isPlaying) "❚❚" else "▶",
                            color = Color.White,
                            fontSize = 22.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Información textual de la emisora
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = station.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = station.frequency,
                        color = TextGray,
                        fontSize = 13.sp
                    )
                }

                // Indicador dinámico de EN VIVO / PAUSA
                Surface(
                    color = if (isPlaying) Color(0xFFE53935) else Color(0xFF3E3E4A),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPlaying) "EN VIVO" else "PAUSA",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Espaciador horizontal entre el indicador EN VIVO y el botón de silencio
                Spacer(modifier = Modifier.width(8.dp))

                // Botón interactivo de Mute / Silencio
                IconButton(
                    onClick = {
                        if (!isPreview) triggerVibration(context) // Ejecuta la vibración háptica al pulsar
                        onMuteToggle()                           // Invoca la acción de alternar silencio
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isMuted) Color(0xFFE53935) else Color(0xFF252530)) // Destaca en rojo si está muteado
                ) {
                    Text(
                        text = if (isMuted) "🔇" else "🔊",       // Ícono dinámico según el estado del volumen
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Fila inferior: Estado de transmisión y Onda de audio
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF252530))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isMuted) "🔇 Audio silenciado" else if (isPlaying) "📡 Transmitiendo señal..." else "⏸️ En espera",
                        color = TextGray,
                        fontSize = 12.sp
                    )
                }
                Text(
                    text = if (isPlaying && !isMuted) "ııılıılııııılıılııı" else "─────────────",
                    color = if (isPlaying && !isMuted) PurpleAccent else TextGray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
// =========================================================================
// SECCIÓN 3: CATÁLOGO DE EMISORAS EN CUADRÍCULA (LAZYCOLUMN)
// =========================================================================
// @Composable: dibuja el catálogo optimizado en formato de lista deslizable vertical

@Composable
fun CatalogList(
    selectedStationId: Int,             // id de la emisora seleccionada actualmente
    onStationSelect: (Station) -> Unit  // Acción a ejecutar al presionar una emisora
) {
    // LazyColumn: renderiza eficientemente los elementos en una lista vertical
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp), // Espaciado vertical entre tarjetas
        modifier = Modifier.fillMaxWidth()                  // Ocupa todo el ancho disponible
    ) {
        // Recorre la lista de emisoras y genera una tarjeta para cada una
        items(sampleStations) { station ->
            // Determina si la emisora actual de la iteración es la que está seleccionada
            val isSelected = station.id == selectedStationId

            // Tarjeta contenedora de la emisora
            Card(
                colors = CardDefaults.cardColors(
                    // Cambia el color de fondo si la emisora está activa
                    containerColor = if (isSelected) Color(0xFF2B2B36) else DarkCard
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onStationSelect(station) } // Asigna la emisora al presionar
            ) {
                // Fila principal: Organiza el ícono, los textos y el indicador horizontalmente
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically // Alinea los elementos al centro vertical
                ) {
                    // Ícono representativo de la emisora
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF2C2C38)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "📻", fontSize = 18.sp)
                    }

                    // Separador horizontal de 12 píxeles de densidad
                    Spacer(modifier = Modifier.width(12.dp))

                    // Columna central: Nombre y frecuencia/género de la emisora
                    Column(modifier = Modifier.weight(1f)) { // Ocupa el espacio restante disponible
                        Text(
                            text = station.name,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = station.frequency,
                            color = TextGray,
                            fontSize = 12.sp
                        )
                    }

                    // Indicador visual opcional si la emisora es la que está reproduciéndose
                    if (isSelected) {
                        Text(text = "▶", color = PurpleAccent, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}
// =========================================================================
// SECCIÓN 4: BARRA DE NAVEGACIÓN FLOTANTE INFERIOR
// =========================================================================

@Composable
fun BottomNavigationBar(
    isPlaying: Boolean,           // Estado de reproducción
    onPlayClick: () -> Unit,      // Acción al pulsar el botón central
    onExitClick: () -> Unit,      // Acción a ejecutar al presionar el botón de Salir
    modifier: Modifier = Modifier
) {
    // Surface: barra contenedora fija en la parte inferior
    Surface(
        color = DarkCard,
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Opción Salir / Apagar
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onExitClick() }// Invoca el evento de salida recibido por parámetro
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "\uD83D\uDCF4 ", fontSize = 16.sp)
                Text(text = "Cerrar App", color = Color.White, fontSize = 14.sp)
            }

            // Botón Flotante Central "Reproducir / Pausa"
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = CircleShape,
                    color = PurpleAccent,
                    modifier = Modifier
                        .size(48.dp)
                        .clickable { onPlayClick() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = if (isPlaying) "❚❚" else "▶", color = Color.White, fontSize = 18.sp)
                    }
                }
                Text(text = "Reproducir", color = TextGray, fontSize = 10.sp)
            }

            // Opción Buscar
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🔍 ", fontSize = 16.sp)
                Text(text = "Buscar", color = Color.White, fontSize = 14.sp)
            }
        }
    }
}

// =========================================================================
// VISTA PREVIA PARA EL EDITOR DE ANDROID STUDIO
// =========================================================================

// Permite visualizar el diseño dentro del panel Split/Design de Android Studio sin ejecutar la app
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun RadioAppPreview() {
    MaterialTheme(colorScheme = darkColorScheme(background = DarkBackground)) {
        RadioAppUI()
    }
}

// =========================================================================
// SERVICIO NATIVO DE VIBRACIÓN HÁPTICA (HARDWARE)
// =========================================================================

// Función que activa el motor de vibración del teléfono durante 50 milisegundos
fun triggerVibration(context: Context) {
    // Verifica si la versión de Android es Android 12 (API 31/S) o superior
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        val vibrator = vibratorManager.defaultVibrator
        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        // Compatibilidad para versiones anteriores a Android 12
        @Suppress("DEPRECATION")
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Para Android 8.0 (API 26) hasta Android 11
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            // Para versiones legadas antiguas
            @Suppress("DEPRECATION")
            vibrator.vibrate(50)
        }
    }
}