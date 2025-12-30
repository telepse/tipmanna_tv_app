# Android TV Installation Instructions

## APK Location
After building, your APK will be located at:
- **Release APK**: `app/build/outputs/apk/release/app-release.apk`
- **Debug APK**: `app/build/outputs/apk/debug/app-debug.apk`

---

## Method 1: Using ADB (Recommended - Most Reliable)

### Prerequisites:
- Android TV and computer on the same Wi-Fi network
- ADB installed on your computer (comes with Android SDK)

### Steps:

1. **Enable Developer Options on Android TV:**
   - Go to **Settings** → **Device Preferences** → **About**
   - Scroll to **Build** and click it **7 times** until you see "You are now a developer"
   - Go back to **Settings** → **Device Preferences** → **Developer Options**
   - Enable **USB Debugging** and **Network Debugging** (ADB over network)

2. **Find TV's IP Address:**
   - On TV: **Settings** → **Device Preferences** → **About** → **Status** → **IP Address**
   - Note down the IP address (e.g., 192.168.1.100)

3. **Connect via ADB:**
   ```bash
   adb connect <TV_IP_ADDRESS>
   # Example: adb connect 192.168.1.100
   ```
   - You should see: "connected to 192.168.1.100:5555"

4. **Install APK:**
   ```bash
   adb install app-release.apk
   # Or if using debug: adb install app-debug.apk
   ```

5. **Launch the App:**
   ```bash
   adb shell monkey -p com.example.telepse -c android.intent.category.LAUNCHER 1
   ```

---

## Method 2: Using USB Drive

### Steps:

1. **Copy APK to USB Drive:**
   - Copy the APK file to a USB drive
   - Format: FAT32 or exFAT

2. **On Android TV:**
   - Insert USB drive into TV's USB port
   - Go to **Settings** → **Device Preferences** → **Security & Restrictions**
   - Enable **Unknown Sources** (allow installation from unknown sources)

3. **Install:**
   - Open **File Manager** app on TV (or download one from Play Store)
   - Navigate to USB drive
   - Select the APK file
   - Click **Install**
   - Click **Open** when done

---

## Method 3: Using "Send Files to TV" App

### Steps:

1. **On Your Computer:**
   - Download "Send Files to TV" from: https://sendfilestotv.app/
   - Install and open it

2. **On Android TV:**
   - Install "Send Files to TV" from Google Play Store
   - Open the app

3. **Transfer APK:**
   - Both devices should be on the same Wi-Fi network
   - On computer: Select "Send" → Choose APK file
   - On TV: Select "Receive"
   - The APK will be transferred

4. **Install:**
   - On TV, open the received file
   - Click **Install**
   - Enable **Unknown Sources** if prompted

---

## Method 4: Using File Manager App (ES File Explorer or Similar)

### Steps:

1. **On Android TV:**
   - Install **ES File Explorer** or similar file manager from Play Store
   - Enable **Unknown Sources** in Settings

2. **Transfer APK:**
   - Use ES File Explorer's network features to access files from your computer
   - Or use cloud storage (Google Drive, Dropbox) to upload APK and download on TV

3. **Install:**
   - Navigate to APK location in file manager
   - Click on APK file
   - Select **Install**

---

## Troubleshooting

### If "Unknown Sources" is not available:
- Some Android TV models require enabling "Developer Options" first
- Go to Settings → Device Preferences → Developer Options
- Enable "Install unknown apps" or "Unknown Sources"

### If ADB connection fails:
- Ensure TV and computer are on the same network
- Check firewall settings on your computer
- Try: `adb kill-server` then `adb connect <TV_IP>` again
- Verify Network Debugging is enabled on TV

### If installation fails:
- Check TV's available storage space
- Ensure APK is not corrupted (try rebuilding)
- Check Android version compatibility (minSdk: 21)
- Try uninstalling any previous version first

### If app doesn't appear on TV home screen:
- Go to **Settings** → **Apps** → Find your app → **Launch**
- Or use ADB: `adb shell monkey -p com.example.telepse -c android.intent.category.LAUNCHER 1`

---

## Quick Reference Commands

```bash
# Connect to TV
adb connect <TV_IP_ADDRESS>

# Check connected devices
adb devices

# Install APK
adb install app-release.apk

# Uninstall app
adb uninstall com.example.telepse

# Launch app
adb shell monkey -p com.example.telepse -c android.intent.category.LAUNCHER 1

# View logs (for debugging)
adb logcat | grep -i telepse
```

---

## Notes

- **Package Name**: `com.example.telepse`
- **Minimum Android Version**: Android 5.0 (API 21)
- **Target Android Version**: Android 14+ (API 36)

For best results, use **Method 1 (ADB)** as it's the most reliable and allows for easy updates and debugging.








