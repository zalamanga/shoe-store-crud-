# Checklist uji manual

Gunakan database pengembangan yang sudah disiapkan melalui [README](../README.md). Checklist ini adalah langkah pemeriksaan, bukan pernyataan bahwa semua fitur sudah lulus uji.

Catat hasil setelah menjalankan aplikasi:

| Informasi | Hasil |
| --- | --- |
| Tanggal | |
| Sistem operasi dan versi Java | |
| Versi SQL Server | |
| Commit yang diuji | |

## Login dan navigasi

- [ ] Username/password kosong menampilkan pesan yang sesuai.
- [ ] Password salah menolak login dan tetap membuka form login.
- [ ] Akun yang tersimpan di `dbo.login` dapat masuk melalui tombol Login.
- [ ] Menekan Enter pada form login menjalankan login satu kali.
- [ ] Menu utama dapat membuka Product, Customer, Employee, dan Supplier.
- [ ] Tombol Back kembali ke menu tanpa menumpuk jendela lama.
- [ ] Ketika koneksi database gagal, aplikasi menampilkan pesan dan dapat dicoba lagi.

## Data percobaan

Pakai data buatan sendiri. Contoh berikut dapat diganti jika ID sudah dipakai.

| Modul | Data awal |
| --- | --- |
| Product | ID `TEST-SHOE-01`, merek `Demo`, nama `Sepatu Uji`, warna `Hitam`, ukuran `42`, harga `250000`, stok `5` |
| Customer | ID `TEST-CUST-01`, nama `Pelanggan Uji`, alamat `Alamat Contoh`, telepon `081234567890` |
| Employee | ID `TEST-EMP-01`, nama `Pegawai Uji`, alamat `Alamat Contoh`, telepon `081234567891` |
| Supplier | ID `TEST-SUP-01`, nama `Pemasok Uji`, alamat `Alamat Contoh`, merek `Demo`, telepon `081234567892` |

## CRUD pada setiap modul

Ulangi urutan ini untuk keempat modul. Catat hasil tiap modul secara terpisah.

1. Klik Read dan cocokkan isi tabel dengan database.
2. Isi data percobaan, lalu klik Insert satu kali. Pastikan hanya satu baris baru tersimpan.
3. Cari data tersebut menggunakan sebagian ID atau nama. Pastikan hasil yang sesuai muncul.
4. Pilih baris pada tabel. Pastikan semua kolom terisi pada form.
5. Ubah nama dan satu kolom lain, lalu klik Update. Pastikan baris yang dipilih berubah dan baris lain tetap sama.
6. Pilih data percobaan, lalu klik Delete. Pastikan baris yang dipilih hilang.
7. Klik Read lagi dan cocokkan hasil akhir dengan database.
8. Tutup dan buka ulang aplikasi untuk memastikan perubahan tetap tersimpan.

| Modul | Read | Insert | Search | Update | Delete | Catatan |
| --- | --- | --- | --- | --- | --- | --- |
| Product | | | | | | |
| Customer | | | | | | |
| Employee | | | | | | |
| Supplier | | | | | | |

## Kasus yang perlu diperiksa

- [ ] Field wajib kosong menolak penyimpanan.
- [ ] ID yang sudah ada tidak membuat data duplikat.
- [ ] Nama dengan apostrof, misalnya `O'Neil`, dapat disimpan dan dicari sebagai data biasa.
- [ ] Update dan Delete menggunakan baris yang dipilih, termasuk ketika dua baris memiliki nomor telepon atau stok yang sama.
- [ ] Mengubah ID melalui Update tetap memperbarui baris yang semula dipilih.
- [ ] Membatalkan konfirmasi Delete mempertahankan data yang dipilih.
- [ ] Harga, ukuran, dan stok yang bukan angka ditolak.
- [ ] Harga/stok negatif dan ukuran yang tidak valid ditolak.
- [ ] Nomor telepon yang diawali `0` tetap menyimpan angka nolnya.
- [ ] Tombol Insert melalui klik mouse atau keyboard tidak menyimpan data dua kali.
- [ ] Aplikasi menangani tabel kosong dan pencarian tanpa hasil.
- [ ] Insert atau Update yang gagal karena koneksi tidak menampilkan pesan sukses dan mempertahankan input pada form.

## Catatan hasil

Tuliskan langkah yang gagal, data masukan, hasil yang diharapkan, dan pesan yang muncul. Screenshot dapat ditambahkan setelah pengujian dilakukan; hindari menampilkan password atau data pribadi.
