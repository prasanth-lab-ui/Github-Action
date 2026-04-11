# Saithanyam - Build & Test Instructions

A complete beginner-friendly guide to build and run the Saithanyam live wallpaper app.

---

## What You Will Need

| Tool | Why You Need It | Download Link |
|------|----------------|---------------|
| **Android Studio** (Hedgehog 2023.1.1 or newer) | The app-building software | https://developer.android.com/studio |
| **An Android Phone** OR **Emulator** | To run and test the app | Comes with Android Studio |
| **A Computer** | Windows, Mac, or Linux — any will work | — |
| **Internet Connection** | To download tools and libraries | — |

---

## PART 1: Install Android Studio (One-Time Setup)

### Step 1 — Download Android Studio

1. Open your web browser
2. Go to: **https://developer.android.com/studio**
3. Click the big green **"Download Android Studio"** button
4. Accept the terms and conditions
5. Wait for the download to finish (it is about 1 GB)

### Step 2 — Install Android Studio

**On Windows:**
1. Double-click the downloaded `.exe` file
2. Click **Next** on every screen (keep all defaults)
3. Click **Install**
4. Click **Finish** when done

**On Mac:**
1. Double-click the downloaded `.dmg` file
2. Drag the **Android Studio** icon into the **Applications** folder
3. Open it from Applications

**On Linux:**
1. Extract the downloaded `.tar.gz` file
2. Open a terminal and run: `./android-studio/bin/studio.sh`

### Step 3 — First-Time Setup Wizard

When you open Android Studio for the first time:

1. Select **"Do not import settings"** and click OK
2. Choose **"Standard"** installation type — click Next
3. Pick your preferred theme (Light or Dark) — click Next
4. It will download SDK components — **wait for it to finish** (this can take 10-20 minutes)
5. Click **Finish**

---

## PART 2: Get the Project Code

### Option A — Clone from GitHub (Recommended)

1. Open Android Studio
2. Click **"Get from VCS"** on the welcome screen
3. In the URL field, paste your repository URL:
   ```
   https://github.com/prasanth-lab-ui/Github-Action.git
   ```
4. Choose a folder on your computer to save it
5. Click **Clone**
6. When asked "Trust this project?" — click **Trust Project**
7. Switch to the correct branch:
   - At the bottom-right corner of Android Studio, click the branch name (it says `main`)
   - Select **`origin/claude/saithanyam-wallpaper-app-JUFt5`**
   - Click **Checkout**

### Option B — Open from a Folder

If you already have the code downloaded:

1. Open Android Studio
2. Click **"Open"**
3. Navigate to the folder that contains `settings.gradle.kts`
4. Select that folder and click **OK**
5. When asked "Trust this project?" — click **Trust Project**

---

## PART 3: Let Gradle Sync (Very Important)

After opening the project, Android Studio needs to download all the libraries.

1. You will see a message at the top: **"Gradle sync started"**
2. **Wait for it to finish** — look at the bottom progress bar
3. This can take 5-15 minutes on the first run
4. When done, you will see: **"Gradle sync finished"** in the bottom status bar

**If Gradle sync fails:**
- Click **"Try Again"** in the notification bar
- If it still fails, go to **File > Invalidate Caches > Invalidate and Restart**
- Make sure you are connected to the internet

---

## PART 4: Set Up a Test Device

You need either a real Android phone or a virtual one (emulator).

### Option A — Use Your Android Phone (Easier)

1. On your phone, go to **Settings > About Phone**
2. Tap **"Build Number"** 7 times — you will see "You are now a developer!"
3. Go back to **Settings > Developer Options**
4. Turn on **"USB Debugging"**
5. Connect your phone to your computer with a USB cable
6. On the phone, tap **"Allow"** when asked to trust the computer
7. In Android Studio, your phone name will appear in the top toolbar dropdown

### Option B — Create an Emulator (No Phone Needed)

1. In Android Studio, click **Tools > Device Manager** (right sidebar)
2. Click **"Create Virtual Device"**
3. Select **"Pixel 6"** (or any phone) — click **Next**
4. Select a system image:
   - Choose **"API 34"** (Android 14)
   - If it says "Download", click the download link and wait
5. Click **Next**, then **Finish**
6. Click the **Play (triangle) button** next to the device to start it
7. Wait for the emulator to boot up (first time takes 2-3 minutes)

---

## PART 5: Build and Run the App

This is the exciting part!

1. In the top toolbar, make sure your device is selected in the dropdown
2. Click the **green Play button** (triangle icon) — or press **Shift + F10**
3. Wait for the build to complete (first build takes 2-5 minutes)
4. The app will automatically install and open on your phone/emulator

**What you should see:**
- A black screen with **"Saithanyam"** title
- Below it: **"Your life in 4,000 weeks"**
- A white button: **"Enter your birthday"**

---

## PART 6: Test the App (Step by Step)

### Test 1 — Enter Your Birthday

1. Tap **"Enter your birthday"**
2. A calendar date picker will appear
3. Select your birth date (use the arrows to go to older months/years)
4. Tap **"Confirm"**
5. **Expected result:** You should see two large numbers:
   - Grey number = **weeks you have lived**
   - White number = **weeks remaining** (out of 4,000)

### Test 2 — Set the Live Wallpaper

1. Tap the **"Set Live Wallpaper"** button
2. Your phone's wallpaper picker will open
3. Tap **"Set wallpaper"** (or "Apply")
4. Choose **"Home screen"** or **"Home screen and lock screen"**
5. Press your phone's **Home button** to go to the home screen
6. **Expected result:** Your wallpaper should now show a grid of tiny dots:
   - **Grey dots** = weeks you have already lived
   - **White dots** = weeks you have remaining
   - **One dot with a glow** = your current week
   - Background should be **solid black**

