# GaitVision KMP: iOS Setup and Run Guide

This guide provides step-by-step instructions for running the GaitVision Kotlin Multiplatform (KMP) application on iOS. You must be on a macOS environment to compile and run the iOS target.

## Prerequisites

Before you begin, ensure you have the following installed on your Mac:
1. **Xcode 15+** (Required for the iOS simulator and compilation).
2. **Android Studio (Koala or later)** with the **Kotlin Multiplatform (KMP) Plugin** installed.

---

## Method 1: Running via Android Studio (Recommended)

This is the easiest method since Android Studio will handle building the Kotlin shared logic and deploying the iOS app automatically.

1. **Open the Project:** Launch Android Studio and open the `GaitVisionKMP` root directory.
2. **Wait for Gradle Sync:** Allow Android Studio to finish syncing the project and downloading the necessary multiplatform dependencies.
3. **Select the Target:** At the top toolbar, find the **Run/Debug Configurations** dropdown menu (usually next to the "Play" button).
4. **Choose `iosApp`:** Select `iosApp` from the dropdown list.
5. **Select a Device:** To the right of the configuration dropdown, select your preferred iOS Simulator (e.g., iPhone 15 Pro) or a plugged-in physical iOS device.
6. **Run:** Click the green **Play (Run)** button. Android Studio will compile the shared code into an iOS framework and launch it on your chosen simulator.

---

## Method 2: Running via Xcode natively

Use this method if you need to debug specific native Swift UI behaviors, review iOS linker logs, or modify native settings.

1. **Build the Shared Framework first (Optional but recommended):**
   Open your terminal in the root `GaitVisionKMP` directory and run a build to ensure the shared Kotlin code compiles into the native iOS framework:
   ```bash
   ./gradlew :composeApp:compileKotlinIosX64
   ```

2. **Open the Xcode Project:**
   Navigate into the `iosApp` folder and open the Xcode project file:
   ```bash
   cd iosApp
   open iosApp.xcodeproj 
   ```
   *(Note: If CocoaPods is introduced in the future, you would open `iosApp.xcworkspace` instead)*

3. **Trust the Project:** If Xcode prompts you about trusting the project or running custom build scripts (which Gradle uses to inject the Kotlin framework), click **Trust and Enable**.

4. **Select a Target:** In the Xcode top toolbar, click on the device name next to the "iosApp" scheme and select a Simulator (e.g., iPhone 15) or a connected iOS device.

5. **Build and Run:** Press `Cmd + R` or click the **Play** button in the top left corner of Xcode to compile and launch the application.

---

## Troubleshooting

- **Missing Framework Error:** If you get an error in Xcode saying the `ComposeApp` framework is missing, you may need to do a clean build. You can do this by pressing `Cmd + Shift + K` in Xcode, or by closing Xcode, running `./gradlew clean` in the root project directory via your terminal, and trying again.
