# GaitVision KMP

This is the Kotlin Multiplatform migration of the GaitVision project.

## Sprint 5 Contributions (Working KMP for iOS and Android)

The focus of Sprint 5 was to port the UI and core business logic from the original Android application into a fully functional and cross-platform architecture that builds successfully for both Android and iOS targets. 

### 1. User Interface (UI) Migration & Modernization 
- **Platform Agnostic UI:** Ported all core Android activities to Compose Multiplatform screens in the `commonMain` module.
- **Patient Management:** Implemented `PatientListScreen`, `PatientCreateScreen`, and `PatientProfileScreen` for navigating and visualizing patient histories.
- **Results & Data Visualization:** Created `ResultsScreen` for dynamic score summaries and built a fully custom `SignalsDashboardScreen` charting tool leveraging KMP Canvas for evaluating kinematic waveforms.
- **Settings & Help:** Added standalone `SettingsScreen`, `InfoScreen`, and `HelpScreen` for app configuration and user guidance.
- **Navigation:** Refactored Jetpack Navigation to a unified multi-screen router enabling completely unified platform backstacks and argument passing.

### 2. Core Algorithm Porting (Business Logic)
- **Feature Extraction:** Successfully mapped the massive 1,600+ line `FeatureExtractor.kt` to KMP by breaking its dependencies on Android-specific MediaPipe interfaces and Android `Log` utilities. Created generic array schemas for Multiplatform Pose index handling.
- **Pose Detection Sequences:** Built multiplatform-compliant versions of `PoseSequence` and `PoseFrame` data entities originally tied to Android's ecosystem.
- **Gait Scoring:** Implemented an `expect`/`actual` abstraction over `GaitScorer.kt` to allow TFLite dependencies to smoothly resolve on Android targets (`androidMain`) while avoiding compilation failures on iOS (`iosMain`), enabling cross-platform model inference.
- **Algorithm Tracking:** Ported `ROITracker.kt` into `commonMain`, replacing Android `Rect` objects with customized geometry structs resolving purely in standard Kotlin. 
- **CSV Exporters:** Refactored completely to output strings generated via standard library strings rather than leveraging strictly JVM `outputStreams` reducing dependency on Android filesystem APIs.
- **Gradle Stability:** Implemented advanced build scripting with accurate Room KSP mappings and dynamic SDK parameters.

### Current Build Status
- **Android Target:** Build tests resolving as `BUILD SUCCESSFUL` dynamically pulling in the shared modules. 
- **iOS Target:** Source code maps cleanly without native library linker conflicts. Ready to compile into the Xcode iOS framework.

---

## Steps to Run the App

This is a Kotlin Multiplatform project using Compose Multiplatform. Since it contains targets for both Android and iOS, ensure you have the requisite environments installed before testing.

### Prerequisites
1. **Android Studio (Koala or later recommended)** for managing the Kotlin logic. 
2. **Xcode 15+** installed on macOS for iOS deployment (Optional but required to test iOS builds).
3. **KMP Plugin** for Android Studio.

### Android Installation & Run
1. Open the `/GaitVisionKMP` directory in Android Studio.
2. Wait for Gradle Sync to finish downloading Compose Multiplatform dependencies.
3. In the toolbar, locate the "Run/Debug Configurations" dropdown menu.
4. Select `composeApp` (this is your Android build target).
5. Ensure a physical device is connected, or an Android Virtual Device (AVD) is running.
6. Click the green **Play (Run)** button or use the terminal:
   ```bash
   ./gradlew :composeApp:installDebug
   ```

### iOS Installation & Run
*(Requires a macOS environment)*
1. Open the `/GaitVisionKMP` directory in Android Studio or Fleet.
2. In the toolbar "Run/Debug Configurations" dropdown menu, select `iosApp`. 
3. Choose your desired iOS Simulator or plugged-in iOS device.
4. Click the green **Play (Run)** button. 
   *(Alternatively, you can open the `/iosApp/iosApp.xcworkspace` in Xcode directly and build the application from there to debug native swift constraints).*
