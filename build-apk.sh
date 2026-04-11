#!/usr/bin/env bash
# ==============================================================================
# Saithanyam APK Build Script for WSL (Windows Subsystem for Linux)
# ==============================================================================
# Builds a debug APK of the Saithanyam live wallpaper app.
# Handles prerequisite checks, SDK setup, and Gradle build in one command.
#
# Usage:   ./build-apk.sh            # builds debug APK
#          ./build-apk.sh release    # builds release APK (unsigned)
#          ./build-apk.sh clean      # cleans build directory
# ==============================================================================

set -e  # Exit on any error

# ---------- Colors for output ----------
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No color

log_info()    { echo -e "${BLUE}[INFO]${NC} $1"; }
log_ok()      { echo -e "${GREEN}[OK]${NC} $1"; }
log_warn()    { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error()   { echo -e "${RED}[ERROR]${NC} $1"; }
log_section() { echo -e "\n${BLUE}========== $1 ==========${NC}"; }

# ---------- Resolve project directory ----------
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

log_section "Saithanyam APK Builder"
log_info "Project directory: $PROJECT_DIR"

# ---------- Parse argument ----------
BUILD_TYPE="${1:-debug}"
case "$BUILD_TYPE" in
    debug|release|clean) ;;
    *)
        log_error "Unknown argument: $BUILD_TYPE"
        echo "Usage: $0 [debug|release|clean]"
        exit 1
        ;;
esac

# ==============================================================================
# STEP 1: Check Java 17
# ==============================================================================
log_section "Step 1: Checking Java 17"

if ! command -v java >/dev/null 2>&1; then
    log_error "Java is not installed."
    echo "Install it with:"
    echo "  sudo apt update && sudo apt install -y openjdk-17-jdk"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n 1 | awk -F '"' '{print $2}' | cut -d '.' -f 1)
if [ "$JAVA_VERSION" != "17" ]; then
    log_warn "Java $JAVA_VERSION found, but Java 17 is required."
    echo "Install it with:"
    echo "  sudo apt update && sudo apt install -y openjdk-17-jdk"
    echo "  sudo update-alternatives --config java   # pick Java 17"
    exit 1
fi

# Set JAVA_HOME if not set
if [ -z "$JAVA_HOME" ]; then
    export JAVA_HOME="$(dirname $(dirname $(readlink -f $(which java))))"
fi
log_ok "Java 17 found at $JAVA_HOME"

# ==============================================================================
# STEP 2: Check / Install Android SDK
# ==============================================================================
log_section "Step 2: Checking Android SDK"

# Default SDK location in WSL home
if [ -z "$ANDROID_HOME" ]; then
    export ANDROID_HOME="$HOME/Android/Sdk"
fi
export ANDROID_SDK_ROOT="$ANDROID_HOME"

SDKMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"

if [ ! -f "$SDKMANAGER" ]; then
    log_warn "Android SDK command-line tools not found at $SDKMANAGER"
    log_info "Installing Android SDK command-line tools..."

    mkdir -p "$ANDROID_HOME/cmdline-tools"
    cd /tmp
    CLI_TOOLS_ZIP="commandlinetools-linux-11076708_latest.zip"
    if [ ! -f "$CLI_TOOLS_ZIP" ]; then
        log_info "Downloading $CLI_TOOLS_ZIP ..."
        wget -q "https://dl.google.com/android/repository/$CLI_TOOLS_ZIP" \
            || { log_error "Failed to download Android command-line tools"; exit 1; }
    fi

    log_info "Extracting command-line tools..."
    unzip -q -o "$CLI_TOOLS_ZIP" -d "$ANDROID_HOME/cmdline-tools"
    mv "$ANDROID_HOME/cmdline-tools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
    cd "$PROJECT_DIR"
    log_ok "Android command-line tools installed"
fi

export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"

# Accept licenses (required before installing packages)
log_info "Accepting Android SDK licenses..."
yes | "$SDKMANAGER" --licenses >/dev/null 2>&1 || true

# Install required SDK packages
log_info "Ensuring required SDK packages are installed..."
"$SDKMANAGER" \
    "platform-tools" \
    "platforms;android-34" \
    "build-tools;34.0.0" >/dev/null

log_ok "Android SDK ready at $ANDROID_HOME"

