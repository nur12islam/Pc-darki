#!/data/data/com.termux/files/usr/bin/bash
set -e

# PC-DARKI — Termux + Termux:X11 + XFCE setup
# Designed for the user's arm64 Termux/F-Droid environment.

PREFIX="${PREFIX:-/data/data/com.termux/files/usr}"
HOME_DIR="${HOME:-/data/data/com.termux/files/home}"
REPO_URL="https://github.com/nur12islam/Pc-darki.git"
BIN_DIR="$PREFIX/bin"
DESKTOP_DIR="$HOME_DIR/.local/share/applications"
BACKUP_DIR="$HOME_DIR/.pc-darki-backup"

printf '\n=== PC-DARKI installer ===\n\n'

if [ "$(uname -m)" != "aarch64" ]; then
  echo "Warning: this installer was designed for aarch64/arm64 Termux."
fi

mkdir -p "$BACKUP_DIR" "$DESKTOP_DIR"

# Enable the X11 repository when needed.
if ! command -v termux-x11-preference >/dev/null 2>&1; then
  echo "[1/7] Enabling Termux:X11 repository..."
  pkg install -y x11-repo
fi

# Update package metadata.
echo "[2/7] Updating packages..."
pkg update -y

# Core desktop + useful lightweight applications.
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
  pgrep

# Android shared storage.
echo "[4/7] Preparing Android storage..."
if [ ! -d "$HOME_DIR/storage" ]; then
  termux-setup-storage || true
fi
mkdir -p "$HOME_DIR/Desktop"
if [ -d "$HOME_DIR/storage/shared" ]; then
  ln -sfn "$HOME_DIR/storage/shared" "$HOME_DIR/Desktop/Android-Storage"
fi

# Termux:X11 preferences: fullscreen, no extra-key bar, trackpad touch mode.
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

# Install the launcher.
echo "[6/7] Installing the pc launcher..."
cat > "$BIN_DIR/pc" <<'PC_EOF'
#!/data/data/com.termux/files/usr/bin/bash
set -u

export DISPLAY=:1
LOG="$HOME/.pc-darki-x11.log"

# Start the X server + XFCE session if it is not already running.
if ! pgrep -f '(^|/)termux-x11( |$)' >/dev/null 2>&1; then
    termux-x11 :1 \
      -xstartup "dbus-launch --exit-with-session xfce4-session" \
      >"$LOG" 2>&1 &
    sleep 3
fi

# Bring the Android X11 activity to the foreground.
am start --user 0 \
  -n com.termux.x11/com.termux.x11.MainActivity \
  >/dev/null 2>&1 || true
PC_EOF
chmod 755 "$BIN_DIR/pc"

# Install Ani command if ani-cli is already available; otherwise leave a helper
# so the user can install/update ani-cli separately without touching the desktop.
cat > "$BIN_DIR/Ani" <<'ANI_EOF'
#!/data/data/com.termux/files/usr/bin/bash
if ! command -v ani-cli >/dev/null 2>&1; then
  echo "ani-cli is not installed. Install it first, then run: Ani"
  exit 1
fi
exec ani-cli --dub -q best "$@"
ANI_EOF
chmod 755 "$BIN_DIR/Ani"

# Handy desktop entries for applications and Android storage.
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

# Preserve a small marker for future upgrades.
printf '%s\n' "PC-DARKI installed on $(date -u '+%Y-%m-%d %H:%M:%S UTC')" > "$HOME_DIR/.pc-darki-version"

# Optional first-run XFCE panel basics. Only touch the panel when an XFCE
# configuration daemon is already available; never fail the installation on UI tweaks.
echo "[7/7] Applying safe XFCE defaults..."
if command -v xfconf-query >/dev/null 2>&1 && pgrep -f 'xfconfd' >/dev/null 2>&1; then
  xfconf-query -c xfce4-panel -p /panels/panel-0/length -s 100 2>/dev/null || true
  xfconf-query -c xfce4-panel -p /panels/panel-0/length-adjust -s true 2>/dev/null || true
  xfconf-query -c xfce4-panel -p /panels/panel-0/size -s 42 2>/dev/null || true
  xfconf-query -c xfce4-panel -p /panels/panel-0/autohide-behavior -s 0 2>/dev/null || true
  xfconf-query -c xfce4-panel -p /panels/panel-0/position -s 'p=8;x=0;y=0' 2>/dev/null || true
  xfconf-query -c xfce4-panel -p /panels/panel-0/position-locked -s true 2>/dev/null || true
fi

echo
printf 'PC-DARKI setup complete.\n\n'
echo 'Start the desktop with:  pc'
echo 'Anime command:          Ani <title>'
echo 'Android Storage:        ~/Desktop/Android-Storage'
echo
echo 'If Android asks for storage permission, allow it and rerun: pc'
