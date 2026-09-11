#!/data/data/com.termux/files/usr/bin/bash
set -e

# PC-DARKI — Termux + Termux:X11 + XFCE setup
# Designed for the user's arm64/F-Droid Termux environment.

PREFIX="${PREFIX:-/data/data/com.termux/files/usr}"
HOME_DIR="${HOME:-/data/data/com.termux/files/home}"
BIN_DIR="$PREFIX/bin"
DESKTOP_DIR="$HOME_DIR/.local/share/applications"
BACKUP_DIR="$HOME_DIR/.pc-darki-backup"

printf '\n=== PC-DARKI installer ===\n\n'

if [ "$(uname -m)" != "aarch64" ]; then
  echo "Warning: this installer was designed for aarch64/arm64 Termux."
fi

mkdir -p "$BACKUP_DIR" "$DESKTOP_DIR"

if ! command -v termux-x11-preference >/dev/null 2>&1; then
  echo "[1/7] Enabling Termux:X11 repository..."
  pkg install -y x11-repo
fi

echo "[2/7] Updating packages..."
pkg update -y

echo "[3/7] Installing desktop packages..."
pkg install -y \
  termux-x11-nightly \
  xfce \
  dbus \
  thunar \
  firefox \
  xfce4-screenshooter \
  mousepad \
  ristretto \
  file-roller \
  unzip \
  p7zip \
  git \
  curl \
  wget \
  nano \
  which \
  procps

echo "[4/7] Preparing Android storage..."
if [ ! -d "$HOME_DIR/storage" ]; then
  termux-setup-storage || true
fi
mkdir -p "$HOME_DIR/Desktop"
if [ -d "$HOME_DIR/storage/shared" ]; then
  ln -sfn "$HOME_DIR/storage/shared" "$HOME_DIR/Desktop/Android-Storage"
fi

echo "[5/7] Configuring Termux:X11..."
if command -v termux-x11-preference >/dev/null 2>&1; then
  termux-x11-preference \
    "fullscreen"="true" \
    "displayScale"="100" \
    "showAdditionalKbd"="false" \
    "touchMode"="Trackpad" \
    "adjustResolution"="false" \
    >/dev/null 2>&1 || true
fi

echo "[6/7] Installing launchers..."
cat > "$BIN_DIR/pc" <<'PC_EOF'
#!/data/data/com.termux/files/usr/bin/bash
set -u
export DISPLAY=:1
LOG="$HOME/.pc-darki-x11.log"

if ! pgrep -x termux-x11 >/dev/null 2>&1; then
    termux-x11 :1 \
      -xstartup "dbus-launch --exit-with-session xfce4-session" \
      >"$LOG" 2>&1 &
    sleep 3
fi

am start --user 0 \
  -n com.termux.x11/com.termux.x11.MainActivity \
  >/dev/null 2>&1 || true
PC_EOF
chmod 755 "$BIN_DIR/pc"

cat > "$BIN_DIR/Ani" <<'ANI_EOF'
#!/data/data/com.termux/files/usr/bin/bash
if ! command -v ani-cli >/dev/null 2>&1; then
  echo "ani-cli is not installed. Install it first, then run: Ani"
  exit 1
fi
exec ani-cli --dub -q best "$@"
ANI_EOF
chmod 755 "$BIN_DIR/Ani"

cat > "$DESKTOP_DIR/firefox-termux.desktop" <<EOF
[Desktop Entry]
Version=1.0
Type=Application
Name=Firefox
Comment=Web browser
Exec=$BIN_DIR/firefox %U
Icon=firefox
Terminal=false
Categories=Network;WebBrowser;
EOF

cat > "$DESKTOP_DIR/android-storage.desktop" <<EOF
[Desktop Entry]
Version=1.0
Type=Application
Name=Android Storage
Comment=Open Android shared storage
Exec=$BIN_DIR/thunar $HOME_DIR/storage/shared
Icon=folder
Terminal=false
Categories=Utility;FileManager;
EOF

printf '%s\n' "PC-DARKI installed on $(date -u '+%Y-%m-%d %H:%M:%S UTC')" > "$HOME_DIR/.pc-darki-version"

echo "[7/7] Finished."
echo
echo "Start desktop:   pc"
echo "Anime command:   Ani <title>"
echo "Android storage: ~/Desktop/Android-Storage"
echo