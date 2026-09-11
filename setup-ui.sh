#!/data/data/com.termux/files/usr/bin/bash
set -u

HOME_DIR="${HOME:-/data/data/com.termux/files/home}"
PREFIX="${PREFIX:-/data/data/com.termux/files/usr}"
BACKUP="$HOME_DIR/.pc-darki-backups/$(date +%Y%m%d-%H%M%S)"
mkdir -p "$BACKUP"
cp -a "$HOME_DIR/.config/xfce4" "$BACKUP/xfce4" 2>/dev/null || true

# Stop the old panel before rebuilding its configuration.
xfce4-panel -q >/dev/null 2>&1 || true

# Dark theme + modern icons.
for theme in Greybird-dark Adwaita-dark; do
  if [ -d "$PREFIX/share/themes/$theme" ]; then
    xfconf-query -c xsettings -p /Net/ThemeName -n -t string -s "$theme" 2>/dev/null || true
    break
  fi
done
for icon in Papirus-Dark Papirus Adwaita hicolor; do
  if [ -d "$PREFIX/share/icons/$icon" ]; then
    xfconf-query -c xsettings -p /Net/IconThemeName -n -t string -s "$icon" 2>/dev/null || true
    break
  fi
done

xfconf-query -c xfwm4 -p /general/use_compositing -n -t bool -s true 2>/dev/null || true
xfconf-query -c xfce4-desktop -p /desktop-icons/icon-size -n -t int -s 48 2>/dev/null || true

# Make sure the modern taskbar plugin exists when available in the Termux X11 repo.
if command -v pkg >/dev/null 2>&1; then
  pkg install -y xfce4-docklike-plugin xfce4-whiskermenu-plugin >/dev/null 2>&1 || true
fi

# Remove every old panel and every old panel plugin from Xfconf.
PANEL_IDS=$(xfconf-query -c xfce4-panel -l 2>/dev/null | sed -n 's#^/panels/panel-\([0-9][0-9]*\).*#\1#p' | sort -nu)
for id in $PANEL_IDS; do
  xfconf-query -c xfce4-panel -p "/panels/panel-$id" -r -R 2>/dev/null || true
done

PLUGIN_IDS=$(xfconf-query -c xfce4-panel -l 2>/dev/null | sed -n 's#^/plugins/plugin-\([0-9][0-9]*\).*#\1#p' | sort -nu)
for id in $PLUGIN_IDS; do
  xfconf-query -c xfce4-panel -p "/plugins/plugin-$id" -r -R 2>/dev/null || true
done

xfconf-query -c xfce4-panel -p /panels -r -R 2>/dev/null || true
xfconf-query -c xfce4-panel -p /panels -n -t int -s 1 -a 2>/dev/null || true

P="/panels/panel-1"

# A floating, centered bottom taskbar (Windows 11-inspired).
xfconf-query -c xfce4-panel -p "$P/length" -n -t uint -s 78
xfconf-query -c xfce4-panel -p "$P/length-adjust" -n -t bool -s false
xfconf-query -c xfce4-panel -p "$P/mode" -n -t uint -s 0
xfconf-query -c xfce4-panel -p "$P/nrows" -n -t uint -s 1
xfconf-query -c xfce4-panel -p "$P/position" -n -t string -s 'p=10;x=0;y=0'
xfconf-query -c xfce4-panel -p "$P/position-locked" -n -t bool -s true
xfconf-query -c xfce4-panel -p "$P/size" -n -t uint -s 54
xfconf-query -c xfce4-panel -p "$P/icon-size" -n -t uint -s 32
xfconf-query -c xfce4-panel -p "$P/autohide-behavior" -n -t uint -s 0
xfconf-query -c xfce4-panel -p "$P/background-style" -n -t uint -s 1
xfconf-query -c xfce4-panel -p "$P/background-alpha" -n -t uint -s 92
xfconf-query -c xfce4-panel -p "$P/enter-opacity" -n -t uint -s 100
xfconf-query -c xfce4-panel -p "$P/leave-opacity" -n -t uint -s 100

