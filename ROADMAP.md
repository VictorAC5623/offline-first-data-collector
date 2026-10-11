# Roadmap — Newman-Registro

> Borrador para revisar. Contexto: app interna del staff (no está en Play Store) para capturar prospectos en ferias y eventos, donde el internet es intermitente. **El requisito principal es no perder datos**: el resto de las mejoras se evalúan contra eso.

Prioridad: 🔴 crítica · 🟠 alta · 🟡 media · 🟢 deseable

---

## Fase 0 — Seguridad e higiene del proyecto

| # | Mejora | Prioridad | Por qué |
|---|---|---|---|
| 0.1 | Agregar `google-services.json`, `app/release/`, `*.apk`, `repomix-output.xml` al `.gitignore` | 🔴 | Hoy el archivo de Firebase está sin ignorar y puede volver a subirse en cualquier commit. |
| 0.2 | Revisar y endurecer las **reglas de seguridad de Firestore** | 🔴 | Hay que confirmar que solo el staff pueda **escribir** en `prospectos` y que **nadie pueda leer** los datos desde la app. Además, validar en las reglas los campos y tipos de cada registro. |
| 0.3 | **Lista de staff autorizado** (en una colección `staff`, por dominio de correo o con custom claims) | 🔴 | Hoy cualquier cuenta de Google puede iniciar sesión y escribir. |
| 0.4 | Respaldar el **keystore de firma** y documentar cómo se genera el APK | 🟠 | Si se pierde el keystore, los teléfonos tendrían que desinstalar la app para poder actualizarla, y se perderían los datos pendientes. |
| 0.5 | Activar `minifyEnabled` (R8) en release | 🟡 | El APK queda más liviano y más difícil de leer por ingeniería inversa. |

## Fase 1 — Confiabilidad offline (el núcleo de la app)

| # | Mejora | Prioridad | Por qué |
|---|---|---|---|
| 1.1 | **Indicador de sincronización**: "X registros pendientes de subir", "Todo sincronizado" y hora del último envío | 🔴 | Hoy el staff no tiene forma de saber si los datos ya llegaron a la nube. |
| 1.2 | **Bloquear o advertir al cerrar sesión con registros pendientes** | 🔴 | Si se cierra sesión con datos sin subir, Firestore los intentará enviar sin usuario y las reglas los rechazarán: **se pierden**. Lo mismo pasa si se borran los datos de la app o se desinstala. |
| 1.3 | Decidir la estrategia de almacenamiento local (ver la decisión A) | 🟠 | La caché de Firestore funciona, pero no se puede consultar, exportar ni reintentar a voluntad. |
| 1.4 | **IDs generados en el teléfono** (UUID) en lugar de `add()` | 🟠 | Si un registro se reenvía, sobrescribe el mismo documento en vez de crear un duplicado. |
| 1.5 | **Exportar a CSV desde el teléfono** como respaldo de emergencia | 🟠 | Plan B si la sincronización falla en un evento importante. |
| 1.6 | Confirmar que la sesión no expire en pleno evento sin internet | 🟡 | Iniciar sesión requiere internet. Conviene dejar los teléfonos con la sesión abierta antes de salir al evento y validar ese flujo. |
| 1.7 | Agregar `app_version` y un `device_id` en cada registro | 🟡 | Sirve para trazar problemas ("los registros de la tablet 3 no llegaron"). |

### Decisión A — ¿Cómo guardar los datos en el teléfono?
- **Opción 1: mantener la caché de Firestore y agregarle visibilidad** (con `hasPendingWrites`, `waitForPendingWrites` y listeners). Requiere poco trabajo y se aprovecha lo que ya funciona.
- **Opción 2: una bandeja de salida propia con Room + WorkManager.** Los registros se guardan primero en una base local y un proceso en segundo plano los sube a Firestore. Da control total para listar pendientes, reintentar, exportar y editar antes de enviar, y los datos sobreviven aunque se cierre sesión. Requiere más trabajo.
- *Recomendación:* la opción 2, porque el caso de uso (ferias con internet intermitente) es justamente el que la justifica.

## Fase 2 — Datos y formulario

