#!/data/data/com.termux/files/usr/bin/bash
set -u

# PC-DARKI UI layer: a clean Windows-11-inspired / modern-Linux XFCE layout.
# This script intentionally keeps Android as the host OS and only configures XFCE.

HOME_DIR="${HOME:-/data/data/com.termux/files/home}"
PREFIX="${PREFIX:-/data/data/com.termux/files/usr}"
CONFIG="$HOME_DIR/.config/xfce4"
BACKUP="$HOME_DIR/.pc-darki-backups/$(date +%Y%m%d-%H%M%S)"

mkdir -p "$BACKUP"

# Backup existing XFCE configuration before changing it.
if [ -d "$CONFIG" ]; then
  cp -a "$CONFIG" "$BACKUP/xfce4" 2>/dev/null || true
fi

# Stop the panel while changing its configuration.
xfce4-panel -q >/dev/null 2>&1 || true

# ---- Appearance ----
if [ -d "$PREFIX/share/themes/Greybird-dark" ]; then
  GTK_THEME="Greybird-dark"
elif [ -d "$PREFIX/share/themes/Adwaita-dark" ]; then
  GTK_THEME="Adwaita-dark"
else
  GTK_THEME="Default"
fi

if [ -d "$PREFIX/share/icons/Papirus-Dark" ]; then
  ICON_THEME="Papirus-Dark"
elif [ -d "$PREFIX/share/icons/Adwaita" ]; then
  ICON_THEME="Adwaita"
else
  ICON_THEME="hicolor"
fi

xfconf-query -c xsettings -p /Net/ThemeName -n -t string -s "$GTK_THEME" 2>/dev/null || \
xfconf-query -c xsettings -p /Net/ThemeName -s "$GTK_THEME" 2>/dev/null || true
xfconf-query -c xsettings -p /Net/IconThemeName -n -t string -s "$ICON_THEME" 2>/dev/null || \
xfconf-query -c xsettings -p /Net/IconThemeName -s "$ICON_THEME" 2>/dev/null || true

# Dark mode where supported.
xfconf-query -c xfce4-panel -p /panels/dark-mode -n -t bool -s true 2>/dev/null || true
xfconf-query -c xfwm4 -p /general/use_compositing -n -t bool -s true 2>/dev/null || true
xfconf-query -c xfwm4 -p /general/theme -n -t string -s "$GTK_THEME" 2>/dev/null || true

# ---- Desktop ----
xfconf-query -c xfce4-desktop -p /desktop-icons/style -n -t int -s 0 2>/dev/null || true
xfconf-query -c xfce4-desktop -p /desktop-icons/icon-size -n -t int -s 48 2>/dev/null || true
xfconf-query -c xfce4-desktop -p /desktop-icons/font-size -n -t double -s 10 2>/dev/null || true

# ---- Modern single bottom taskbar ----
# Remove the common second panel used by the default XFCE layout.
xfconf-query -c xfce4-panel -p /panels/panel-1 -rR 2>/dev/null || true
xfconf-query -c xfce4-panel -p /panels/panel-2 -rR 2>/dev/null || true

# Ensure panel 0 exists and is the only panel.
xfconf-query -c xfce4-panel -p /panels -rR 2>/dev/null || true
xfconf-query -c xfce4-panel -p /panels -n -t int -s 0 --create 2>/dev/null || true

# Panel geometry: bottom, full width, 48px touch-friendly height.
setp() { xfconf-query -c xfce4-panel -p "$1" "$@" >/dev/null 2>&1 || true; }

xfconf-query -c xfce4-panel -p /panels/panel-0/length -n -t uint -s 100 2>/dev/null || true
xfconf-query -c xfce4-panel -p /panels/panel-0/length-adjust -n -t bool -s false 2>/dev/null || true
xfconf-query -c xfce4-panel -p /panels/panel-0/mode -n -t uint -s 0 2>/dev/null || true
xfconf-query -c xfce4-panel -p /panels/panel-0/nrows -n -t uint -s 1 2>/dev/null || true
xfconf-query -c xfce4-panel -p /panels/panel-0/position -n -t string -s 'p=10;x=0;y=0' 2>/dev/null || true
xfconf-query -c xfce4-panel -p /panels/panel-0/position-locked -n -t bool -s true 2>/dev/null || true
xfconf-query -c xfce4-panel -p /panels/panel-0/size -n -t uint -s 48 2>/dev/null || true
xfconf-query -c xfce4-panel -p /panels/panel-0/autohide-behavior -n -t uint -s 0 2>/dev/null || true
xfconf-query -c xfce4-panel -p /panels/panel-0/background-style -n -t uint -s 1 2>/dev/null || true
xfconf-query -c xfce4-panel -p /panels/panel-0/background-alpha -n -t uint -s 92 2>/dev/null || true
xfconf-query -c xfce4-panel -p /panels/panel-0/icon-size -n -t uint -s 32 2>/dev/null || true

