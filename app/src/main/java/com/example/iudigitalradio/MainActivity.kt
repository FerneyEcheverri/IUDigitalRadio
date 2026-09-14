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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

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
    Station(1, "Logo Emi 1", "Live • Pop", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"),
    Station(2, "Logo Emi 2", "Live • Noticias", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"),
    Station(3, "Logo Emi 3", "Live • Rock", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3"),
    Station(4, "Logo Emi 4", "Live • Jazz", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3")
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
            CatalogGrid(
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
    // Row: distribuye sus elementos horizontalmente en una misma fila
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween, // Separa los extremos de la fila
        verticalAlignment = Alignment.CenterVertically    // Centra el contenido verticalmente
    ) {
        // Columna izquierda: foto redonda de perfil
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (bitmap != null) {
                // Muestra la imagen capturada por el usuario
                Image(
                    bitmap = bitmap.asImageBitmap(),     // Convierte el mapa de bits a imagen de Compose
                    contentDescription = "Foto de perfil",
                    modifier = Modifier
                        .size(56.dp)                      // Tamaño de 56x56 dp
                        .clip(CircleShape),               // Recorta la imagen en forma circular
                    contentScale = ContentScale.Crop      // Escala la foto rellenando el círculo
                )
            } else {
                // Muestra un círculo gris por defecto si no hay foto cargada
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.Gray)
                )
            }
            // Etiqueta de texto debajo de la imagen
            Text(text = "Foto", color = TextGray, fontSize = 12.sp)
        }

        // Botón derecho para solicitar permiso y abrir la cámara
        Button(
            onClick = onTakePhoto,
            colors = ButtonDefaults.buttonColors(containerColor = DarkCard), // Fondo de tarjeta oscura
            shape = RoundedCornerShape(24.dp)                                // Bordes redondeados
        ) {
            Text(text = "📅 📷 Registro Jornal • Foto", color = Color.White)
        }
    }
}

// =========================================================================
// SECCIÓN 2: TARJETA DEL REPRODUCTOR EN VIVO
// =========================================================================

@Composable
fun PlayerCardSection(
    station: Station,             // Emisora actual en pantalla
    isPlaying: Boolean,           // Estado de reproducción
    onPlayToggle: () -> Unit      // Acción de reproducir/pausar
) {
    // Card: contenedor con estilo de tarjeta elevada
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Fila superior: botón de reproducción grande e información de la emisora
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Botón circular morado
                Surface(
                    shape = CircleShape,
                    color = PurpleAccent,
                    modifier = Modifier
                        .size(64.dp)
                        .clickable { onPlayToggle() } // Escucha el toque del usuario
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        // Cambia el ícono entre Pausa y Reproducción según el estado
                        Text(
                            text = if (isPlaying) "❚❚" else "▶",
                            color = Color.White,
                            fontSize = 24.sp
                        )
                    }
                }

                // Información textual de la emisora activa
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${station.name} ((•))",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ahora en vivo • Radio Emi",
                        color = TextGray,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Fila inferior: minireproductor con onda de audio decorativa
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón pequeño de control secundario
                IconButton(
                    onClick = onPlayToggle,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF2C2C36))
                ) {
                    Text(text = if (isPlaying) "❚❚" else "▶", color = Color.White)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = "00:12 / 02:45", color = TextGray, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    // Representación gráfica simulada de la onda de sonido
                    Text(text = "ııılıılııııılıılııııılııııılıı", color = PurpleAccent, fontSize = 14.sp)
                }
            }
        }
    }
}

// =========================================================================
// SECCIÓN 3: CATÁLOGO DE EMISORAS EN CUADRÍCULA
// =========================================================================

@Composable
fun CatalogGrid(
    selectedStationId: Int,                  // ID de la estación seleccionada actualmente
    onStationSelect: (Station) -> Unit       // Acción al hacer clic sobre una tarjeta de estación
) {
    // LazyVerticalGrid: dibuja eficientemente elementos en formato de cuadrícula (2 columnas)
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),                         // Define exactamente 2 columnas
        horizontalArrangement = Arrangement.spacedBy(12.dp),  // Espaciado entre columnas
        verticalArrangement = Arrangement.spacedBy(12.dp),    // Espaciado entre filas
        modifier = Modifier.fillMaxWidth()
    ) {
        // Mapea la lista de emisoras a tarjetas individuales
        items(sampleStations) { station ->
            // Evalúa si la estación de esta tarjeta es la activa para cambiar su color de fondo
            val isSelected = station.id == selectedStationId
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) Color(0xFF2B2B36) else DarkCard
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clickable { onStationSelect(station) } // Asigna la emisora seleccionada
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Ícono cuadrado distintivo de la emisora
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF2C2C38)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "EMi", color = Color(0xFF8E8E9A), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    // Nombre de la emisora
                    Text(
                        text = station.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    // Categoría o género musical
                    Text(
                        text = station.frequency,
                        color = TextGray,
                        fontSize = 11.sp
                    )
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
            // Opción Salir
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "📁 ", fontSize = 16.sp)
                Text(text = "Salir", color = Color.White, fontSize = 14.sp)
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