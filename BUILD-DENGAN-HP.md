# Build APK HTML2MP4 Studio tanpa komputer

Proyek ini disiapkan untuk dibuild di GitHub Actions (komputer cloud), sehingga tidak perlu memasang Android Studio di HP.

## Persyaratan
- Akun GitHub gratis.
- Browser di HP.

## Langkah
1. Ekstrak ZIP proyek ini di HP.
2. Buka https://github.com/new lalu buat repository baru bernama `HTML2MP4Studio` (boleh Public atau Private).
3. Dari halaman repository, pilih **Add file → Upload files**. Unggah seluruh isi folder `HTML2MP4Studio` yang sudah diekstrak, termasuk folder tersembunyi `.github` beserta `workflows/android-apk.yml`. Jika pemilih file Android tidak bisa mengunggah folder, gunakan browser dengan fitur **Situs desktop** atau aplikasi pengelola berkas yang mendukung pemilihan folder.
4. Tekan **Commit changes**.
5. Buka tab **Actions** pada repository. Pilih workflow **Build Android APK**, lalu tekan **Run workflow** jika belum berjalan otomatis.
6. Tunggu hingga workflow selesai dengan status hijau.
7. Buka hasil run tersebut, cari bagian **Artifacts**, lalu unduh `HTML2MP4Studio-debug-apk`.
8. Ekstrak unduhan artifact dan pasang `app-debug.apk` di HP. Android mungkin meminta izin memasang aplikasi dari sumber tersebut.

## Catatan
- APK debug cocok untuk instalasi/uji coba pribadi, bukan rilis Play Store.
- Workflow menggunakan Java 17, Gradle 8.9, Android SDK Platform 35 dan Build Tools 35.0.0.
- Build memerlukan koneksi internet dan akun GitHub. Jangan unggah kunci rahasia atau kredensial ke repository.
