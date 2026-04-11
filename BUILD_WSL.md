# Building the Saithanyam APK in WSL — Step-by-Step Guide

This guide shows you how to build the app as an installable `.apk` file using **WSL (Windows Subsystem for Linux)** on Windows, without needing Android Studio at all.

---

## What is WSL?

**WSL** lets you run a real Linux system inside Windows 10 or 11. It is free and made by Microsoft. Once installed, you can run Linux commands in a terminal — and that is enough to build Android apps.

---

## Part A — One-Time Setup (do this only the first time)

### Step 1 — Install WSL on Windows

1. Press the **Windows key**, type **"PowerShell"**
2. Right-click **Windows PowerShell** and choose **"Run as administrator"**
3. Paste this command and press Enter:
   ```
   wsl --install
   ```
4. Wait for it to finish. It will install Ubuntu by default.
5. **Restart your computer** when it asks you to.
6. After restart, Ubuntu will open automatically. It will ask you to:
   - Create a **username** (use lowercase, no spaces — for example: `saithanyam`)
   - Create a **password** (you will not see the characters as you type — this is normal)

You now have a Linux terminal running inside Windows.

### Step 2 — Update Ubuntu

In the Ubuntu terminal, run:
```
sudo apt update && sudo apt upgrade -y
```
Type your password when asked. Wait for it to finish (can take a few minutes).

### Step 3 — Install Required Tools

Install Java 17, `unzip`, `wget`, and `git`:
```
sudo apt install -y openjdk-17-jdk unzip wget git
```

Verify Java 17 is installed:
```
java -version
```
You should see something like `openjdk version "17.0.x"`.

### Step 4 — Clone the Project

Go to your home folder and clone the repository:
```
cd ~
git clone https://github.com/prasanth-lab-ui/Github-Action.git
cd Github-Action
git checkout claude/saithanyam-wallpaper-app-JUFt5
```

You now have the project at `~/Github-Action` inside WSL.

---

## Part B — Build the APK (every time you want a new APK)

### Step 5 — Run the Build Script

From the project folder, run:
```
./build-apk.sh
```

**First run:** The script will automatically:
1. Check for Java 17
2. Download the Android SDK command-line tools (~150 MB)
3. Accept Android SDK licenses
4. Download Android platform 34 and build tools (~500 MB)
5. Install Gradle 8.5 (~150 MB)
6. Build the APK

**This takes 10-20 minutes on the first run.** Later runs take about 1 minute because everything is cached.

When it finishes, you will see:
```
[OK] APK built successfully!

  File: /home/you/Github-Action/app/build/outputs/apk/debug/app-debug.apk
  Size: 9.5M

[OK] Copied to Windows Desktop: Saithanyam-debug.apk
```

The script automatically copies the APK to your **Windows Desktop** so you can find it easily.

### Step 6 — Alternative Commands

Run `./build-apk.sh` with a different argument for different tasks:

| Command | What it does |
|---------|--------------|
| `./build-apk.sh` | Builds debug APK (default) |
| `./build-apk.sh debug` | Same as above |
| `./build-apk.sh release` | Builds unsigned release APK (smaller, optimized) |
| `./build-apk.sh clean` | Deletes old build files — use if you have weird errors |

---

## Part C — Install the APK on Your Phone

### Option 1 — Via USB Cable (Easy)

1. On your phone, enable **Developer Options**:
   - Go to **Settings > About Phone**
   - Tap **"Build Number"** 7 times
2. Enable **USB Debugging**:
   - **Settings > Developer Options > USB Debugging** — turn it ON
3. Connect your phone to your computer with a USB cable
4. On your phone, tap **"Allow"** when it asks to trust the computer
5. In WSL terminal, install ADB and install the APK:
   ```
   sudo apt install -y android-tools-adb
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```
6. The app will appear in your phone's app drawer

### Option 2 — Copy the APK Manually

1. The script already copies `Saithanyam-debug.apk` to your **Windows Desktop**
2. Send it to your phone using any method:
   - Upload to Google Drive, download on phone
   - Email it to yourself
   - Use a USB cable and copy to phone storage
3. On your phone, go to **Settings > Security > Install Unknown Apps**
4. Allow your file manager (or Chrome) to install APKs
5. Tap the APK file on your phone → **Install**
6. Open the app

---

## Part D — Troubleshooting

### "Permission denied" when running `./build-apk.sh`
Make the script executable:
```
chmod +x build-apk.sh
```

### "Java not found" or wrong Java version
Install Java 17:
```
sudo apt install -y openjdk-17-jdk
sudo update-alternatives --config java
```
Then pick the number next to Java 17 from the menu.

### "Failed to download Android command-line tools"
Check your internet connection, then try again:
```
./build-apk.sh
```

### Build fails with "SDK location not found"
Delete the `local.properties` file and run the script again:
```
rm local.properties
./build-apk.sh
```

### Build takes forever / freezes
The first build is slow (10-20 minutes). Do not stop it. Later builds are fast.

### "Out of memory" error
Give Gradle more memory by editing `gradle.properties`:
```
nano gradle.properties
```
Change the first line to:
```
org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8
```
Press **Ctrl+O**, Enter, then **Ctrl+X** to save. Run the build again.

### Script says "APK not found" after build
The Gradle build itself had an error. Scroll up in the terminal and look for the **red error message**. Common causes:
- Missing Android SDK packages — run `./build-apk.sh clean` and try again
- Out of disk space — WSL needs at least 10 GB free

### Finding your WSL files from Windows
Open File Explorer and type this in the address bar:
```
\\wsl$\Ubuntu\home\YOUR_WSL_USERNAME\Github-Action
```
You will see all your project files and can copy them to Windows.

### Finding your Windows files from WSL
Your `C:\` drive is at `/mnt/c/` inside WSL.
Your Windows Desktop is at `/mnt/c/Users/YOUR_WINDOWS_USERNAME/Desktop/`.

---

## Summary — The Short Version

After one-time setup, every build is just **two steps**:

```
cd ~/Github-Action
./build-apk.sh
```

The APK will be at:
- **Inside WSL:** `~/Github-Action/app/build/outputs/apk/debug/app-debug.apk`
- **On Windows:** `C:\Users\YourName\Desktop\Saithanyam-debug.apk`

Copy it to your phone and install — done!

---

## What the Script Does (for curious people)

The `build-apk.sh` script runs 6 steps automatically:

1. **Checks Java 17** is installed
2. **Installs Android SDK** if not present (cmdline-tools, platform-tools, Android 34, build-tools 34.0.0)
3. **Creates `local.properties`** pointing to the SDK
4. **Installs Gradle 8.5** if the wrapper is missing
5. **Builds the APK** using `gradle assembleDebug` (or `assembleRelease`)
6. **Reports the result** and copies APK to Windows Desktop

You can read the script yourself — it is plain text with comments explaining each step.
