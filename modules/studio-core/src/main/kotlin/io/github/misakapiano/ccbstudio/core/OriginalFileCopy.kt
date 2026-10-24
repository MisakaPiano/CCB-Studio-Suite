package io.github.misakapiano.ccbstudio.core

import java.io.BufferedOutputStream
import java.io.OutputStream
import java.nio.channels.Channels
import java.nio.channels.FileChannel
import java.nio.file.*
import java.security.MessageDigest
import java.util.UUID

/** Runtime result only; no new stored format. Prefix is at most the image signature's 12 bytes. */
data class OriginalFileCopy(val size:Long,val sha256:String?,val prefix:ByteArray)

/** Copy to a new private/staged file, retaining the existing store's checksum verification.
 * Caller owns publication. Failure leaves an unpublished partial; it never replaces a target. */
fun copyVerifiedOriginal(workspace:StudioWorkspace,id:String,target:Path,receiptDigest:Boolean=true,checkCancelled:()->Unit={}):OriginalFileCopy {
    safePath(target);checkCancelled()
    // Store source/checksum verification is unconditional. This extra digest is only for a
    // caller's receipt; callers with their own full destination fingerprint can omit it.
    val digest=if(receiptDigest)MessageDigest.getInstance("SHA-256")else null
    val prefix=ByteArray(12);var prefixSize=0;var size=0L
    FileChannel.open(target,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE,LinkOption.NOFOLLOW_LINKS).use{channel->
        val output=BufferedOutputStream(Channels.newOutputStream(channel),256*1024)
        val checked=object:OutputStream() {
            override fun write(bytes:ByteArray,offset:Int,length:Int) {
                checkCancelled();output.write(bytes,offset,length);digest?.update(bytes,offset,length);size+=length
                val count=minOf(length,prefix.size-prefixSize)
                if(count>0){System.arraycopy(bytes,offset,prefix,prefixSize,count);prefixSize+=count}
            }
            override fun write(value:Int) {
                checkCancelled();output.write(value);digest?.update(value.toByte());size++
                if(prefixSize<prefix.size)prefix[prefixSize++]=value.toByte()
            }
        }
        // Check workspace admission briefly. AssetStore.export owns its serialized operation
        // and drain registration; a slow destination must not monopolize workspace state reads.
        val store=workspace.operation{workspace.assets}
        store.export(UUID.fromString(id),checked)
        // Never flush a failed/unverified transfer; keep its private partial for inspection.
        checkCancelled();output.flush();channel.force(true)
    }
    return OriginalFileCopy(size,digest?.digest()?.let{java.util.HexFormat.of().formatHex(it)},prefix.copyOf(prefixSize))
}
