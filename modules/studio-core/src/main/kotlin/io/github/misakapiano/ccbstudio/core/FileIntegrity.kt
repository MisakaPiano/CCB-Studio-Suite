package io.github.misakapiano.ccbstudio.core

import java.nio.file.*
import java.security.MessageDigest

/** Streaming verification for stored files, independent of image decode limits.
 * The asset ceiling matches the existing AssetStore slice; documents remain 4 MiB. */
internal const val STORED_FILE_LIMIT=256L*1024*1024
internal data class FileFingerprint(val size:Long,val sha256:String)
internal fun fingerprintFile(path:Path,max:Long=STORED_FILE_LIMIT):FileFingerprint {
    require(max>=0);safePath(path)
    try {
        val attributes=Files.readAttributes(path,java.nio.file.attribute.BasicFileAttributes::class.java,LinkOption.NOFOLLOW_LINKS)
        if(!attributes.isRegularFile)throw DataProblem("READ_ERROR","Expected a regular stored file; retained")
        val digest=MessageDigest.getInstance("SHA-256");var size=0L
        Files.newInputStream(path,LinkOption.NOFOLLOW_LINKS).use{input->
            val buffer=ByteArray(65536)
            while(true){val n=input.read(buffer);if(n<0)break;size+=n
                if(size>max)throw DataProblem("CORRUPT","Stored file exceeds its budget; retained")
                digest.update(buffer,0,n)
            }
        }
        return FileFingerprint(size,java.util.HexFormat.of().formatHex(digest.digest()))
    }catch(e:NoSuchFileException){throw DataProblem("MISSING","Required stored file missing",e)}
    catch(e:DataProblem){throw e}
    catch(e:java.io.IOException){throw DataProblem("READ_ERROR","Stored file unreadable; retained",e)}
}