# ==============================================================================
# STEP 3: Create / verify local.properties
# ==============================================================================
log_section "Step 3: Configuring local.properties"

LOCAL_PROPS="$PROJECT_DIR/local.properties"
if [ ! -f "$LOCAL_PROPS" ]; then
    echo "sdk.dir=$ANDROID_HOME" > "$LOCAL_PROPS"
    log_ok "Created local.properties"
else
    log_ok "local.properties already exists"
fi

# ==============================================================================
# STEP 4: Ensure Gradle is available
# ==============================================================================
log_section "Step 4: Checking Gradle"

# The repo ships a stub gradlew; use system gradle if real wrapper is missing
GRADLEW="$PROJECT_DIR/gradlew"
WRAPPER_JAR="$PROJECT_DIR/gradle/wrapper/gradle-wrapper.jar"

if [ ! -f "$WRAPPER_JAR" ]; then
    log_warn "Gradle wrapper jar missing — using system Gradle"
    if ! command -v gradle >/dev/null 2>&1; then
        log_info "Installing Gradle 8.5 to ~/.gradle-dist ..."
        GRADLE_DIR="$HOME/.gradle-dist"
        mkdir -p "$GRADLE_DIR"
        cd /tmp
        if [ ! -f gradle-8.5-bin.zip ]; then
            wget -q https://services.gradle.org/distributions/gradle-8.5-bin.zip
        fi
        unzip -q -o gradle-8.5-bin.zip -d "$GRADLE_DIR"
        export PATH="$GRADLE_DIR/gradle-8.5/bin:$PATH"
        cd "$PROJECT_DIR"
        log_ok "Gradle 8.5 installed"
    fi
    GRADLE_CMD="gradle"
else
    chmod +x "$GRADLEW"
    GRADLE_CMD="$GRADLEW"
fi

log_ok "Using: $GRADLE_CMD ($(${GRADLE_CMD} --version 2>/dev/null | grep Gradle | head -n 1))"

# ==============================================================================
# STEP 5: Build
# ==============================================================================
log_section "Step 5: Building APK"

case "$BUILD_TYPE" in
    clean)
        log_info "Running clean..."
        $GRADLE_CMD clean
        log_ok "Clean complete"
        exit 0
        ;;
    debug)
        log_info "Building debug APK (this can take 5-15 minutes on first run)..."
        $GRADLE_CMD assembleDebug
        APK_PATH="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"
        ;;
    release)
        log_info "Building release APK (unsigned, this can take 5-15 minutes)..."
        $GRADLE_CMD assembleRelease
        APK_PATH="$PROJECT_DIR/app/build/outputs/apk/release/app-release-unsigned.apk"
        ;;
esac

# ==============================================================================
# STEP 6: Report result
# ==============================================================================
log_section "Step 6: Result"

if [ -f "$APK_PATH" ]; then
    APK_SIZE=$(du -h "$APK_PATH" | cut -f 1)
    log_ok "APK built successfully!"
    echo ""
    echo "  File: $APK_PATH"
    echo "  Size: $APK_SIZE"
    echo ""

    # Copy to Windows Desktop if running in WSL
    if grep -qi microsoft /proc/version 2>/dev/null; then
        WIN_USER=$(powershell.exe '$env:UserName' 2>/dev/null | tr -d '\r\n' || true)
        if [ -n "$WIN_USER" ]; then
            WIN_DESKTOP="/mnt/c/Users/$WIN_USER/Desktop"
            if [ -d "$WIN_DESKTOP" ]; then
                cp "$APK_PATH" "$WIN_DESKTOP/Saithanyam-${BUILD_TYPE}.apk"
                log_ok "Copied to Windows Desktop: Saithanyam-${BUILD_TYPE}.apk"
            fi
        fi
    fi

    echo ""
    echo "To install on your phone:"
    echo "  1. Copy the APK to your phone (via USB, Google Drive, email, etc.)"
    echo "  2. On your phone, enable 'Install from unknown sources' in Settings"
    echo "  3. Tap the APK file on your phone to install"
    echo ""
    echo "Or install via ADB (phone connected via USB debugging):"
    echo "  adb install \"$APK_PATH\""
else
    log_error "Build finished but APK not found at expected location:"
    log_error "  $APK_PATH"
    exit 1
fi