### Test 3 — Verify the Dot Grid

1. Look at the wallpaper carefully
2. Check these things:
   - [ ] All dots are small circles arranged in a grid
   - [ ] Grey dots come first (top-left area)
   - [ ] White dots fill the remaining area
   - [ ] One dot has a subtle glowing ring around it (current week)
   - [ ] Background is pure black
   - [ ] No text or labels anywhere — just dots
   - [ ] Dots fill the screen nicely (not too big, not too small)

### Test 4 — Rotate the Screen

1. Rotate your phone sideways (landscape mode)
2. **Expected result:** The dot grid should rearrange itself to fit the new screen shape
3. Rotate back to portrait — it should rearrange again
4. (This tests the dynamic `onSurfaceChanged()` recalculation)

### Test 5 — Share Card

1. Open the Saithanyam app again
2. Tap the **"Share card"** button
3. **Expected result:** A toast message says **"Saved to gallery"**
4. Open your phone's **Photos** or **Gallery** app
5. Look for a folder called **"Saithanyam"**
6. You should see a black image with white text: **"I have X weeks left"**

### Test 6 — Change Birthday

1. Open the Saithanyam app
2. Tap **"Change birthday"** at the bottom
3. Pick a different date and confirm
4. **Expected result:** The weeks lived and weeks remaining numbers should update
5. Go to your home screen — the wallpaper dots should also update on next redraw

### Test 7 — App Restart Persistence

1. Force-close the Saithanyam app:
   - Go to **Settings > Apps > Saithanyam > Force Stop**
2. Open the app again
3. **Expected result:** Your birthday should still be remembered (not asking again)
4. The weeks lived/remaining should show immediately
5. (This tests EncryptedSharedPreferences storage)

---

## PART 7: Build a Release APK (To Share With Others)

If you want to create an APK file to install on other phones:

1. In Android Studio, go to **Build > Build Bundle(s) / APK(s) > Build APK(s)**
2. Wait for the build to complete
3. A notification will appear: "APK(s) generated successfully"
4. Click **"locate"** to find the APK file
5. The file is at: `app/build/outputs/apk/debug/app-debug.apk`
6. You can send this file to any Android phone and install it

---

## Troubleshooting

### "Gradle sync failed"
- Make sure you have internet
- Go to **File > Invalidate Caches > Invalidate and Restart**
- Try again

### "No device found"
- If using a real phone: check USB cable, make sure USB Debugging is ON
- If using emulator: make sure it is running (click play in Device Manager)

### "SDK not found"
- Go to **File > Project Structure > SDK Location**
- Make sure the Android SDK path is set correctly
- Usually it is at:
  - Windows: `C:\Users\YourName\AppData\Local\Android\Sdk`
  - Mac: `/Users/YourName/Library/Android/sdk`
  - Linux: `/home/YourName/Android/Sdk`

### App crashes on launch
- Look at the **Logcat** panel at the bottom of Android Studio
- Filter by **"saithanyam"** to find error messages
- Common fix: make sure your phone is Android 8.0 (API 26) or higher

### Wallpaper not showing dots
- Make sure you entered a birthday first in the app
- The wallpaper reads the birthday from storage — if none is saved, it shows a blank black screen

### Build takes too long
- First builds are slow (5+ minutes) — this is normal
- Later builds will be much faster (under 1 minute)
- Close other heavy programs to free up RAM

---

## Project File Map (What Each File Does)

```
Github-Action/
├── settings.gradle.kts          -- Tells Gradle this is an Android project
├── build.gradle.kts             -- Project-level build config (plugin versions)
├── gradle.properties            -- Gradle memory and feature settings
├── gradle/wrapper/
│   └── gradle-wrapper.properties -- Gradle version (8.5)
│
└── app/
    ├── build.gradle.kts         -- App dependencies (Compose, Crypto, etc.)
    ├── proguard-rules.pro       -- Code shrinking rules for release builds
    │
    └── src/main/
        ├── AndroidManifest.xml  -- App registration: activity + wallpaper service
        │
        ├── java/com/saithanyam/wallpaper/
        │   ├── ui/
        │   │   ├── MainActivity.kt        -- App entry point
        │   │   ├── screen/
        │   │   │   └── SetupScreen.kt     -- The one and only screen (birthday + stats)
        │   │   └── theme/
        │   │       └── Theme.kt           -- Dark theme colors
        │   │
        │   ├── viewmodel/
        │   │   └── SetupViewModel.kt      -- Manages UI state (MVVM pattern)
        │   │
        │   ├── data/
        │   │   └── BirthdayRepository.kt  -- Saves/loads birthday (encrypted)
        │   │
        │   ├── service/
        │   │   └── SaithanyamWallpaperService.kt  -- Draws 4,000 dots on wallpaper
        │   │
        │   └── util/
        │       ├── WeekCalculator.kt      -- Math: weeks lived / remaining
        │       └── ShareCardGenerator.kt  -- Creates "I have X weeks left" image
        │
        └── res/
            ├── values/strings.xml         -- App name and descriptions
            ├── values/themes.xml          -- App theme (black background)
            ├── xml/wallpaper.xml          -- Wallpaper service config
            ├── drawable/                  -- Icon graphics
            └── mipmap-hdpi/               -- Launcher icon
```

---

## Quick Reference: Key Numbers

| Item | Value |
|------|-------|
| Total weeks in a life | 4,000 (about 76.9 years) |
| Min Android version | 8.0 (API 26) |
| Target Android version | 14 (API 34) |
| Wallpaper redraw interval | Once per hour |
| Share card image size | 1080 x 1080 pixels |
