# Rencana Pembersihan UI & Perapihan Fitur Aplikasi

Rencana ini menyederhanakan tampilan layar utama agar lebih bersih, fokus pada interaksi AI & belajar, serta menghapus fitur dan elemen UI yang tumpang tindih sesuai pilihan Anda.

---

## 1. Penghapusan Fitur Rawat Pet & Kustomisasi Berlebih
- **Hapus Fitur Rawat Pet (`PetCareAndShopSheet`):**
  - Menghapus sistem lapar, energi, mandi, tidur, dan toko belanja item perawatan pet.
  - Menghapus tombol **Keranjang Toko (Shop)** di pojok kanan atas HUD serta kapsul **Rawat & Makan**.
  - Menghapus indikator mata uang **Diamond** dan **Koin** dari HUD atas karena tidak lagi diperlukan tanpa sistem belanja pet.
- **Penyederhanaan Karakter & Kostum:**
  - Menghapus sistem belanja aksesoris/kostum berbayar koin dan menghapus opsi kustomisasi karakter yang menumpuk di layar utama.
  - Memfokuskan tombol profil di kiri atas layar khusus untuk **Ganti Karakter AI** (*Bimo, Luna, Mochi, Dodo, Robo, Naga*) lengkap dengan label suara cowok/cewek masing-masing.
  - Apabila masih diperlukan, menu **Kostum & Tema Ruangan** diletakkan secara terpisah di dalam tombol **Menu Aktivitas** (bebas pilih tanpa koin).

---

## 2. Tata Letak Layar Utama Baru (HUD Atas & Dock Bawah Utama)
- **HUD Atas yang Bersih & Minimalis:**
  - **Kiri:** Tombol **Karakter Aktif** (menampilkan nama & indikator gender suara, ketuk untuk ganti karakter).
  - **Kanan:** Tombol **Riwayat Obrolan (Chat History)** dan tombol **Pengaturan (Settings)**.
- **Hapus Baris Kapsul Ganda (`CompanionFeaturesQuickBar`):**
  - Menghapus deretan 8 tombol kapsul geser yang sebelumnya menumpuk tepat di atas dock bawah dan menduplikasi tombol Tugas/Rawat Pet.
- **Dock Bawah Utama dengan Satu Tombol "Menu Aktivitas":**
  - Menyusun ulang dock bawah menjadi **4 tombol utama** yang jelas dan tidak duplikat:
    1. **Video Call:** Panggilan tatap muka langsung (*Real-Time Voice/Video Call*) dengan karakter AI.
    2. **Tanya PR:** Membuka halaman *Scan Foto Soal & Bantuan Tugas Sekolah*.
    3. **Obrolan (Chat):** Membuka bar ketik pesan teks dan tombol mikrofon dikte suara (`VoiceInputDialog`).
    4. **Menu Aktivitas:** Membuka satu panel menu ringkas berisi aktivitas pendamping (*Kuis AI*, *Timer Belajar Pomodoro*, *Flashcard*, *Jurnal Mood*, serta *Kostum / Tema Ruangan* terpisah).

---

## 3. Menu Pengaturan (Settings) Lengkap & Rapi
- Mempertahankan seluruh pengaturan di dalam `SettingsDialog` agar tetap tampil lengkap:
  - Pengaturan efek suara, tampilan *3D Interactive Companion*, dan mode otomatis baca balasan AI.
  - Pilihan mesin suara (**Suara Natural Gemini AI**, Google Cloud Neural2, ElevenLabs) beserta *slider* kecepatan & nada bicara.
  - Kolom pengaturan API Key (Custom Gemini API Key, Google Cloud TTS, ElevenLabs) tetap ditampilkan secara utuh bagi pengguna yang ingin memakainya.

---

## 4. Pembersihan Kode Duplikat & Tidak Terpakai (*Dead Code Cleanup*)
- Menghapus komponen UI lama yang sudah tidak dirender namun masih membebani kode di `AvatarRoomScreen.kt`, `TalkingTomHud.kt`, `TalkingTomCareInteractions.kt`, dan `CareAndMoodSheets.kt` (seperti `LiquidGlassTopRoomBar`, `TalkingTomSideButtons`, `TalkingTomBottomNeedBar`, `PetCareAndShopSheet`, serta daftar `quickChips` statis).
