package io.github.misakapiano.ccbstudio.core

import com.sun.jna.platform.win32.Crypt32Util
import com.sun.jna.platform.win32.WinCrypt
import java.nio.file.*
import java.util.UUID

/** Suite-only, user-bound DPAPI. Plaintext never belongs to a document or ordinary backup. */
class SecretVault(private val root:Path) {
    init { safePath(root);Files.createDirectories(root) }
    fun put(plaintext:ByteArray):String {
        require(plaintext.size in 1..16384)
        val id=newId()
        val encrypted=Crypt32Util.cryptProtectData(plaintext,entropy(),WinCrypt.CRYPTPROTECT_UI_FORBIDDEN,"CCB Studio Suite",null)
        writeNewForced(root.resolve("$id.dpapi"),encrypted)
        return id
    }
    fun <T> use(ref:String,action:(ByteArray)->T):T {
        require(UUID.fromString(ref).toString()==ref)
        val encrypted=readBounded(root.resolve("$ref.dpapi"),65536)
        val plain=try { Crypt32Util.cryptUnprotectData(encrypted,entropy(),WinCrypt.CRYPTPROTECT_UI_FORBIDDEN,null) }
            catch(e:Exception) {throw DataProblem("READ_ERROR","OS-bound credential unavailable; ciphertext retained")}
        return try {action(plain)} finally {plain.fill(0)}
    }
    private fun entropy()="CCBStudioSuite/preview/v1".toByteArray()
}
