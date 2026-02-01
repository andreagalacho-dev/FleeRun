# 🏃‍♀️ FleeRun

**FleeRun** es una aplicación móvil Android desarrollada en **Kotlin** como parte de un **Trabajo de Fin de Grado (TFG)**.  
La aplicación está orientada a la **gestión de entrenamientos deportivos**, permitiendo la interacción entre **atletas y entrenadores**, así como el seguimiento de actividades y feedback.

---

## 📱 Características principales

- Registro e inicio de sesión de usuarios
- Diferenciación de roles (atleta / entrenador)
- Visualización de entrenamientos asignados
- Gestión de atletas por parte del entrenador
- Envío y visualización de feedback
- Calendario de entrenamientos
- Interfaz moderna basada en **Material Design**

---

## 🛠️ Tecnologías utilizadas

- **Lenguaje:** Kotlin  
- **IDE:** Android Studio  
- **Arquitectura:** Android estándar (Activities + UI Components)
- **Base de datos y autenticación:** Firebase
  - Firebase Authentication
  - Firebase Firestore / Realtime Database
- **Control de versiones:** Git & GitHub  
- **Sistema de compilación:** Gradle (Kotlin DSL)

---

## 📂 Estructura del proyecto
FleeRun/
│── app/
│ ├── src/
│ │ ├── main/
│ │ │ ├── java/com/example/fleerun/
│ │ │ ├── res/
│ │ │ └── AndroidManifest.xml
│ └── build.gradle.kts
│── gradle/
│── build.gradle.kts
│── settings.gradle.kts
│── gradlew

# 🔐 Firebase y configuración

Por motivos de **seguridad**, el archivo:

app/google-services.json


❌ **NO está incluido en el repositorio**.

Para ejecutar el proyecto correctamente:

1. Crear un proyecto en [Firebase Console](https://console.firebase.google.com/)
2. Registrar la aplicación Android
3. Descargar el archivo `google-services.json`
4. Colocarlo en:
app/google-services.json



## ▶️ Ejecución del proyecto

1. Clonar el repositorio:
```bash
git clone https://github.com/andreagalacho-dev/FleeRun.git
Abrir el proyecto con Android Studio

Sincronizar Gradle

Añadir google-services.json

Ejecutar en emulador o dispositivo físico

