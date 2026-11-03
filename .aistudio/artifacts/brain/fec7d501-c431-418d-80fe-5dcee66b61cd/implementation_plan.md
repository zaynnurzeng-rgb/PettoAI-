# 3D Touchable Animal Avatars, Liquid Glassmorphism UI & Dynamic Skybox Engine

Rencana implementasi menyeluruh untuk memperbarui seluruh antarmuka dan interaksi aplikasi menjadi pengalaman virtual pet 3D yang hidup: 5 karakter hewan berbeda (Kucing, Kelinci, Anjing, Kapibara, Beruang) dengan deteksi sentuh raycast multi-zona (Kepala, Perut, Tangan, Kaki), antarmuka Liquid Glassmorphism modern yang mengambang, serta sistem latar belakang dinamis otomatis waktu nyata dan pemilih tema manual.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> Berdasarkan jawaban klarifikasi yang telah dikonfirmasi:
> 1. **5 Karakter Hewan Unik**:
>    - **Zayn** (Kasar & Tsundere): **Kucing 3D** (telinga runcing, kumis lentur, ekor berayun).
>    - **Siti** (Sayang & Lembut): **Kelinci 3D** (telinga panjang lentur, hidung imut, ekor kapas bulat).
>    - **Rizky** (Tegas & Jelas): **Anjing 3D** (moncong tegas, kalung dinamis, telinga lincah).
>    - **Jamal** (Pemalas & Nakal): **Kapibara 3D** (moncong kotak khas kapibara, mata sipit santai, hiasan yuzu di kepala).
>    - **Heru** (Pemarah & Gak Sopan): **Beruang 3D** (telinga bulat tebal, moncong besar, ekspresi alis sangar).
> 2. **Gaya Visual 3D**: Stylized 3D dengan depth shading, ambient occlusion, spherical highlights, perspektif 3D dengan respons drag/tilt interaktif, serta partikel sentuh multi-efek.
> 3. **Background Dinamis**: Mengikuti waktu nyata (Pagi, Siang, Senja, Malam) dengan siklus atmosferik otomatis, dilengkapi tombol pemilih tema manual (Cozy Room, Sunset Glow, Cyberpunk Neon, Sakura Garden, Deep Space).

---

### 1. Overview & Core Concept

- **Apa yang Dibangun**:
  Aplikasi PawPal AI ditransformasikan menjadi asisten hewan peliharaan 3D interaktif. Karakter tidak lagi berupa gambar datar 2D, melainkan model 3D stylized dengan depth illumination, bayangan ambient, dan rotasi perspektif 3D yang dapat disentuh langsung di berbagai bagian tubuhnya.
- **Interaksi Sentuh Multi-Zona (Raycast Detection)**:
  - **Kepala (Head)**: Mengelus/menyentuh kepala memicu animasi mata memejam nyaman, pipi merona, partikel hati & bintang berkilau, dan suara/audio manja atau senang.
  - **Perut (Belly)**: Menyentuh perut memicu animasi wiggle geli, gelombang tertawa, partikel love bubble, dan reaksi tawa geli khas kepribadian masing-masing.
  - **Tangan / Paws (Hands)**: Menyentuh tangan memicu animasi tos (high-five) ke arah depan dengan partikel bintang kejora dan suara sapaan kompak.
  - **Kaki (Feet)**: Menyentuh kaki memicu animasi melompat kaget atau hentakan lucu dengan partikel tanda seru/musik dan sautan kaget jenaka.
- **Nilai Utama**:
  Pengguna merasa benar-benar memegang dan berinteraksi fisik dengan karakter virtual 3D yang hidup dan memiliki respons haptik/visual/suara yang kaya.

---

### 2. User Experience & Visual Design

#### A. Liquid Glass / Glassmorphism Design System
- **Estetika & Material**:
  - Panel mengambang (floating overlays) dengan frosted glass look: latar semi-transparan `Color(0x33FFFFFF)` untuk tema terang atau `Color(0x28000000)` untuk tema gelap.
  - Border halus kaca kristal dengan gradien refleksi cahaya: `BorderStroke(1.dp, Brush.linearGradient(listOf(Color(0x70FFFFFF), Color(0x18FFFFFF))))`.
  - Radius sudut melengkung elegan (20.dp - 28.dp) yang lembut di mata.
  - Efek bayangan ambient diffuse (`ambientColor`, `spotColor`) untuk kedalaman 3D tanpa terasa berat.
