# Pembersihan Antarmuka, Video Call Live Kamera Depan & Mode Ngerjain Tugas Sekolah

Rencana pembaruan menyeluruh untuk menyederhanakan tampilan antarmuka (menghapus tombol makan, toilet, tidur, main, dan tombol-tombol samping), mengembalikan fitur **Video Call Live dengan Kamera Depan (PiP mengambang seperti WhatsApp)** lengkap dengan perizinan kamera, serta menghadirkan **Mode Ngerjain Tugas Sekolah** dengan bimbingan tanya-jawab materi dan pemindaian foto soal tugas.

## User Review & Critical Decisions

> [!IMPORTANT]
> **Keputusan yang Telah Dikonfirmasi Pengguna:**
> - **Pembersihan Tombol**: Menghapus tombol kebutuhan Tamagotchi (makan, toilet, tidur, main) dan seluruh tombol samping kiri/kanan. Ruangan menjadi bersih dan fokus pada interaksi karakter 3D.
> - **Video Call Live**: Kamera depan aktif secara live dalam jendela mengambang (*Picture-in-Picture*) di sudut layar seperti panggilan video WhatsApp, dengan karakter 3D interaktif di layar utama.
> - **Mode Ngerjain Tugas Sekolah**: Mengintegrasikan 2 fitur utama:
>   1. *Tanya Jawab & Bimbingan Materi*: Bimbingan interaktif langkah-demi-langkah untuk berbagai mata pelajaran dengan kepribadian karakter.
>   2. *Foto Soal Tugas*: Memotret lembar soal PR langsung dari kamera untuk dianalisis dan dibantu solusinya oleh AI.

---

## 1. Overview & Core Concept

- **Tujuan**: Mengubah antarmuka dari gaya simulasi kebutuhan (*Tamagotchi gauges*) menjadi aplikasi pendamping interaktif yang fokus pada **Interaksi Langsung (Video Call Live)** dan **Pendidikan/Belajar (Mode Ngerjain Tugas Sekolah)**.
- **Nilai Utama**:
  1. *Layar Bersih & Elegan*: Tidak ada tombol mengganggu di kiri, kanan, atau bawah. Karakter 3D tampil maksimal di tengah ruangan.
  2. *Live Video Call*: Pengguna dapat melihat wajah sendiri di jendela kamera depan mini sambil mengobrol tatap muka dua arah secara real-time dengan karakter hewan kesayangan.
  3. *Tutor Pribadi Sekolah*: Karakter hewan bertindak sebagai teman belajar cerdas yang membantu menjelaskan PR, konsep sulit, dan memecahkan soal matematika/sains berdasarkan foto lembaran soal.

---

## 2. User Experience & Visual Design

### 1. Antarmuka Ruangan Utama yang Bersih (Clean Room Screen)
- **Karakter 3D**: Tetap berukuran besar 76% layar di tengah ruangan dengan pencahayaan dan bayangan lantai realistis.
- **Bilah Aksi Bawah yang Ramping**:
  - Tombol **Video Call Live** (Ikon Kamera Video Hijau / Panggilan Tatap Muka).
  - Tombol **Tugas Sekolah** (Ikon Buku Belajar 📚 "Mode Tugas").
  - Tombol **Ketik Pesan / Obrolan** (Ikon Chat 💬).
  - Tombol **Bicara Cepat** (Ikon Mikrofon 🎙️).
- **Semua tombol makan, toilet, tidur, main, dan widget samping dihapus tuntas**.

### 2. Video Call Live dengan Kamera Depan (WhatsApp PiP Style)
- **Izin Kamera**: Meminta izin runtime `android.permission.CAMERA` secara elegan saat tombol Video Call pertama kali ditekan.
- **Jendela Kamera Mengambang (Floating PiP Window)**:
  - Kotak pratinjau kamera depan berbentuk kartu rounded dengan bayangan halus di sudut kanan atas layar (105dp x 140dp).
  - Menampilkan video stream kamera depan pengguna secara real-time dengan tombol flip kamera atau minimize.
- **Karakter 3D di Layar Penuh**:
  - Karakter berbicara, mendengarkan, dan merespons dengan animasi mulut dan ekspresi mata tersinkronisasi.
  - Indikator status panggilan: *Mendengarkan (Hijau)*, *Berpikir (Ungu)*, *Berbicara (Biru)*.
  - Tombol kontrol panggilan: Matikan Mic (Mute), Balik Kamera, dan Akhiri Panggilan (Merah).

### 3. Mode Ngerjain Tugas Sekolah (Homework & Study Companion)
- **Akses Cepat**: Dibuka melalui tombol bertema edukasi di bilah bawah.
- **Fitur 1: Tanya Jawab & Bimbingan Tugas**:
  - Pilihan mata pelajaran: Matematika, IPA (Fisika/Biologi/Kimia), Bahasa Indonesia, Bahasa Inggris, IPS/Sejarah, dan Umum.
  - Karakter menjelaskan konsep langkah-demi-langkah dengan bahasa yang mudah dipahami dan menyemangati, bukan sekadar memberikan kunci jawaban mentah.
