package io.github.misakapiano.ccbstudio.core

import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicReference

class GenerationFailure(val code:String,val notBefore:Long=0,val uncertain:Boolean=false):java.io.IOException("生成暂停 · $code")

/** Captures only status and Retry-After. Never retains response bodies, tokens, URLs or prompts. */
class GenerationStatusInterceptor(private val now:()->Long=System::currentTimeMillis):Interceptor {
    val failure=AtomicReference<GenerationFailure?>(null)
    override fun intercept(chain:Interceptor.Chain):Response {
        val response=chain.proceed(chain.request())
        if(!response.isSuccessful){
            failure.set(GenerationFailure(response.code.toString(),retryAfterDeadline(response.header("Retry-After"),now()),response.code>=500))
            // Authority must not wait for an error body (especially a stalled 429 response).
            response.close()
            return response.newBuilder().body(ByteArray(0).toResponseBody(null)).build()
        };return response
    }
}
fun retryAfterDeadline(value:String?,now:Long):Long {
    if(value==null)return 0
    value.trim().toLongOrNull()?.let{seconds->return now+seconds.coerceIn(0,31536000)*1000L}
    return runCatching{ZonedDateTime.parse(value,DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli().coerceAtLeast(now)}.getOrDefault(0)
}
