package io.github.misakapiano.ccbstudio.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.nio.channels.FileChannel
import java.nio.file.*
import java.nio.file.LinkOption.NOFOLLOW_LINKS

@Serializable private data class Reservation(val version:Int=1,val number:Int,val startedMs:Long,val service:String,val previousHash:String)
class BudgetWait(val waitMillis:Long):java.io.IOException("API rate budget unavailable; wait ${waitMillis/1000+1} seconds")

/** Durable reservation before every outbound attempt, shared across providers/processes. No credentials here. */
class LiveRequestBudget(private val directory:Path,private val clock:()->Long=System::currentTimeMillis) {
    fun reserve(service:String):Int {
        require(service in setOf("LLM","NAI"));safePath(directory);Files.createDirectories(directory)
        FileChannel.open(directory.resolve(".budget.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE,NOFOLLOW_LINKS).use {channel ->
            val lock=try {channel.tryLock()}catch(e:java.nio.channels.OverlappingFileLockException){null}
                ?: throw DataProblem("IN_USE","Live API budget is being reserved")
            lock.use {
                val entries=Files.list(directory).use {it.filter {p->p.fileName.toString()!=".budget.lock"}.sorted().toList()}
                val times=mutableListOf<Long>();var previous=""
                for((i,p) in entries.withIndex()) {
                    if(p.fileName.toString()!="%08d.json".format(i+1))throw DataProblem("CORRUPT","Budget entries incomplete; no request permitted")
                    val raw=readBounded(p,2048)
                    val r=try {documentJson.decodeFromString<Reservation>(raw.decodeToString())}catch(e:Exception){throw DataProblem("CORRUPT","Budget malformed; no request permitted")}
                    if(r.version!=1 || r.number!=i+1 || r.previousHash!=previous || (times.isNotEmpty() && r.startedMs-times.last()<300000))throw DataProblem("CORRUPT","Budget chain invalid; no request permitted")
                    times+=r.startedMs;previous=sha256(raw)
                }
                val now=clock()
                if(times.isNotEmpty() && now<times.last())throw DataProblem("CLOCK_ROLLBACK","Clock moved backwards; no request permitted")
                val hour=times.filter {now-it < 3600000}
                val interval=times.lastOrNull()?.let {300000-(now-it)}?:0L
                val hourly=if(hour.size>=12)3600000-(now-hour[hour.size-12])else 0L
                val wait=maxOf(interval,hourly)
                if(wait>0)throw BudgetWait(wait)
                val r=Reservation(number=times.size+1,startedMs=now,service=service,previousHash=previous)
                // CREATE_NEW: an incomplete reservation is a fail-closed state, not a free retry.
                writeNewForced(directory.resolve("%08d.json".format(r.number)),documentJson.encodeToString(r).toByteArray())
                return r.number
            }
        }
    }
}
