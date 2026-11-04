# Rencana Implementasi: Konfigurasi Prompt Gemini 3.5 & Bahasa Indonesia Kasual

Berdasarkan preferensi yang Anda pilih:
- **Gaya Bahasa:** Campuran bahasa gaul dengan istilah kekinian dan humor santai.
- **Manajemen Konteks:** Menyimpan 10 riwayat percakapan terakhir untuk memori percakapan mendalam.
- **Model Target:** Menggunakan **Gemini 3.5** (`gemini-3.5-flash` dan `gemini-3.5-pro`) dengan fallback andal.

---

## 1. Pembaruan Endpoint Model ke Gemini 3.5
- **Prioritas Model Utama:**
  1. `gemini-3.5-flash` (prioritas utama untuk respons obrolan kilat dan cerdas)
  2. `gemini-3.5-pro` (prioritas lanjutan untuk penalaran tugas sekolah)
  3. `gemini-2.0-flash` & `gemini-1.5-flash` (fallback otomatis jika kunci API atau tier jaringan belum mengaktifkan 3.5)
- Diterapkan pada fungsi percakapan karakter (`generateReply`) dan fitur pemecah tugas sekolah (`generateHomeworkHelp`).

---

## 2. Manajemen Status Percakapan Multi-Turn (State Management)
- **Kapasitas Riwayat:** Mengambil 10 pesan terakhir (`history.takeLast(10)`) dari riwayat percakapan pengguna dan avatar.
- **Sanitasi Alternasi Role Gemini:** Memastikan urutan `contents` mematuhi aturan API Gemini:
  - Format terstruktur `{ "role": "user", "parts": [...] }` dan `{ "role": "model", "parts": [...] }`.
  - Menggabungkan atau merapikan pesan jika terdapat pesan berturut-turut dari role yang sama agar API tidak mengembalikan error `INVALID_ARGUMENT`.
  - Memastikan pet mengingat nama pengguna, topik pembicaraan sebelumnya, dan konteks pertanyaan sebelumnya.

---

## 3. Konfigurasi Prompt Kepribadian "Bahasa Indonesia Gaul & Santai"
- **Pembaruan `buildSystemInstruction`:**
  - Mengintegrasikan bahasa santai Indonesia yang natural dengan istilah kekinian (*santuy, beneran, gaspol, sabi, wkwk, auto, gemoy, relate, spill*).
  - Menghindari gaya kaku seperti robot/ensiklopedia; pet berbicara seperti sahabat nongkrong yang ceria, humoris, dan ekspresif.
  - Mempertahankan akurasi fakta: Ketika ditanya sains, matematika, atau PR, pet memberikan fakta yang benar terlebih dahulu dengan cara penyampaian yang santai dan mudah dimengerti.
  - Menjaga ciri khas masing-masing karakter:
    - **Zayn:** Tsundere gengsian, sok pinter tapi gemesin dan suka ngasih istilah kekinian.
    - **Siti:** Manis, ceria, hangat, penuh perhatian dan supportif.
    - **Rizky:** Anjing energik, sigap, penuh semangat "gaspol!".
    - **Jamal:** Kapibara super santuy, humoris, mageran tapi solutif.
    - **Heru:** Beruang ngegas kocak, frontal tapi aslinya setia kawan.

---

## 4. Penyelarasan Mode Ngerjain Tugas Sekolah
- Memperbarui `callGeminiJson` dalam pemecahan tugas sekolah agar memanggil `gemini-3.5-flash` terlebih dahulu.
- Tips tutor (`tutorTip`) dirancang dengan gaya santai dan menyemangati siswa dengan gaya bahasa gaul yang bersahabat.
- Memastikan pemecah deterministik lokal tetap aktif sebagai jaring pengaman berkecepatan 0ms.

---

## 5. Verifikasi & Pengujian
- Menjalankan Robolectric unit tests (`gradle :app:testDebugUnitTest`) untuk memastikan:
  - Payload JSON Gemini valid dan struktur multi-turn 10 riwayat percakapan terformat dengan benar.
  - Tanggapan karakter dan tutor tugas sekolah berfungsi secara akurat.
- Melakukan verifikasi kompilasi build melalui `compile_applet`.
