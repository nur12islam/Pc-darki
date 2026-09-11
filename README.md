# PC-DARKI 🖥️

A lightweight PC-style Linux desktop for Android using **Termux + Termux:X11 + XFCE**.

Designed for the Lenovo A101LV / aarch64 setup used by DARKI, while keeping the installer reasonably portable across Termux arm64 devices.

## What it sets up

- Termux:X11
- XFCE desktop
- D-Bus session support
- Thunar file manager
- Firefox
- XFCE screenshot tool
- Mousepad, Ristretto and File Roller
- ZIP / 7z utilities
- Git, curl, wget and nano
- Android shared-storage shortcut
- Fullscreen X11 preference
- Termux:X11 additional keyboard disabled
- `pc` one-command desktop launcher
- `Ani` wrapper for `ani-cli --dub -q best`

## Install

The Termux:X11 Android companion app must already be installed. For F-Droid Termux, use the normal/universal Termux:X11 APK rather than the shared-UID variant.

In Termux:

```bash
git clone https://github.com/nur12islam/Pc-darki.git
cd Pc-darki
bash install.sh
```

If Android asks for storage permission, allow it.

Then start the desktop with:

```bash
pc
```

## Daily use

```bash
pc
```

Anime:

```bash
Ani <anime title>
```

`Ani` uses dub and requests the best available quality through ani-cli.

## Notes

Android remains the host operating system. PC-DARKI does not replace Android or install a second operating system. XFCE runs as a Linux desktop inside Termux:X11.

The installer is intended to be safe to re-run: it recreates its launchers instead of appending duplicate shell configuration lines.

## Roadmap

- PC-style XFCE panel/taskbar configuration
- Start menu customization
- App launchers and desktop shortcuts
- Better tablet keyboard/mouse shortcuts
- Theme and icon customization
- Optional development/media tools
- Diagnostics and repair commands