- **Fitur 2: Foto Soal Tugas (Photo Homework Solver)**:
  - Tombol "Foto Soal" yang membuka kamera langsung atau memilih dari galeri.
  - Mengambil foto lembaran PR di buku tulis, menganalisis teks pertanyaan soal, dan menjelaskan langkah penyelesaian secara runut.
- **Tampilan Catatan & Rangkuman**: Kotak catatan ramah anak/remaja dengan rumus rapi, poin-poin penting, dan tombol audio untuk mendengarkan penjelasan suara karakter.

---

## 3. Key Product Decisions & Trade-Offs

- **Integrasi CameraX untuk Kamera Depan Live**:
  - *Pendekatan*: Mengaktifkan dependensi CameraX (`androidx.camera.camera2`, `androidx.camera.view`, `androidx.camera.lifecycle`) yang sudah tersedia di Version Catalog untuk merender pratinjau kamera live dengan zero lag.
  - *Alasan*: Standar resmi Android modern yang stabil, mendukung lifecycle Compose, dan hemat daya.
- **Pembersihan Menyeluruh vs Menyembunyikan Tombol**:
  - *Pendekatan*: Menghapus total `TalkingTomBottomNeedBar` dan `TalkingTomSideButtons` dari komposisi UI `AvatarRoomScreen.kt`.
  - *Alasan*: Menjamin kode bersih, tidak ada beban komputasi status kebutuhan yang tidak diinginkan pengguna, dan antarmuka terasa sangat lapang.

---

## 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Updated Clean App Architecture                  │
│                                                                        │
│   ┌────────────────────────────────────────────────────────────────┐   │
│   │ 1. AvatarRoomScreen (Clean Room + 76% 3D Character)            │   │
│   │    ├─ REMOVED: Need Gauges (Makan, Toilet, Tidur, Main)        │   │
│   │    ├─ REMOVED: Left & Right Side Buttons                       │   │
│   │    └─ NEW Streamlined Bottom Action Dock:                      │   │
│   │       [📞 Video Call Live]  [📚 Tugas Sekolah]  [💬 Chat]     │   │
│   └────────────────────────────────────────────────────────────────┘   │
│                                │                                       │
│                ┌───────────────┴───────────────┐                       │
│                ▼                               ▼                       │
│   ┌───────────────────────────┐   ┌───────────────────────────────┐    │
│   │ 2. Live Video Call View   │   │ 3. Mode Ngerjain Tugas        │    │
│   │    ├─ Front Camera (PiP)  │   │    ├─ Step-by-Step AI Tutor   │    │
│   │    ├─ 3D Talking Pet Face │   │    ├─ Camera Foto Soal PR     │    │
│   │    └─ Speech Recognition  │   │    └─ Subject Category Picker │    │
│   └───────────────────────────┘   └───────────────────────────────┘    │
└────────────────────────────────────────────────────────────────────────┘
```

### Rencana File yang Dikerjakan:
1. **`app/build.gradle.kts` & `AndroidManifest.xml`**:
   - Aktifkan dependensi `androidx.camera.*` di `app/build.gradle.kts`.
   - Tambahkan izin `CAMERA` dan fitur hardware kamera di `AndroidManifest.xml`.
2. **`app/src/main/java/com/example/ui/screens/AvatarRoomScreen.kt`**:
   - Hapus pemanggilan `TalkingTomBottomNeedBar` dan `TalkingTomSideButtons`.
   - Pasang bilah kontrol bawah baru yang elegan: Tombol Video Call Live, Tombol Mode Tugas Sekolah, dan Tombol Chat.
3. **`app/src/main/java/com/example/ui/components/FaceToFaceCallView.kt`**:
   - Tambahkan komponen pratinjau kamera depan CameraX dalam jendela mengambang PiP di sudut layar.
   - Tambahkan penanganan izin runtime kamera dengan launcher Compose.
4. **`app/src/main/java/com/example/ui/components/HomeworkStudyView.kt`**:
   - Buat komponen Mode Ngerjain Tugas Sekolah dengan antarmuka bimbingan belajar, pemilihan mata pelajaran, input pertanyaan soal, dan pemotretan soal kamera.
5. **`app/src/main/java/com/example/ui/AvatarViewModel.kt`**:
   - Tambahkan state dan penanganan AI khusus untuk bimbingan tugas sekolah dan analisis foto soal.
6. **Verifikasi**:
   - Jalankan `compile_applet` untuk memastikan kompilasi sukses.
   - Jalankan unit & Robolectric tests untuk memverifikasi seluruh komponen.
