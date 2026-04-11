#!/usr/bin/env bash
# ==============================================================================
# Saithanyam Auto-Build & Deliver Script
# ==============================================================================
# Does everything in one command:
#   1. Reads config from ~/.saithanyam_delivery.conf
#   2. Increments version counter
#   3. Clones the repo to a TEMP folder
#   4. Installs Android SDK if not present (SDK stays, temp code is deleted)
#   5. Builds the APK
#   6. Renames APK with version: Saithanyam-b001-2026-04-11.apk
#   7. Uploads APK to Google Drive
#   8. Sends Gmail notification with Drive link
#   9. DELETES the temp clone completely
#
# Usage:
#   ./auto-build-deliver.sh           # use default build type from config
#   ./auto-build-deliver.sh debug     # force debug build
#   ./auto-build-deliver.sh release   # force release build
#
# Run setup-delivery.sh ONCE before using this script.
# ==============================================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

log_info()    { echo -e "${BLUE}[INFO]${NC} $1"; }
log_ok()      { echo -e "${GREEN}[OK]${NC} $1"; }
log_warn()    { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error()   { echo -e "${RED}[ERROR]${NC} $1"; }
log_section() { echo -e "\n${CYAN}══════════════ $1 ══════════════${NC}"; }

# ---------- Timestamp + unique temp dir ----------
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
DATE_PRETTY=$(date +%Y-%m-%d)
TEMP_DIR="/tmp/saithanyam-${TIMESTAMP}"

# ---------- Cleanup trap — always delete temp dir on exit ----------
cleanup() {
    if [ -d "$TEMP_DIR" ]; then
        log_info "Cleaning up temp directory..."
        rm -rf "$TEMP_DIR"
        log_ok "Temp clone deleted: $TEMP_DIR"
    fi
}
trap cleanup EXIT      # runs on any exit (success, error, or Ctrl+C)

# ==============================================================================
# PRE-FLIGHT: Read config
# ==============================================================================
CONFIG_FILE="$HOME/.saithanyam_delivery.conf"
VERSION_FILE="$HOME/.saithanyam_build_counter"

if [ ! -f "$CONFIG_FILE" ]; then
    log_error "Config not found: $CONFIG_FILE"
    echo "Please run setup-delivery.sh first:"
    echo "  ./setup-delivery.sh"
    exit 1
fi

# shellcheck source=/dev/null
source "$CONFIG_FILE"

# Override build type if argument given
BUILD_TYPE="${1:-$DEFAULT_BUILD_TYPE}"
BUILD_TYPE="${BUILD_TYPE:-debug}"

case "$BUILD_TYPE" in
    debug|release) ;;
    *)
        log_error "Unknown build type: $BUILD_TYPE — use 'debug' or 'release'"
        exit 1
        ;;
esac

# ==============================================================================
# STEP 1: Increment version counter
# ==============================================================================
log_section "Step 1: Versioning"

if [ ! -f "$VERSION_FILE" ]; then
    echo "0" > "$VERSION_FILE"
fi
CURRENT=$(cat "$VERSION_FILE")
BUILD_NUM=$((CURRENT + 1))
BUILD_NUM_PADDED=$(printf "%03d" "$BUILD_NUM")

APK_VERSIONED_NAME="Saithanyam-b${BUILD_NUM_PADDED}-${DATE_PRETTY}.apk"

log_info "Build number : #${BUILD_NUM}"
log_info "APK filename : $APK_VERSIONED_NAME"
log_info "Build type   : $BUILD_TYPE"

# ==============================================================================
# STEP 2: Check Java 17
# ==============================================================================
log_section "Step 2: Java 17"

if ! command -v java >/dev/null 2>&1; then
    log_error "Java 17 is not installed. Run:"
    echo "  sudo apt install -y openjdk-17-jdk"
    exit 1
fi
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | awk -F '"' '{print $2}' | cut -d '.' -f 1)
if [ "$JAVA_VERSION" != "17" ]; then
    log_error "Java $JAVA_VERSION found, need Java 17. Run:"
    echo "  sudo apt install -y openjdk-17-jdk"
    echo "  sudo update-alternatives --config java"
    exit 1
fi
if [ -z "$JAVA_HOME" ]; then
    export JAVA_HOME
    JAVA_HOME="$(dirname "$(dirname "$(readlink -f "$(which java)")")")"
