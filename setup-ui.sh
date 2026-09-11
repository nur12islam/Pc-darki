#!/data/data/com.termux/files/usr/bin/bash
set -u
HOME_DIR="${HOME:-/data/data/com.termux/files/home}"
PREFIX="${PREFIX:-/data/data/com.termux/files/usr}"
PANEL="$HOME_DIR/.config/xfce4/panel"
BACKUP="$HOME_DIR/.pc-darki-backups/$(date +%Y%m%d-%H%M%S)"
mkdir -p "$BACKUP"
cp -a "$HOME_DIR/.config/xfce4" "$BACKUP/xfce4" 2>/dev/null || true
xfce4-panel -q >/dev/null 2>&1 || true

# Theme
for theme in Greybird-dark Adwaita-dark; do
  if [ -d "$PREFIX/share/themes/$theme" ]; then xfconf-query -c xsettings -p /Net/ThemeName -n -t string -s "$theme" 2>/dev/null || xfconf-query -c xsettings -p /Net/ThemeName -s "$theme" 2>/dev/null; break; fi
done
for icon in Papirus-Dark Adwaita hicolor; do
  if [ -d "$PREFIX/share/icons/$icon" ]; then xfconf-query -c xsettings -p /Net/IconThemeName -n -t string -s "$icon" 2>/dev/null || xfconf-query -c xsettings -p /Net/IconThemeName -s "$icon" 2>/dev/null; break; fi
done
xfconf-query -c xfwm4 -p /general/use_compositing -n -t bool -s true 2>/dev/null || true
xfconf-query -c xfce4-desktop -p /desktop-icons/icon-size -n -t int -s 48 2>/dev/null || true

# Keep one existing panel, remove duplicate panels.
IDS=$(xfconf-query -c xfce4-panel -p /panels 2>/dev/null | awk '/^[0-9]+$/{print $1}')
KEEP=$(printf '%s\n' "$IDS" | head -n1)
[ -n "$KEEP" ] || KEEP=0
for id in $IDS; do
  [ "$id" = "$KEEP" ] || xfconf-query -c xfce4-panel -p "/panels/panel-$id" -rR 2>/dev/null || true
done
xfconf-query -c xfce4-panel -p /panels -rR 2>/dev/null || true
xfconf-query -c xfce4-panel -p /panels -n --force-array -t int -s "$KEEP" 2>/dev/null || true

P="/panels/panel-$KEEP"
xfconf-query -c xfce4-panel -p "$P/length" -n -t uint -s 100 2>/dev/null || true
xfconf-query -c xfce4-panel -p "$P/length-adjust" -n -t bool -s false 2>/dev/null || true
xfconf-query -c xfce4-panel -p "$P/mode" -n -t uint -s 0 2>/dev/null || true
xfconf-query -c xfce4-panel -p "$P/nrows" -n -t uint -s 1 2>/dev/null || true
xfconf-query -c xfce4-panel -p "$P/position" -n -t string -s 'p=12;x=0;y=0' 2>/dev/null || true
xfconf-query -c xfce4-panel -p "$P/position-locked" -n -t bool -s true 2>/dev/null || true
xfconf-query -c xfce4-panel -p "$P/size" -n -t uint -s 48 2>/dev/null || true
xfconf-query -c xfce4-panel -p "$P/autohide-behavior" -n -t uint -s 0 2>/dev/null || true
xfconf-query -c xfce4-panel -p "$P/background-style" -n -t uint -s 0 2>/dev/null || true
xfconf-query -c xfce4-panel -p "$P/icon-size" -n -t uint -s 32 2>/dev/null || true

# Choose Whisker when installed; otherwise use native Applications Menu.
if [ -f "$PREFIX/share/xfce4/panel/plugins/whiskermenu.desktop" ] || [ -f "$PREFIX/share/xfce4/panel-plugins/whiskermenu.desktop" ]; then START=whiskermenu; else START=applicationsmenu; fi

# Rebuild a clean modern panel: Start | pinned apps | task area | tray | clock.
for id in 101 102 103 104 105 106 107 108 109; do xfconf-query -c xfce4-panel -p "/plugins/plugin-$id" -rR 2>/dev/null || true; done
mk(){ xfconf-query -c xfce4-panel -p "/plugins/plugin-$1" -n -t string -s "$2" 2>/dev/null || true; }
mk 101 "$START"
mk 102 launcher
mk 103 launcher
mk 104 launcher
mk 105 separator
mk 106 tasklist
mk 107 separator
mk 108 systray
mk 109 clock

make_launcher(){
  id="$1"; file="$2"; name="$3"; exec_cmd="$4"; icon="$5"
  dir="$PANEL/launcher-$id"; mkdir -p "$dir"
  cat > "$dir/$file" <<DESKTOP
[Desktop Entry]
Type=Application
Name=$name
Exec=$exec_cmd
Icon=$icon
Terminal=false
Categories=Utility;
DESKTOP
  xfconf-query -c xfce4-panel -p "/plugins/plugin-$id/items" -n --force-array -t string -s "$file" 2>/dev/null || true
}
make_launcher 102 firefox.desktop Firefox firefox firefox
make_launcher 103 files.desktop Files thunar system-file-manager
make_launcher 104 terminal.desktop Terminal xfce4-terminal utilities-terminal

xfconf-query -c xfce4-panel -p /plugins/plugin-101/button-title -n -t string -s 'Start' 2>/dev/null || true
xfconf-query -c xfce4-panel -p /plugins/plugin-101/show-button-title -n -t bool -s true 2>/dev/null || true
xfconf-query -c xfce4-panel -p /plugins/plugin-101/show-menu-icons -n -t bool -s true 2>/dev/null || true
xfconf-query -c xfce4-panel -p /plugins/plugin-105/expand -n -t bool -s true 2>/dev/null || true
xfconf-query -c xfce4-panel -p /plugins/plugin-107/expand -n -t bool -s true 2>/dev/null || true
xfconf-query -c xfce4-panel -p /plugins/plugin-105/style -n -t uint -s 0 2>/dev/null || true
xfconf-query -c xfce4-panel -p /plugins/plugin-107/style -n -t uint -s 0 2>/dev/null || true
xfconf-query -c xfce4-panel -p /plugins/plugin-106/grouping -n -t uint -s 1 2>/dev/null || true
xfconf-query -c xfce4-panel -p /plugins/plugin-109/digital-format -n -t string -s '%a %d %b  %H:%M' 2>/dev/null || true

xfconf-query -c xfce4-panel -p "$P/plugin-ids" -rR 2>/dev/null || true
xfconf-query -c xfce4-panel -p "$P/plugin-ids" --create --force-array \
  -t int -s 101 -t int -s 102 -t int -s 103 -t int -s 104 -t int -s 105 -t int -s 106 -t int -s 107 -t int -s 108 -t int -s 109 2>/dev/null || true

# Useful keyboard shortcuts.
for keycmd in \
  '<Super>e|thunar' \
  '<Super>w|firefox' \
  '<Super>t|xfce4-terminal' \
  '<Super>d|xfdesktop --toggle-desktop'; do
  key="${keycmd%%|*}"; cmd="${keycmd#*|}"
  xfconf-query -c xfce4-keyboard-shortcuts -p "/commands/custom/$key" -n -t string -s "$cmd" 2>/dev/null || true
done

xfce4-panel >/dev/null 2>&1 &
sleep 2
xfce4-panel -r >/dev/null 2>&1 || true
printf 'PC-DARKI UI ready. Panel=%s Start=%s Backup=%s\n' "$KEEP" "$START" "$BACKUP"
