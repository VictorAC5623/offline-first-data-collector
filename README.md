# Newman-Registro (PreuCapturaApp)

App Android para que el **staff de un preuniversitario** capture datos de **prospectos** (estudiantes interesados) desde un teléfono o tablet, incluso **sin conexión a internet**. Los registros se guardan en **Firebase Firestore** y se sincronizan automáticamente cuando vuelve la conexión.

- **Nombre visible:** Newman-Registro (`app/src/main/res/values/strings.xml`)
- **Proyecto Gradle:** PreuCapturaApp
- **Paquete / applicationId:** `com.pre.capturadatos`
- **Versión:** 1.0 (versionCode 1)

---

## Funcionalidad

### 1. Inicio de sesión (`LoginScreen.kt`)
- Login con **Google Sign-In** → credencial de **Firebase Auth**.
- Usa el `default_web_client_id` que genera el plugin de Google Services a partir de `google-services.json`.
- Muestra estado de carga y mensajes de error (Google, Firebase o cancelación).
- Cualquier cuenta de Google puede iniciar sesión; la app no restringe por dominio o lista de staff (eso dependería de las reglas de Firestore).

### 2. Formulario de registro (`DataCaptureScreen.kt`)
Campos:

| Campo | Tipo | Validación |
|---|---|---|
| Nombre | Texto | Obligatorio |
| Apellidos | Texto | Obligatorio |
| Colegio | Texto | Obligatorio |
| Teléfono | Numérico | Exactamente 10 dígitos (oculta el teclado al llegar a 10) |
| Año de curso | Chips | `Primero`, `Segundo`, `Tercero`, `Graduado` |
| ¿Inscrito en otro preuniversitario? | Chips | `Sí` / `No` |

- El botón **Enviar** se habilita solo cuando todo es válido.
- Al enviar: guarda en Firestore, reproduce un sonido de éxito (`res/raw/success_sound.mp3`), muestra "¡Guardado con éxito!" durante 3 s y limpia el formulario para el siguiente prospecto.
- Logo del preuniversitario como marca de agua de fondo (`res/drawable/logo_preu.PNG`).
- Botón discreto **"Cerrar Sesión (Solo Staff)"** al final del formulario.

### 3. Navegación (`MainActivity.kt`)
- Un `AuthStateListener` decide qué pantalla mostrar: `LoginScreen` si no hay usuario, `DataCaptureScreen` si lo hay. No hay NavHost (aunque la dependencia `navigation-compose` está incluida).

---

## Modelo de datos (Firestore)

Colección: **`prospectos`** — un documento por registro (ID automático).

```json
{
  "nombre": "Juan",
  "apellidos": "Pérez Soto",
  "colegio": "Colegio X",
  "telefono": "0991234567",
  "ano_curso": "Tercero",
  "inscrito_otro_preu": false,
  "fecha_registro": "<serverTimestamp>",
  "staff_uid": "<uid del usuario logueado>",
  "staff_email": "staff@ejemplo.com"
}
```

## Modo offline-first

En `MainActivity.onCreate` se configura Firestore con `PersistentCacheSettings` (caché persistente en disco). Por eso:
- `db.collection("prospectos").add(...)` se escribe primero en la caché local y se sube cuando hay red.
- La app **no espera** la confirmación del servidor: el mensaje de éxito indica que se guardó localmente, no que ya llegó a la nube.

---

## Stack técnico

| Componente | Versión |
|---|---|
| Lenguaje | Kotlin 1.9.22 |
| UI | Jetpack Compose (BOM 2023.08.00), Material 3 |
| Compose compiler | 1.5.8 |
| Android Gradle Plugin | 8.13.2 |
| compileSdk / targetSdk / minSdk | 36 / 34 / 26 (Android 8.0+) |
| Firebase BOM | 32.7.2 (Auth KTX, Firestore KTX) |
| Google Sign-In | play-services-auth 21.2.0 |
| Java target | 1.8 |

Las versiones están centralizadas en `gradle/libs.versions.toml` (excepto `navigation-compose`, declarada directamente en `app/build.gradle.kts`).

## Estructura

```
captura_datos/
├── app/
│   ├── build.gradle.kts
│   ├── google-services.json          # Config de Firebase (NO versionar)
│   ├── release/app-release.apk       # APK de release generado
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/pre/capturadatos/
│       │   ├── MainActivity.kt        # Config offline de Firestore + switch login/formulario
│       │   ├── LoginScreen.kt         # Google Sign-In → Firebase Auth
│       │   ├── DataCaptureScreen.kt   # Formulario de prospectos
│       │   └── ui/theme/              # Colores (azul de marca), tema claro/oscuro, tipografía
│       └── res/
│           ├── drawable/logo_preu.PNG
│           └── raw/success_sound.mp3
├── gradle/libs.versions.toml
├── build.gradle.kts
└── settings.gradle.kts
```

---

## Configuración y ejecución

1. Abrir el proyecto en **Android Studio**.
2. Colocar el archivo **`app/google-services.json`** del proyecto Firebase (descargar desde la consola de Firebase → Configuración del proyecto → App Android `com.pre.capturadatos`).
3. En Firebase:
   - Habilitar **Authentication → Google**.
   - Registrar las huellas **SHA-1/SHA-256** de los keystores de debug y release (necesario para Google Sign-In).
   - Crear la base **Cloud Firestore**.
4. Compilar y ejecutar:
   ```bash
   ./gradlew assembleDebug
   ```
   Release:
   ```bash
   ./gradlew assembleRelease
   ```

---

## Notas técnicas / pendientes detectados

Observaciones del análisis del código, útiles para futuras mejoras:

- **`google-services.json` no está en `.gitignore`.** El último commit lo eliminó del repo, pero el archivo sigue en `app/` sin ignorar y podría volver a subirse. Además sigue presente en el historial de git (commit inicial).
- **APK de release versionado** en `app/release/`. Normalmente los binarios no se suben al repo.
- **Escritura sin confirmación:** `add()` no tiene `addOnSuccessListener`/`addOnFailureListener`; el `try/catch` solo atrapa errores síncronos. No hay forma de ver registros pendientes de sincronizar.
- **Google Sign-In legacy:** `GoogleSignIn`/`GoogleSignInOptions` están deprecados; la alternativa actual es **Credential Manager**.
- **APIs KTX de Firebase deprecadas:** `firebase-*-ktx` y `com.google.firebase.ktx.Firebase` se reemplazan por los módulos principales (BOM 32.5+ ya los incluye).
- **Sin control de acceso por staff:** cualquier cuenta de Google entra; conviene validarlo en reglas de Firestore o en la app.
- **Sin pantalla para ver/exportar registros**; la consulta se hace desde la consola de Firebase.
- **Dependencias sin usar:** `navigation-compose` y `material-icons-extended`.
- **Sin tests reales** (solo los `Example*Test` de plantilla).
- `minifyEnabled = false` en release.
