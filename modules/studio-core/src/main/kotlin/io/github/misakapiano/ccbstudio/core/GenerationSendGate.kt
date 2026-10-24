package io.github.misakapiano.ccbstudio.core

import kotlinx.coroutines.*
import kotlinx.coroutines.selects.select

/** A local send admission point. Closing drains accepted responses; it never cancels them. */
class GenerationNotSent:java.io.IOException("Generation was stopped before send")
class GenerationSendGate {
    private var stopped=CompletableDeferred<Unit>()
    val open:Boolean @Synchronized get()=!stopped.isCompleted
    @Synchronized fun close(){stopped.complete(Unit)}
    @Synchronized fun reopen(){check(stopped.isCompleted);stopped=CompletableDeferred()}
    @Synchronized fun <T> submit(body:()->T):T {if(!open)throw GenerationNotSent();return body()}
    suspend fun await(admission:RequestAdmission)=coroutineScope {
        val signal=synchronized(this@GenerationSendGate){stopped}
        if(signal.isCompleted)throw GenerationNotSent()
        val waiting=async {admission.await("NAI")}
        try {select {signal.onAwait{throw GenerationNotSent()};waiting.onAwait{}}}
        finally {waiting.cancelAndJoin()}
    }
}
