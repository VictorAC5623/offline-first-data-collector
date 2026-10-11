# Offline-First Data Collection App 📱

**A native Android application designed for high-reliability data acquisition in low-connectivity environments.**

This tool was engineered to replace manual data entry at trade fairs for *Preuniversitario Newman*, eliminating transcription errors and ensuring 100% data integrity regardless of network status.

## 🏗 Architecture

* **Language:** Kotlin (100%).
* **Pattern:** MVVM (Model-View-ViewModel) with Clean Architecture principles.
* **Database:** Google Firestore (NoSQL).
* **Concurrency:** Kotlin Coroutines & Flow.

## ⚡ Key Technical Features

### 1. Offline-First Synchronization
Unlike standard apps that crash without internet, this app writes directly to a local persistence layer first.
* Uses Firestore's `PersistentCacheSettings` to store leads locally on the device.
* Implements a background worker that automatically synchronizes the delta with the cloud once connectivity is restored.

### 2. Robust Data Validation
* Real-time input masking for phone numbers (EC format).
* Duplicate detection logic (prevents registering the same lead twice locally).

### 3. UX/UI for Speed
* Optimized specifically for high-throughput environments (trade fairs) where speed of entry is critical.
* Minimalist interface to reduce cognitive load on the operator.

## 📂 Project Structure

* `app/src/main/java/com/.../data`: Repository implementation and Firestore sources.
* `app/src/main/java/com/.../domain`: Business logic and use cases.
* `app/src/main/java/com/.../ui`: Fragments and ViewModels.

## 🚀 Setup

1.  Clone the repository.
2.  Add your own `google-services.json` to the `/app` folder (Required for Firebase connection).
3.  Build using Android Studio Hedgehog or later.

---
*Authored by **Victor Altamirano** - Physicist & AI Engineer*

## © Copyright This project is intended for portfolio and educational demonstration purposes only. Commercial use, modification, or distribution of this source code without the author's explicit permission is prohibited.