fi
log_ok "Java 17 at $JAVA_HOME"

# ==============================================================================
# STEP 3: Clone the repo to temp dir
# ==============================================================================
log_section "Step 3: Cloning Repo"

mkdir -p "$TEMP_DIR"
log_info "Cloning ${REPO_URL} (branch: ${REPO_BRANCH})..."
git clone \
    --depth=1 \
    --branch "$REPO_BRANCH" \
    "$REPO_URL" \
    "$TEMP_DIR/source" \
    2>&1 | tail -3
log_ok "Repo cloned to $TEMP_DIR/source"

SOURCE_DIR="$TEMP_DIR/source"

# ==============================================================================
# STEP 4: Android SDK
# ==============================================================================
log_section "Step 4: Android SDK"

# SDK stays permanently at ~/Android/Sdk — NOT in the temp dir
if [ -z "$ANDROID_HOME" ]; then
    export ANDROID_HOME="$HOME/Android/Sdk"
fi
export ANDROID_SDK_ROOT="$ANDROID_HOME"
SDKMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"

if [ ! -f "$SDKMANAGER" ]; then
    log_info "Installing Android SDK command-line tools..."
    mkdir -p "$ANDROID_HOME/cmdline-tools"
    cd /tmp
    CLI_TOOLS_ZIP="commandlinetools-linux-11076708_latest.zip"
    [ ! -f "$CLI_TOOLS_ZIP" ] && wget -q "https://dl.google.com/android/repository/$CLI_TOOLS_ZIP"
    unzip -q -o "$CLI_TOOLS_ZIP" -d "$ANDROID_HOME/cmdline-tools"
    mv "$ANDROID_HOME/cmdline-tools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
    cd "$SOURCE_DIR"
    log_ok "Android cmdline-tools installed"
fi

log_info "Accepting licenses and ensuring packages..."
yes | "$SDKMANAGER" --licenses >/dev/null 2>&1 || true
"$SDKMANAGER" "platform-tools" "platforms;android-34" "build-tools;34.0.0" >/dev/null
log_ok "Android SDK ready at $ANDROID_HOME"

# Write local.properties into the temp clone
echo "sdk.dir=$ANDROID_HOME" > "$SOURCE_DIR/local.properties"

# ==============================================================================
# STEP 5: Gradle
# ==============================================================================
log_section "Step 5: Gradle"

WRAPPER_JAR="$SOURCE_DIR/gradle/wrapper/gradle-wrapper.jar"

if [ ! -f "$WRAPPER_JAR" ]; then
    log_warn "Gradle wrapper jar missing — using system Gradle"
    if ! command -v gradle >/dev/null 2>&1; then
        GRADLE_DIST_DIR="$HOME/.gradle-dist"
        if [ ! -f "$GRADLE_DIST_DIR/gradle-8.5/bin/gradle" ]; then
            log_info "Downloading Gradle 8.5..."
            mkdir -p "$GRADLE_DIST_DIR"
            cd /tmp
            [ ! -f gradle-8.5-bin.zip ] && wget -q https://services.gradle.org/distributions/gradle-8.5-bin.zip
            unzip -q -o gradle-8.5-bin.zip -d "$GRADLE_DIST_DIR"
            log_ok "Gradle 8.5 installed"
        fi
        export PATH="$GRADLE_DIST_DIR/gradle-8.5/bin:$PATH"
    fi
    GRADLE_CMD="gradle"
else
    chmod +x "$SOURCE_DIR/gradlew"
    GRADLE_CMD="$SOURCE_DIR/gradlew"
fi

log_ok "Gradle: $(${GRADLE_CMD} --version 2>/dev/null | grep '^Gradle' | head -1)"

# ==============================================================================
# STEP 6: Build APK
# ==============================================================================
log_section "Step 6: Building APK (${BUILD_TYPE})"

cd "$SOURCE_DIR"
log_info "This takes 5-15 min on first run, ~1 min later (Gradle cache reused)..."

if [ "$BUILD_TYPE" = "release" ]; then
    $GRADLE_CMD assembleRelease 2>&1 | grep -E "^(BUILD|FAILURE|ERROR|> Task|Downloading|Download)" || true
    RAW_APK="$SOURCE_DIR/app/build/outputs/apk/release/app-release-unsigned.apk"
