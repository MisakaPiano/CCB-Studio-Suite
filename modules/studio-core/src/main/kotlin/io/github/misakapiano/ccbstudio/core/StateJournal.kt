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
    var committedBody:String?=null; private set
    private val versionBodies=mutableMapOf<Int,String>()
    fun committedBodyForVersion(version:Int):String {
        val body=versionBodies[version]?:throw DataProblem("UPDATE_SNAPSHOT_MISMATCH","Missing source-format commit")
        if(documentJson.decodeFromString<StudioState>(body).copy(version=state.version)!=state)
            throw DataProblem("UPDATE_SNAPSHOT_MISMATCH","Source-format state differs beyond migration version")
        return body
    }
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
            state=next;committedBody=e.body;versionBodies[next.version]=e.body
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
        state=next;committedBody=documentJson.decodeFromString<Envelope>(bytes.decodeToString()).body
        versionBodies[next.version]=requireNotNull(committedBody);fault("DOCUMENT_COMMITTED")
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
        FileChannel.open(path,StandardOpenOption.READ,NOFOLLOW_LINKS).use {channel->
            // Size of this open handle is an allocation hint, never an integrity/budget decision.
            return readBoundedInput(java.nio.channels.Channels.newInputStream(channel),max,channel.size())
        }
    } catch(e:NoSuchFileException) { throw DataProblem("MISSING","Required file missing",e) }
    catch(e:DataProblem) { throw e }
    catch(e:java.io.IOException) { throw DataProblem("READ_ERROR","File unreadable; retained",e) }
}
/** Same max+1 sentinel/EOF contract as readNBytes, without a second full array for stable files.
 * The caller owns the input. A stale size hint may shrink/grow; actual reads enforce the budget. */
internal fun readBoundedInput(input:java.io.InputStream,max:Int,sizeHint:Long):ByteArray {
    val sentinelLimit=max+1
    require(sentinelLimit>=0) // Preserve readNBytes' rejection of negative/overflowed lengths.
    val capacity=sizeHint.coerceIn(0L,sentinelLimit.toLong()).toInt()
    val initial=ByteArray(capacity);val count=input.readNBytes(initial,0,capacity)
    if(count>max)throw DataProblem("CORRUPT","File exceeds budget; retained")
    if(count<capacity)return initial.copyOf(count)
    val extra=input.read()
    if(extra<0)return initial
    if(count>=max)throw DataProblem("CORRUPT","File exceeds budget; retained")
    // Source grew after the hint. Consume at most the remaining budget plus its sentinel.
    val tail=input.readNBytes(max-count)
    val total=count+1+tail.size
    if(total>max)throw DataProblem("CORRUPT","File exceeds budget; retained")
    return ByteArray(total).also {result->
        initial.copyInto(result,0,0,count);result[count]=extra.toByte();tail.copyInto(result,count+1)
    }
}
fun writeNewForced(path:Path,bytes:ByteArray) {
    safePath(path)
    FileChannel.open(path,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE,NOFOLLOW_LINKS).use { c ->
        val b=ByteBuffer.wrap(bytes);while(b.hasRemaining()) c.write(b);c.force(true)
    }
}