if [ -f "$PREFIX/share/xfce4/panel/plugins/whiskermenu.desktop" ] || [ -f "$PREFIX/share/xfce4/panel-plugins/whiskermenu.desktop" ]; then
  START="whiskermenu"
else
  START="applicationsmenu"
fi

# 1 Start | 2 expandable space | 3 Docklike | 4 expandable space | 5 tray | 6 clock
xfconf-query -c xfce4-panel -p /plugins/plugin-1 -n -t string -s "$START"
xfconf-query -c xfce4-panel -p /plugins/plugin-2 -n -t string -s separator
xfconf-query -c xfce4-panel -p /plugins/plugin-3 -n -t string -s docklike
xfconf-query -c xfce4-panel -p /plugins/plugin-4 -n -t string -s separator
xfconf-query -c xfce4-panel -p /plugins/plugin-5 -n -t string -s systray
xfconf-query -c xfce4-panel -p /plugins/plugin-6 -n -t string -s clock

# Start button: compact Windows-style menu button.
xfconf-query -c xfce4-panel -p /plugins/plugin-1/button-title -n -t string -s 'Start' 2>/dev/null || true
xfconf-query -c xfce4-panel -p /plugins/plugin-1/show-button-title -n -t bool -s false 2>/dev/null || true
xfconf-query -c xfce4-panel -p /plugins/plugin-1/show-menu-icons -n -t bool -s true 2>/dev/null || true
xfconf-query -c xfce4-panel -p /plugins/plugin-1/show-tooltips -n -t bool -s false 2>/dev/null || true

# Expandable separators push Docklike into the exact center.
for id in 2 4; do
  xfconf-query -c xfce4-panel -p "/plugins/plugin-$id/expand" -n -t bool -s true
  xfconf-query -c xfce4-panel -p "/plugins/plugin-$id/style" -n -t uint -s 0
  xfconf-query -c xfce4-panel -p "/plugins/plugin-$id/transparent" -n -t bool -s true 2>/dev/null || true
done

# Keep the tray compact.
xfconf-query -c xfce4-panel -p /plugins/plugin-5/show-frame -n -t bool -s false 2>/dev/null || true
xfconf-query -c xfce4-panel -p /plugins/plugin-5/size-max -n -t uint -s 24 2>/dev/null || true

# Modern clock.
xfconf-query -c xfce4-panel -p /plugins/plugin-6/digital-format -n -t string -s '%H:%M' 2>/dev/null || true
xfconf-query -c xfce4-panel -p /plugins/plugin-6/digital-layout -n -t uint -s 3 2>/dev/null || true
xfconf-query -c xfce4-panel -p /plugins/plugin-6/digital-time-font -n -t string -s 'Sans Bold 10' 2>/dev/null || true

# Exact plugin order.
xfconf-query -c xfce4-panel -p "$P/plugin-ids" -n \
  -t int -s 1 -t int -s 2 -t int -s 3 -t int -s 4 -t int -s 5 -t int -s 6

# Useful tablet/keyboard shortcuts.
for keycmd in \
  '<Super>e|thunar' \
  '<Super>w|firefox' \
  '<Super>t|xfce4-terminal' \
  '<Super>d|xfdesktop --toggle-desktop'; do
  key="${keycmd%%|*}"
  cmd="${keycmd#*|}"
  xfconf-query -c xfce4-keyboard-shortcuts -p "/commands/custom/$key" -n -t string -s "$cmd" 2>/dev/null || true
done

# Small PC-DARKI GTK polish.
mkdir -p "$HOME_DIR/.config/gtk-3.0"
cat > "$HOME_DIR/.config/gtk-3.0/gtk.css" <<'CSS'
/* PC-DARKI: restrained dark UI polish */
#XfcePanelWindow {
  border-radius: 18px;
}
CSS

xfce4-panel >/dev/null 2>&1 &
sleep 2
xfce4-panel -r >/dev/null 2>&1 || true

printf 'PC-DARKI UI v2 ready. Centered Docklike taskbar configured. Backup=%s\n' "$BACKUP"
