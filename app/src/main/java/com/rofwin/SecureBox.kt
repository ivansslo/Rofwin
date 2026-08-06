package com.rofwin

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * ===== v1.8.2 — SecureBox =====
 * Enkripsi AES-256/GCM dengan kunci privat yang tidak pernah keluar dari
 * Android Keystore (hardware-backed bila device mendukung).
 *
 * Dipakai untuk menyimpan API key AI di SharedPreferences secara aman (v1.8.1
 * ke bawah menyimpan plaintext — nilai lama tanpa prefix "v1:" tetap terbaca
 * apa adanya agar migrasi mulus, lalu otomatis terenkripsi saat user mengedit).
 *
 * Format tersimpan: "v1:" + Base64( IV(12B) || ciphertext+GCM-tag ).
 */
object SecureBox {
    private const val KEY_ALIAS = "rofwin_secure_box_v1"
    private const val PREFIX = "v1:"
    private const val KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORM = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128
    private const val IV_LEN = 12

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (ks.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        kg.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .setKeySize(256)
                .build()
        )
        return kg.generateKey()
    }

    /** Enkripsi untuk disimpan ke prefs. Gagal (Keystore tak tersedia) -> kembalikan apa adanya (perilaku lama). */
    fun encryptTo(plain: String): String = try {
        if (plain.isEmpty()) "" else {
            val c = Cipher.getInstance(TRANSFORM)
            c.init(Cipher.ENCRYPT_MODE, key())
            val enc = c.doFinal(plain.toByteArray(Charsets.UTF_8))
            PREFIX + Base64.encodeToString(c.iv + enc, Base64.NO_WRAP)
        }
    } catch (_: Exception) {
        plain
    }

    /** Dekripsi dari prefs. Nilai legacy tanpa prefix dibaca apa adanya; gagal dekripsi -> kosong (user isi ulang). */
    fun decryptFrom(stored: String): String = try {
        if (!stored.startsWith(PREFIX)) stored else {
            val raw = Base64.decode(stored.removePrefix(PREFIX), Base64.NO_WRAP)
            val c = Cipher.getInstance(TRANSFORM)
            c.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(GCM_TAG_BITS, raw, 0, IV_LEN))
            String(c.doFinal(raw, IV_LEN, raw.size - IV_LEN), Charsets.UTF_8)
        }
    } catch (_: Exception) {
        ""
    }
}
