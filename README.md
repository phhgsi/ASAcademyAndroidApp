# A.S. Academy - Android Mobile Application

The standalone native Android Studio project for **A.S. Academy Higher Secondary School Portal & Mobile Photo Studio**.

Built with Jetpack Compose, **Kyant0 Capsule & Shapes** UI design, Google ML Kit Face Detection, and Material 3 Expressive aesthetics.

---

## 📱 Features

1. **📸 Rapid Photo Desk / Studio Mode**:
   - Bulk photography workflow matching the web portal.
   - Filter by "Missing Photos Only" or "Captured".
   - Live completion progress bar (`X / Total Captured`).
2. **✨ AI-Framed Photo Updates (Camera & Gallery)**:
   - Safe native camera shutter + phone gallery / file picker.
   - Google ML Kit on-device face detection, 3:4 passport auto-cropping, edge sharpening, and adaptive compression (<75KB).
   - Instant upload & bidirectional sync across active students and permanent register archive.
3. **🪪 Digital Student Identity Card**:
   - Kyant0 continuous squircle portrait framing with academic year badge.
   - Dynamic on-device QR code generation for digital verification.
   - One-tap system share sheet to send ID card details.
4. **💬 Parent Quick Actions**:
   - Direct WhatsApp message trigger addressed to the parent.
   - 1-tap phone dialer.
5. **🏛️ Permanent Register / Archive Photo Management**:
   - Capture, optimize, and update photos for alumni and archived student records.
6. **🌐 Dynamic Server URL Connection**:
   - Connect over local Wi-Fi LAN (`http://192.168.x.x:5233`) or Cloudflare Live Tunnel (`https://your-school.trycloudflare.com`).
   - Integrated QR code server scanner.
7. **💳 Fee Desk & Attendance**:
   - Instant mobile fee collection and digital receipt generation.
   - Batch class attendance marking.

---

## 🚀 How to Build APK with Android Studio

1. Open **Android Studio**.
2. Click **File -> Open** and select this `ASAcademyAndroidApp` directory.
3. Allow Gradle sync to complete.
4. Click **Build -> Build Bundle(s) / APK(s) -> Build APK(s)**.
5. The generated `app-debug.apk` will be in `app/build/outputs/apk/debug/`. Transfer to any Android phone (Android 5.0 to 14+) and install!