- **Komponen Liquid Glass**:
  - **GlassTopBar**: Menampilkan nama karakter, status afeksi dengan bar gradien kaca, tombol ganti karakter, ganti background, dan pengaturan.
  - **GlassSpeechBubble**: Gelembung percakapan mengambang dengan efek frosted glass, ikon speaker untuk memutar ulang suara, dan animasi gelombang audio.
  - **GlassQuickActions**: Tombol aksi cepat horizontal berbentuk pil kaca dengan highlight saat ditekan.
  - **GlassBottomBar**: Kotak input teks semi-transparan dengan tombol mic suara berdenyut dan tombol kirim aksen neon.
  - **GlassModalSheets & Dialogs**: Dialog pemilihan karakter dan tema ruangan dengan backdrop blur dan kartu kaca berhias ikon 3D.

#### B. Dynamic Background Engine
- **Mode Otomatis (Realtime Sync)**:
  - **Fajar / Pagi (05:00 - 10:59)**: Langit fajar keemasan lembut, gradien oranye pastel dan biru muda dengan partikel berkas sinar matahari (sunbeams).
  - **Siang (11:00 - 16:59)**: Langit biru cerah dengan awan mengambang halus dan partikel kilau cahaya segar.
  - **Sore / Senja (17:00 - 19:29)**: Langit senja spektakuler dengan gradien amber, violet, dan magenta serta partikel kelopak bunga/debu emas melayang perlahan.
  - **Malam (19:30 - 04:59)**: Langit malam berbintang dengan nebula lembut, bulan bercahaya, dan partikel bintang berkedip (twinkling stars).
- **Mode Manual (Tema Kustom)**:
  - Tombol ikon pemilih tema di pojok atas memungkinkan pengguna berganti ke preset: *Auto Realtime*, *Cozy Living Room*, *Sunset Pastel*, *Cyberpunk Neon*, *Sakura Garden*, atau *Deep Space*.

---

### 3. Key Product Decisions & Trade-Offs

1. **Procedural Stylized 3D Mesh vs. Beratnya File GLTF/OBJ**:
   - *Pilihan*: Menggunakan arsitektur procedural 3D stylized rendering berbasis Jetpack Compose Canvas dengan depth-buffer layering, spherical shading illumination, dan transformasi matriks rotasi 3D dinamis.
   - *Alasan*: Menghasilkan waktu muat instan (0 ms tanpa unduhan aset 3D eksternal bergiga-giga), performa 60 FPS mulus di semua perangkat Android, warna dan kostum yang dapat dikustomisasi secara real-time, dan raycast hit-testing zona sentuh yang presisi secara matematis.
2. **Raycast Local Coordinate Hit-Testing**:
   - *Pilihan*: Menghitung proyeksi inversi titik sentuh (touch offset) terhadap pusat model 3D, memperhitungkan rotasi tilt dan skala animasi, lalu memetakan ke bounding sphere/capsule untuk 4 zona sentuh (`HEAD`, `BELLY`, `HAND_LEFT`, `HAND_RIGHT`, `FOOT_LEFT`, `FOOT_RIGHT`).
   - *Alasan*: Interaksi terasa sangat akurat; menyentuh telinga/wajah langsung terbaca sebagai Kepala, menyentuh bagian tengah bawah sebagai Perut, dsb.
3. **Respon Suara & Animasi Khusus untuk 5 Karakter**:
   - Respon sentuh disesuaikan dengan kepribadian unik:
     - **Zayn** (Tsundere): Dipegang kepala tersipu malu ("H-heh! Jangan sembarangan sentuh ya!"), dipegang perut mengeluh tapi gemas ("Aduh geli baka!").
     - **Siti** (Lembut): Dipegang kepala langsung mendengkur hangat ("Hangatnya tanganmu sayang..."), dipegang perut tertawa manis ("Geli sayangku, hihi").
     - **Rizky** (Tegas): Dipegang kepala memuji ("Terima kasih atas apresiasinya!"), dipegang tangan langsung high-five ("Siap, salam komando!").
     - **Jamal** (Kapibara Mager): Dipegang kepala santai banget ("Enak nih... jadi ngantuk..."), dipegang perut tertawa ngakak ("Wkwk geli bro, awas jeruk di kepalaku jatuh").
     - **Heru** (Beruang Pemarah): Dipegang kepala langsung kaget ngegas ("Woi jangan sentuh kepala gua!", lalu luluh "Tapi enak sih dikit"), dipegang kaki loncat ("Kakiku jangan diinjek woy!").

