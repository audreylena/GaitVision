# GaitVision

_2-D Gait Analysis for clinical use._

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Technology](#technology)
- [Installation](#installation)
- [Usage](#usage)
- [Contact](#contact)
- [References](#References)

---

## Overview

GaitVision in an android application focused on solving the problem of lower technology access for gait analysis in developing parts of the world. Using a minimum amount of hardware only requiring:
1. Android phone
2. GaitVision Software
3. Camera access OR stored videos

---

## Features

- [Record Videos in App]
- [Alter Input video to show angles at timepoint]
- [Show gait score estimate from autoencoder]
- [Show graph of angles for analysis]
- [Store CSV of angles on local machine for later use]

---

## Technology

- **Language:** Kotlin / Java
- **UI Framework:** Android XML
- **Pose Detection:** MLKit
- **Gait Score:** Autoencoder

---

## Installation

### Steps

1. Transfer .apk file to android compatible device
2. Open .apk file in filemanager
3. Click install and allow all

---

## PC Development Environment Setup

### Requirements

- Git
- Android Studio
- JDK 17 or compatible version

### Setup Steps

1. Clone the repository
2. Open the project in Android Studio
3. Allow Gradle to sync and install dependencies
4. Install any required SDK components if prompted
5. Select the Android app configuration
6. Run the app on an emulator or connected Android device

---

## Deployment Artifacts

The following artifacts should be included for deployment:

- Android APK file
- Source code repository, GitHub
- Autoencoder (AE) model files
- PCA model files
- Linear regression scoring model
- Sample test videos
- Installation and setup instructions

---

## Usage

- Launch the app.
- Input Unique Participant ID.
- Input participant height in the form FEET INCHES
- Click Record video or Select video
- Have participant walk as they normally would for minimum 2 gait cycles ( roughly 5 seconds )
- Click Perform Analysis
- View Video then click View Analysis
- Select graphs as needed
- Export CSV or return to main menu

---

## Contact

contact for clarification at nathaniel.schimpf@gmail.com

---

## References
-[MPAndroidChart](https://github.com/PhilJay/MPAndroidChart) by Phil Jay (Apache 2.0 License)

Special thanks to:
Guna Sindhuja Siripurapu 
Dr. Rita Patterson
Dr. Mark Albert
University of North Texas