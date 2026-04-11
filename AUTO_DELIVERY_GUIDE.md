# Saithanyam Auto-Build & Deliver Guide

One command builds the APK, uploads it to Google Drive, emails you a link, and **deletes all the code** from your laptop automatically.

---

## Is This Possible? Yes. Here Is What It Requires

| Requirement | What It Is | Cost |
|---|---|---|
| **WSL** (Windows Subsystem for Linux) | Already set up from previous guide | Free |
| **Java 17** | Already installed | Free |
| **rclone** | A tool that talks to Google Drive | Free |
| **Google Account** | For Google Drive storage | Free |
| **Gmail App Password** | Special password for scripts to send email | Free |
| **msmtp** | Small Linux tool that sends email | Free |

**Important limitations to know:**
- Gmail **blocks APK files as email attachments** (Google security policy — not our limitation). So the APK goes to **Google Drive**, and you get an **email with the download link**.
- The Android SDK (~1 GB) is downloaded **once** and stays on your laptop permanently — only the cloned source code is deleted.

---

## Two Scripts Explained

| Script | When to run | What it does |
|---|---|---|
| `setup-delivery.sh` | **Once only** | Installs rclone + msmtp, connects Google Drive, saves Gmail App Password |
| `auto-build-deliver.sh` | **Every time** | Clones → Builds → Uploads → Emails → Deletes clone |

---

## Part A — One-Time Setup

### Step 1 — Get a Gmail App Password

A Gmail App Password is a special 16-character password that lets scripts send email on your behalf. It is NOT your normal Gmail password.

1. Open `myaccount.google.com` in your browser
2. Click **Security** in the left sidebar
3. Scroll to **"How you sign in to Google"**
4. Click **2-Step Verification** — make sure it is **ON** (required)
5. Scroll to the bottom of that page
6. Click **App passwords**
7. Under "App name", type: `Saithanyam`
8. Click **Create**
9. A 16-character password appears — **copy it now** (you will not see it again)
   - It looks like: `abcd efgh ijkl mnop`
   - The spaces do not matter — the script removes them

Keep this password ready for the next step.

### Step 2 — Run the Setup Script

Open WSL (Ubuntu terminal) and navigate to the project:

```
cd ~/Github-Action
chmod +x setup-delivery.sh
./setup-delivery.sh
```

The setup wizard will:

1. **Install rclone** automatically
2. **Open the Google Drive connection wizard:**
   - It asks: `name>` — type exactly: `gdrive` and press Enter
   - It asks: `Storage>` — type: `drive` and press Enter
   - It asks for Client ID and Secret — just press **Enter** (leave blank, use defaults)
   - It asks about scopes — press **Enter** (full access)
   - It asks about root folder — press **Enter**
   - It asks `Use auto config?` — type `y` and press Enter
   - A browser window opens — **sign in with your Google account** and click Allow
   - Come back to the terminal and press Enter to continue
   - It asks `Configure this as a Shared Drive?` — type `n` and press Enter
   - It asks `Yes this is OK` — type `y` and press Enter
   - Type `q` to quit the rclone config menu
3. **Ask for your Gmail address** — type it and press Enter
4. **Ask for your Gmail App Password** — paste the 16-character password (you will not see it typed — this is normal) and press Enter
5. **Send a test email** — check your inbox to confirm it worked
6. **Save everything** to `~/.saithanyam_delivery.conf`

Setup is complete. You never need to run this again.

---

## Part B — Building and Delivering (Every Time)

### Step 3 — Run the Main Script

```
cd ~/Github-Action
chmod +x auto-build-deliver.sh
./auto-build-deliver.sh
```

That is it. One command. Watch the output:

