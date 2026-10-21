package io.github.misakapiano.ccbstudio.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.nio.file.*
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.channels.FileChannel
import java.nio.ByteBuffer

@Serializable private data class Envelope(val version:Int=1,val sequence:Long,val previous:String,val body:String,val sha256:String)

/** Append-only document commits. Unknown entries and partial writes are preserved and block writes. */
class StateJournal(private val directory: Path, private val fault:(String)->Unit={}) {
    private var sequence=0L
    private var previous=""
    var state=StudioState(); private set
    /** Read projection only; the on-disk envelope and commit algorithm remain unchanged. */
    var creationRevision:String?=null; private set
    init {
        safePath(directory); Files.createDirectories(directory)
        val entries=Files.list(directory).use { it.sorted().toList() }
        for(p in entries) {
            safePath(p)
            if(!p.fileName.toString().matches(Regex("[0-9]{12}-[0-9a-f-]{36}\\.json")))
                throw DataProblem("RECOVERY_REQUIRED","Unknown or staged document retained: ${p.fileName}")
            val bytes=readBounded(p,4*1024*1024)
            val e=try { documentJson.decodeFromString<Envelope>(bytes.decodeToString(throwOnInvalidSequence=true)) }
                catch(ex:Exception) { throw DataProblem("CORRUPT","Document envelope retained",ex) }
            if(e.version!=1) throw DataProblem("UNSUPPORTED_VERSION","Document envelope version ${e.version} retained")
            if(e.sequence!=sequence+1 || e.previous!=previous || sha256(e.body.toByteArray())!=e.sha256 || !p.fileName.toString().startsWith("%012d-".format(e.sequence)))
                throw DataProblem("CORRUPT","Document chain/hash mismatch; retained")
            val next=try { documentJson.decodeFromString<StudioState>(e.body).also { it.validate() } }
                catch(ex:DataProblem) { throw ex }
                catch(ex:Exception) { throw DataProblem("CORRUPT","Document fields retained",ex) }
            sequence=e.sequence; previous=sha256(bytes)
            if(next.creation!=state.creation)creationRevision=previous.takeIf{next.creation!=null}
            state=next
        }
    }
    private fun encode(next:StudioState):ByteArray {
        next.validate()
        val body=documentJson.encodeToString(next)
        val bytes=documentJson.encodeToString(Envelope(sequence=sequence+1,previous=previous,body=body,sha256=sha256(body.toByteArray()))).toByteArray()
        if(bytes.size>4*1024*1024)throw DataProblem("DOCUMENT_LIMIT","Document budget reached; prior revisions retained")
        return bytes
    }
    fun requireCapacity(next:StudioState) {encode(next)}
    fun commit(next:StudioState) {
        val bytes=encode(next)
        val id=newId(); val pending=directory.resolve(".pending-$id")
        val target=directory.resolve("%012d-%s.json".format(sequence+1,id))
        writeNewForced(pending,bytes);fault("DOCUMENT_STAGED")
        Files.move(pending,target,StandardCopyOption.ATOMIC_MOVE)
        // Durable file + atomic publication; no claim of arbitrary power-loss directory durability.
        sequence++;previous=sha256(bytes)
        if(next.creation!=state.creation)creationRevision=previous.takeIf{next.creation!=null}
        state=next;fault("DOCUMENT_COMMITTED")
    }
}

fun safePath(path:Path) {
    val absolute=path.toAbsolutePath().normalize()
    var current=absolute.root
    for(part in absolute) {
        current=current.resolve(part)
        if(Files.exists(current,NOFOLLOW_LINKS)) {
            val a=Files.readAttributes(current,java.nio.file.attribute.BasicFileAttributes::class.java,NOFOLLOW_LINKS)
            if(a.isSymbolicLink || a.isOther) throw DataProblem("READ_ERROR","Link/reparse path refused")
        }
    }
}
fun readBounded(path:Path,max:Int):ByteArray {
    safePath(path)
    try {
        Files.newInputStream(path,NOFOLLOW_LINKS).use {
            val bytes=it.readNBytes(max+1)
            if(bytes.size>max) throw DataProblem("CORRUPT","File exceeds budget; retained")
            return bytes
        }
    } catch(e:NoSuchFileException) { throw DataProblem("MISSING","Required file missing",e) }
    catch(e:DataProblem) { throw e }
    catch(e:java.io.IOException) { throw DataProblem("READ_ERROR","File unreadable; retained",e) }
}
fun writeNewForced(path:Path,bytes:ByteArray) {
    safePath(path)
    FileChannel.open(path,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE,NOFOLLOW_LINKS).use { c ->
        val b=ByteBuffer.wrap(bytes);while(b.hasRemaining()) c.write(b);c.force(true)
    }
}