# Clear the visible plugin list. Plugin definitions not in this list are harmless.
xfconf-query -c xfce4-panel -p /panels/panel-0/plugin-ids -rR 2>/dev/null || true

# Modern taskbar components.
# 101 = Whisker application menu (Start-style menu)
# 102 = docklike taskbar (grouped/pinnable application icons)
# 103 = separator spacer
# 104 = system tray
# 105 = clock
# 106 = actions/power menu

make_plugin() {
  local id="$1" type="$2"
  xfconf-query -c xfce4-panel -p "/plugins/plugin-$id" -n -t string -s "$type" 2>/dev/null || \
  xfconf-query -c xfce4-panel -p "/plugins/plugin-$id" -s "$type" 2>/dev/null || true
}

make_plugin 101 whiskermenu
make_plugin 102 docklike
make_plugin 103 separator
make_plugin 104 systray
make_plugin 105 clock
make_plugin 106 actions

xfconf-query -c xfce4-panel -p /plugins/plugin-101/button-title -n -t string -s 'Start' 2>/dev/null || true
xfconf-query -c xfce4-panel -p /plugins/plugin-101/show-button-title -n -t bool -s true 2>/dev/null || true
xfconf-query -c xfce4-panel -p /plugins/plugin-101/show-menu-icons -n -t bool -s true 2>/dev/null || true

xfconf-query -c xfce4-panel -p /plugins/plugin-102/show-frame -n -t bool -s false 2>/dev/null || true
xfconf-query -c xfce4-panel -p /plugins/plugin-103/expand -n -t bool -s true 2>/dev/null || true
xfconf-query -c xfce4-panel -p /plugins/plugin-103/style -n -t uint -s 0 2>/dev/null || true
xfconf-query -c xfce4-panel -p /plugins/plugin-104/icon-size -n -t uint -s 20 2>/dev/null || true
xfconf-query -c xfce4-panel -p /plugins/plugin-105/digital-format -n -t string -s '%a %d %b  %H:%M' 2>/dev/null || true
xfconf-query -c xfce4-panel -p /plugins/plugin-105/digital-layout -n -t uint -s 3 2>/dev/null || true
xfconf-query -c xfce4-panel -p /plugins/plugin-105/show-frame -n -t bool -s false 2>/dev/null || true

# Plugin order: Start | taskbar | spacer | tray | clock | power.
xfconf-query -c xfce4-panel -p /panels/panel-0/plugin-ids -rR 2>/dev/null || true
xfconf-query -c xfce4-panel -p /panels/panel-0/plugin-ids \
  -n -t int -s 101 -t int -s 102 -t int -s 103 -t int -s 104 -t int -s 105 -t int -s 106 2>/dev/null || true

# Keyboard shortcuts: Super opens the Start menu, Super+E opens files,
# Super+W opens Firefox, Super+T opens a terminal.
shortcut() {
  local key="$1" command="$2"
  xfconf-query -c xfce4-keyboard-shortcuts -p "/commands/custom/$key" -n -t string -s "$command" 2>/dev/null || \
  xfconf-query -c xfce4-keyboard-shortcuts -p "/commands/custom/$key" -s "$command" 2>/dev/null || true
}
shortcut '<Super_L>' 'xfce4-popup-whiskermenu'
shortcut '<Super>e' 'thunar'
shortcut '<Super>w' 'firefox'
shortcut '<Super>t' 'xfce4-terminal'
shortcut '<Super>d' 'xfdesktop --toggle-desktop'

# Restart panel and refresh settings.
xfce4-panel >/dev/null 2>&1 &
sleep 2
xfce4-panel -r >/dev/null 2>&1 || true

printf '\nPC-DARKI UI configured.\nBackup: %s\nTheme: %s\nIcons: %s\n' "$BACKUP" "$GTK_THEME" "$ICON_THEME"