```
══════════════ Step 1: Versioning ══════════════
[INFO] Build number : #1
[INFO] APK filename : Saithanyam-b001-2026-04-11.apk
[INFO] Build type   : debug

══════════════ Step 2: Java 17 ══════════════
[OK] Java 17 at /usr/lib/jvm/java-17-openjdk-amd64

══════════════ Step 3: Cloning Repo ══════════════
[INFO] Cloning https://github.com/...
[OK] Repo cloned to /tmp/saithanyam-20260411_2038/source

══════════════ Step 4: Android SDK ══════════════
[OK] Android SDK ready at /home/you/Android/Sdk

══════════════ Step 5: Gradle ══════════════
[OK] Gradle: Gradle 8.5

══════════════ Step 6: Building APK (debug) ══════════════
[INFO] This takes 5-15 min on first run, ~1 min later...
> Task :app:assembleDebug
BUILD SUCCESSFUL
[OK] APK built: 9.5M
[OK] Versioned APK: Saithanyam-b001-2026-04-11.apk

══════════════ Step 7: Uploading to Google Drive ══════════════
[INFO] Uploading to gdrive:Saithanyam-Builds/ ...
[OK] Upload complete

══════════════ Step 8: Gmail Notification ══════════════
[OK] Notification sent to yourname@gmail.com

══════════════ Step 10: Cleanup ══════════════
[INFO] Cleaning up temp directory...
[OK] Temp clone deleted: /tmp/saithanyam-20260411_2038

══════════════════════════════════════════════
  BUILD COMPLETE
══════════════════════════════════════════════
  Build   : #1 (debug)
  APK     : Saithanyam-b001-2026-04-11.apk
  Drive   : gdrive:Saithanyam-Builds/
  Email   : yourname@gmail.com
  Cleanup : temp clone deleted automatically
```

### Step 4 — Get the APK on Your Phone

**From the email:**
1. Open the email: `[Saithanyam] Build #1 ready`
2. Tap the Google Drive link
3. In Google Drive, tap the APK file → three-dot menu → **Download**
4. Tap the downloaded APK to install

**Or from Google Drive directly:**
1. Open Google Drive on your phone
2. Look for a folder called **Saithanyam-Builds**
3. Tap the APK file → tap **Download** (or three-dot → Open with)
4. Tap the downloaded APK to install (allow unknown sources if asked)

---

## Versioning System

Each successful build gets a unique name:

```
Saithanyam-b001-2026-04-11.apk    ← Build 1, April 11 2026
Saithanyam-b002-2026-04-12.apk    ← Build 2, April 12 2026
Saithanyam-b003-2026-04-15.apk    ← Build 3, April 15 2026
```

- The build counter is stored in `~/.saithanyam_build_counter`
- It increments by 1 for every **successful** upload
- If a build fails, the counter does NOT increment (no version is skipped)
- All APKs are kept in Google Drive — nothing is automatically deleted from Drive

---

## Command Options

```bash
./auto-build-deliver.sh            # default type (set during setup, usually 'debug')
./auto-build-deliver.sh debug      # force debug build
./auto-build-deliver.sh release    # force release build (optimized, smaller)
```

---

## What Gets Deleted vs What Stays

| Item | Location | Deleted? |
|---|---|---|
| Cloned source code | `/tmp/saithanyam-TIMESTAMP/` | **Yes — always** |
| Built APK (local copy) | Same temp folder | **Yes — always** |
| Android SDK | `~/Android/Sdk/` | No — kept forever |
| Gradle build cache | `~/.gradle/` | No — kept for speed |
| Your config | `~/.saithanyam_delivery.conf` | No — kept |
| Version counter | `~/.saithanyam_build_counter` | No — kept |

The cleanup happens automatically even if the build **fails** or you press **Ctrl+C** — the `trap` command in the script guarantees it.

---

## Troubleshooting

### "Config not found" error
Run setup first:
```
./setup-delivery.sh
```

### rclone upload fails
Check your Google Drive auth is still valid:
```
rclone lsd gdrive:
```
If it fails, re-run setup to refresh the connection:
```
./setup-delivery.sh
```

### Email notification not received
1. Check your spam folder
2. Make sure 2-Step Verification is ON in your Google Account
3. Check if the App Password is correct:
   ```
   cat ~/.msmtprc
   ```
4. Re-run setup to enter a new App Password:
   ```
   ./setup-delivery.sh
   ```

### Build fails: "SDK not found"
The script handles this automatically. But if it persists:
```
echo $ANDROID_HOME
ls ~/Android/Sdk/
```
If the folder is missing, the script will re-download the SDK on next run.

### "Permission denied" running scripts
Make scripts executable:
```
chmod +x setup-delivery.sh auto-build-deliver.sh
```

### Want to reset the version counter to zero
```
echo "0" > ~/.saithanyam_build_counter
```
Next build will be #1 again.

### Want to change the Gmail or Drive folder
Edit the config file directly:
```
nano ~/.saithanyam_delivery.conf
```
Or re-run setup: `./setup-delivery.sh`

---

## Full Workflow Summary

```
First time only:
  ./setup-delivery.sh     ← connects Google Drive + Gmail

Every build after that:
  ./auto-build-deliver.sh ← clone → build → upload → email → delete
```

Your laptop stays clean. Your APK is in Drive. Your email has the link.
