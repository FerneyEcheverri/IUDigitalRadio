# 📻 Proyecto: IU Digital Radio

Este repositorio contiene el código fuente de la aplicación **"IU Digital Radio"**.

Este proyecto fue desarrollado **en equipo** como parte de la **Evidencia de Aprendizaje 3** para el curso de Desarrollo Móvil de la **Institución Universitaria Digital de Antioquia**.

El objetivo principal del proyecto fue poner en práctica los conocimientos adquiridos sobre **Jetpack Compose**, manejo de estados, reproducción de audio y utilización del hardware del dispositivo móvil.

## 📝 ¿De qué trata la aplicación?

**IU Digital Radio** es un reproductor de radio por *streaming* que permite seleccionar y reproducir diferentes emisoras desde un dispositivo Android.

Para el desarrollo de la aplicación, el equipo implementó los requerimientos establecidos en la guía:

* **Cero XML:** Toda la interfaz gráfica fue desarrollada utilizando **Jetpack Compose**, mediante programación declarativa en Kotlin y java con componentes como `Column`, `Row`, `Card`, entre otros.
* **Cámara integrada:** En la parte superior de la aplicación se encuentra un botón que solicita los permisos necesarios para utilizar la cámara, permitiendo tomar una fotografía y establecerla como imagen de perfil.
* **Vibración (Háptica):** Al interactuar con los botones de **Play, Pausa y Silencio**, el dispositivo genera una pequeña vibración como respuesta háptica para indicar la interacción del usuario.
* **Manejo del estado:** Al cambiar la orientación del dispositivo, la reproducción continúa sin pausarse y se conserva la emisora seleccionada. Para esto se utilizaron herramientas de manejo de estado como `mutableStateOf` y `rememberSaveable`.
* **Audio real:** Se utilizó la librería **Media3 ExoPlayer** para conectarse a los enlaces de las emisoras y reproducir el audio mediante *streaming*.
* **Catálogo de emisoras:** La aplicación cuenta con una lista desplazable mediante `LazyColumn` y un buscador que permite encontrar y seleccionar diferentes emisoras.

## 💻 Herramientas utilizadas

* **Lenguaje:** Kotlin + java
* **Interfaz:** Jetpack Compose
* **Multimedia:** Media3 ExoPlayer para la reproducción de audio.
* **Cámara y permisos:** `ActivityResultContracts`
* **Vibración:** `VibratorManager`
* **Entorno de desarrollo:** Android Studio

## 🛠️ ¿Cómo probar este proyecto?

Para ejecutar y probar la aplicación:

1. Clonar este repositorio o descargar el proyecto en formato `.zip`.
2. Abrir la carpeta del proyecto en **Android Studio**.
3. Esperar a que Gradle descargue y configure las librerías necesarias, incluyendo Compose y ExoPlayer.
4. Ejecutar el proyecto utilizando el botón **Run** (▶️).

> **Nota:** Se recomienda probar la aplicación en un **dispositivo Android físico** conectado mediante USB o conectado en red por codigo QR, en lugar de utilizar únicamente el emulador. Esto permitirá comprobar correctamente las funciones de vibración y el uso de la cámara.

## 👥 Datos del equipo

 **Integrantes:**
 * David Alejandro Agudelo Meneses
 * Jhojanth Camilo Alegria Escobar
 * Juan Fernando Beltran Lopez
 * Ferney de Jesus Echeverri Echeverri
 * Maria Fernanda Vasquez Montiel

  **Modalidad:** Trabajo en equipo

 **Curso:** Programación dispositivos móviles

 **Institución:** Institución Universitaria Digital de Antioquia