| # | Mejora | Prioridad | Por qué |
|---|---|---|---|
| 2.1 | **Campo "Evento/Feria"**: seleccionar el evento activo al iniciar la jornada y guardarlo en cada registro | 🔴 | Sin este dato no se puede medir qué feria rinde más. |
| 2.2 | **Consentimiento de tratamiento de datos** (checkbox y texto legal) | 🔴 | Se recolectan datos personales de menores de edad. Si el preuniversitario está en Ecuador (el formato de teléfono lo sugiere), aplica la LOPDP. |
| 2.3 | Lista de **colegios con autocompletado** | 🟠 | Evita variantes como "Col. San José", "San Jose" o "Colegio San José", que hoy hacen imposible agrupar los datos. |
| 2.4 | Nuevos campos posibles: correo, carrera o universidad de interés, cómo se enteró, observaciones | 🟡 | Hay que definirlos con el equipo comercial. |
| 2.5 | Validación de teléfono más estricta (por ejemplo, que empiece con `09`) | 🟡 | Hoy se aceptan 10 dígitos cualesquiera. |
| 2.6 | **Detección de duplicados** (mismo teléfono) | 🟡 | La misma persona suele pasar por el stand dos veces. |
| 2.7 | Opciones configurables desde la nube (años, eventos, colegios) sin publicar otro APK | 🟡 | Hoy cambiar las opciones obliga a compilar y reinstalar en todos los teléfonos. |
| 2.8 | Contador "Registros de hoy: N" para el usuario | 🟢 | Motiva al staff y sirve como control rápido. |

## Fase 3 — El sistema completo (más allá de la app)

| # | Mejora | Prioridad | Por qué |
|---|---|---|---|
| 3.1 | **Panel web de administración** (con Firebase Hosting): ver, filtrar por evento, staff o fecha, y exportar a Excel | 🟠 | Hoy los datos solo se ven desde la consola de Firebase. |
| 3.2 | **Seguimiento de prospectos**: estados (nuevo, contactado, interesado, inscrito) y notas | 🟠 | Convierte la base de datos en un embudo de ventas medible. |
| 3.3 | Roles **admin y staff** | 🟠 | Lo necesita el panel web. |
| 3.4 | Estadísticas: registros por evento, por staff y por colegio, y tasa de conversión | 🟡 | |
| 3.5 | Integraciones: botón de WhatsApp, sincronización a Google Sheets y notificaciones | 🟢 | |
| 3.6 | **Autorregistro por QR** en el stand (formulario web que alimenta la misma colección) | 🟢 | Ayuda cuando hay filas largas, aunque depende del internet del visitante. |

## Fase 4 — Modernización técnica

| # | Mejora | Prioridad | Por qué |
|---|---|---|---|
| 4.1 | Actualizar Kotlin 2.x, Compose BOM, AGP, Java 17 y targetSdk 35+ | 🟠 | Las versiones actuales son de 2023-2024. |
| 4.2 | Migrar Firebase a los módulos sin `-ktx` (BOM 33+/34) | 🟠 | En el BOM 34 se eliminaron los módulos `-ktx`, así que la app no podrá actualizarse sin esta migración. |
| 4.3 | Migrar Google Sign-In a **Credential Manager** | 🟠 | `GoogleSignIn` está deprecado. |
| 4.4 | Arquitectura: **ViewModel + Repository** para separar la UI de Firebase | 🟠 | Es la base para la fase 1, para los tests y para crecer sin desorden. |
| 4.5 | Pasar los textos a `strings.xml` | 🟡 | |
| 4.6 | Tests unitarios (validaciones, repositorio y sincronización) | 🟡 | |
| 4.7 | Distribución con **Firebase App Distribution** y aviso de "nueva versión disponible" | 🟠 | Facilita actualizar los teléfonos del staff sin pasar el APK a mano. |
| 4.8 | **Crashlytics** | 🟡 | Avisa de fallos en los teléfonos del staff, incluso durante los eventos. |
| 4.9 | Quitar dependencias sin uso (`navigation-compose`, `material-icons-extended`) o empezar a usarlas | 🟢 | |
| 4.10 | Diseño adaptado a tablets y modo de captura rápida | 🟢 | |

---

## Orden sugerido

1. **Fase 0** completa, que es rápida y protege lo que ya existe.
2. **4.1–4.4** (actualización y arquitectura), porque es la base técnica que necesita la fase 1.
3. **Fase 1** (confiabilidad offline).
4. **2.1–2.3** (evento, consentimiento y colegios).
5. **4.7** (distribución), para poder actualizar los teléfonos con facilidad.
6. **Fase 3** (panel web y seguimiento).

## Preguntas abiertas
- ¿Cómo están hoy las reglas de Firestore?
- ¿Cómo consumen los datos actualmente: consola, exportación manual o alguien los copia a Excel?
- ¿Cuántos teléfonos y personas usan la app? ¿Son teléfonos personales o del preuniversitario?
- ¿En qué país operan? Esto define la ley de protección de datos que aplica.
- ¿Qué campos nuevos necesita el área comercial?
- ¿Interesa el panel web y el seguimiento de prospectos, o solo la app?