else
    $GRADLE_CMD assembleDebug 2>&1 | grep -E "^(BUILD|FAILURE|ERROR|> Task|Downloading|Download)" || true
    RAW_APK="$SOURCE_DIR/app/build/outputs/apk/debug/app-debug.apk"
fi

if [ ! -f "$RAW_APK" ]; then
    log_error "Build failed — APK not found at $RAW_APK"
    exit 1
fi
log_ok "APK built: $(du -h "$RAW_APK" | cut -f1)"

# Rename with version
VERSIONED_APK="$TEMP_DIR/$APK_VERSIONED_NAME"
cp "$RAW_APK" "$VERSIONED_APK"
log_ok "Versioned APK: $APK_VERSIONED_NAME"

# ==============================================================================
# STEP 7: Upload to Google Drive
# ==============================================================================
log_section "Step 7: Uploading to Google Drive"

log_info "Uploading to gdrive:${DRIVE_FOLDER}/ ..."

if rclone copy "$VERSIONED_APK" "gdrive:${DRIVE_FOLDER}/" \
        --progress \
        --stats-one-line 2>&1; then
    log_ok "Upload complete: gdrive:${DRIVE_FOLDER}/${APK_VERSIONED_NAME}"
    DRIVE_LINK=$(rclone link "gdrive:${DRIVE_FOLDER}/${APK_VERSIONED_NAME}" 2>/dev/null || echo "Check Google Drive")
    UPLOAD_OK="yes"
else
    log_warn "Upload failed. Check your internet connection and rclone config."
    DRIVE_LINK="Upload failed — check Drive manually"
    UPLOAD_OK="no"
fi

# ==============================================================================
# STEP 8: Save version counter ONLY after successful upload
# ==============================================================================
if [ "$UPLOAD_OK" = "yes" ]; then
    echo "$BUILD_NUM" > "$VERSION_FILE"
    log_ok "Version counter saved: $BUILD_NUM"
fi

# ==============================================================================
# STEP 9: Gmail notification
# ==============================================================================
log_section "Step 9: Gmail Notification"

APK_SIZE=$(du -h "$VERSIONED_APK" | cut -f1)
SUBJECT="[Saithanyam] Build #${BUILD_NUM} ready — ${APK_VERSIONED_NAME}"

BODY=$(cat << EMAILEOF
Saithanyam APK Build Report
============================

Build  : #${BUILD_NUM}
File   : ${APK_VERSIONED_NAME}
Type   : ${BUILD_TYPE}
Size   : ${APK_SIZE}
Date   : ${DATE_PRETTY}

Google Drive:
${DRIVE_LINK}

How to install:
1. Open the link above on your phone
2. Download the APK file
3. Tap it to install (allow unknown sources if asked)

-- Auto-build-deliver.sh
EMAILEOF
)

if command -v msmtp >/dev/null 2>&1 && [ -f "$HOME/.msmtprc" ]; then
    printf "Subject: %s\nFrom: %s\nTo: %s\n\n%s\n" \
        "$SUBJECT" "$GMAIL_FROM" "$GMAIL_TO" "$BODY" \
        | msmtp "$GMAIL_TO" 2>/dev/null \
        && log_ok "Notification sent to $GMAIL_TO" \
        || log_warn "Email send failed — check Gmail App Password in ~/.msmtprc"
else
    log_warn "msmtp not configured — skipping email notification"
fi

# ==============================================================================
# STEP 10: Cleanup — trap at EXIT handles this automatically
# ==============================================================================
# (cleanup() function at top of script deletes $TEMP_DIR on any exit)

# ==============================================================================
# Summary
# ==============================================================================
echo ""
echo -e "${GREEN}══════════════════════════════════════════════${NC}"
echo -e "${GREEN}  BUILD COMPLETE                               ${NC}"
echo -e "${GREEN}══════════════════════════════════════════════${NC}"
echo ""
echo "  Build   : #${BUILD_NUM} (${BUILD_TYPE})"
echo "  APK     : ${APK_VERSIONED_NAME}"
echo "  Size    : ${APK_SIZE}"
echo "  Drive   : gdrive:${DRIVE_FOLDER}/"
echo "  Email   : ${GMAIL_TO}"
echo "  Cleanup : temp clone deleted automatically"
echo ""
echo "  To install: open Google Drive on your phone,"
echo "  find '${DRIVE_FOLDER}' folder, tap the APK."
echo ""
