package io.github.misakapiano.ccbstudio.core

import com.sun.jna.platform.win32.Crypt32Util
import com.sun.jna.platform.win32.WinCrypt
import java.nio.file.*
import java.util.UUID

/** Suite-only, user-bound DPAPI. Plaintext never belongs to a document or ordinary backup. */
class SecretVault(private val root:Path) {
    companion object {
        // Stable credential reference, scoped to this workspace's vault. No document format change.
        const val GLOBAL_LLM_REF="a90a39b7-b8bb-4bf7-9272-4c063320fe02"
    }
    init { safePath(root);Files.createDirectories(root) }
    fun hasGlobalKey():Boolean=Files.isRegularFile(root.resolve("$GLOBAL_LLM_REF.dpapi"),LinkOption.NOFOLLOW_LINKS)
    fun putGlobal(plaintext:ByteArray) {
        val revision=put(plaintext) // Retain previous ciphertext and the new revision on failure.
        val staged=root.resolve(".${newId()}.pending")
        writeNewForced(staged,readBounded(root.resolve("$revision.dpapi"),65536))
        Files.move(staged,root.resolve("$GLOBAL_LLM_REF.dpapi"),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING)
    }
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