---

### 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                          AvatarRoomScreen                              │
│                                                                        │
│  ┌─────────────────────── Dynamic Background ───────────────────────┐  │
│  │ Realtime Skybox / Gradient Canvas (Pagi/Siang/Senja/Malam)       │  │
│  │ Floating Ambient Particles (Sunbeams / Stars / Sakura)           │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│                                  │                                     │
│  ┌───────────────────────── 3D Avatar Area ─────────────────────────┐  │
│  │  Interactive 3D Canvas (Procedural 3D Mesh + Depth Shading)     │  │
│  │  ├─ Species Renderer: Cat | Rabbit | Dog | Capybara | Bear      │  │
│  │  ├─ Raycast Hit Tester (Head, Belly, Hands, Feet)               │  │
│  │  ├─ 3D Tilt / Drag Perspective Transform                         │  │
│  │  └─ Particle System (Hearts, Stars, Giggle Bubbles, Musical)     │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│                                  │                                     │
│  ┌───────────────────── Liquid Glassmorphic UI ─────────────────────┐  │
│  │  ├─ GlassTopBar (Status, Afeksi, Background Switcher, Menu)      │  │
│  │  ├─ GlassSpeechBubble (Audio Waveform, Replay TTS)               │  │
│  │  ├─ GlassQuickChips (Aksi Cepat & Panggilan)                    │  │
│  │  └─ GlassBottomInputBar (Voice Mic Pill, Text Input, Send)       │  │
│  └──────────────────────────────────────────────────────────────────┘  │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                          AvatarViewModel                               │
│  - onTouchZone(zone: TouchZone) -> triggerAnimation() + speakReaction()│
│  - setBackgroundTheme(theme: DynamicBackgroundTheme)                   │
│  - autoRefreshTimeOfDay()                                              │
│  - sendMessage() / Gemini AI & Fast Persona Integration                │
└────────────────────────────────────────────────────────────────────────┘
```

#### Struktur Komponen Baru & Pembaruan:
1. `com.example.model.PetSpecies`: Diperluas menjadi `CAT`, `DOG`, `RABBIT`, `CAPYBARA`, `BEAR`.
2. `com.example.model.TouchZone`: Enum `HEAD`, `BELLY`, `HANDS`, `FEET` beserta data reaksi animasi & afeksi.
3. `com.example.model.DynamicBackgroundTheme`: Engine waktu nyata (`AUTO_REALTIME`) dan preset tema dinamis dengan warna langit, lantai, dan jenis partikel.
4. `com.example.ui.components.LiquidGlass`: Helper komponen & Modifier glassmorphism (frosted effect, subtle inner shine, specular border).
5. `com.example.ui.components.Avatar3DCanvas`: Engine 3D canvas interaktif dengan raycasting, depth shading, rotasi 3D tilt, dan partikel sentuh.
6. `com.example.ui.screens.AvatarRoomScreen`: Perombakan tata letak UI overlay floating modern berbasis Liquid Glass dan penyambungan interaksi 3D.
7. `com.example.ui.AvatarViewModel`: Penambahan state background dinamis, penanganan interaksi sentuh per zona (Head, Belly, Hands, Feet), dan audio TTS reaksi sentuh.
8. `ExampleRobolectricTest`: Penyesuaian dan pengujian unit test untuk validasi 5 spesies hewan dan sistem zona sentuh.

---

### Verification Plan
1. Jalankan `compile_applet` untuk memastikan kompilasi kode bersih tanpa error.
2. Jalankan `gradle :app:testDebugUnitTest` via `run_command` untuk memverifikasi seluruh unit test lulus dengan sukses.
3. Pastikan kelima karakter (Zayn kucing, Siti kelinci, Rizky anjing, Jamal kapibara, Heru beruang) memiliki visual 3D yang jelas, zona sentuh responsif, antarmuka Liquid Glass yang elegan, serta background dinamis yang bekerja otomatis sesuai waktu lokal.
