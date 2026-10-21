package io.github.misakapiano.ccbstudio.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.util.UUID
import com.example.chatbar.domain.image.NovelAiStudioDraft
import com.example.chatbar.domain.image.NovelAiDesignConversation

val documentJson = Json { encodeDefaults = true; ignoreUnknownKeys = false }
fun newId(): String = UUID.randomUUID().toString()
fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

@Serializable data class AccountProfile(
    val id: String = newId(), val alias: String, val service: String,
    val provider: String, val endpoint: String = "https://synthetic.invalid",
    val model: String, val secretRef: String? = null,
) {
    fun validate() {
        UUID.fromString(id)
        require(alias.isNotBlank() && alias.length <= 100)
        require(service in setOf("LLM", "NAI"))
        if(provider == "SYNTHETIC") require(endpoint == "https://synthetic.invalid")
        else {
            require(provider == if(service == "NAI") "NOVELAI" else "OPENAI_COMPATIBLE")
            val uri = java.net.URI(endpoint)
            require(uri.scheme == "https" && !uri.host.isNullOrBlank() && uri.userInfo == null && uri.query == null && uri.fragment == null) { "Use an HTTPS endpoint without credentials, query or fragment" }
            if(service == "NAI") require(endpoint.trimEnd('/') == "https://image.novelai.net") { "NovelAI uses its authoritative image endpoint" }
        }
        require(model.isNotBlank() && model.length <= 160)
        secretRef?.let { UUID.fromString(it) }
    }
}
@Serializable data class Point(val x: Float, val y: Float)
@Serializable data class Layer(
    val id: String = newId(), val kind: String, val x: Float = 20f, val y: Float = 20f,
    val width: Float = 100f, val height: Float = 50f, val text: String = "Preview",
    val font: String = "Dialog", val fontSize: Int = 28, val color: Int = -16777216,
    val vertical: Boolean = false, val mosaic: Boolean = false, val blockSize: Int = 16,
    val points: List<Point> = emptyList(), val visible: Boolean = true,
) {
    fun validate() {
        UUID.fromString(id)
        require(kind in setOf("TEXT", "RECT", "BRUSH", "POLYGON")) { "Unsupported layer retained; not rendered" }
        require(listOf(x,y,width,height).all { it.isFinite() && it in -100000f..100000f })
        require(width > 0 && height > 0 && fontSize in 8..240 && blockSize in 2..128)
        require(text.length <= 10000 && points.size <= 10000)
        require(points.all { it.x.isFinite() && it.y.isFinite() && it.x in -100000f..100000f && it.y in -100000f..100000f })
        if(kind == "POLYGON") require(points.size >= 3)
        if(kind == "BRUSH") require(points.size >= 2)
    }
}
@Serializable data class ProjectPage(val assetId: String, val layers: List<Layer> = emptyList())
@Serializable data class EditSnapshot(val pages: List<ProjectPage> = emptyList())
@Serializable data class EditProject(
    val id: String = newId(), val name: String, val current: EditSnapshot = EditSnapshot(),
    val undo: List<EditSnapshot> = emptyList(), val redo: List<EditSnapshot> = emptyList(),
) {
    fun change(next: EditSnapshot) = copy(current=next, undo=(undo + current).takeLast(30), redo=emptyList())
    fun undo() = if(undo.isEmpty()) this else copy(current=undo.last(), undo=undo.dropLast(1), redo=(redo+current).takeLast(30))
    fun redo() = if(redo.isEmpty()) this else copy(current=redo.last(), redo=redo.dropLast(1), undo=(undo+current).takeLast(30))
}
@Serializable data class DesignRecord(val id: String = newId(), val instruction: String, val previousPlan: String?, val resultPlan: String,
    val account: AccountProfile, val exchanges: List<String>, val createdAt: Long = System.currentTimeMillis())
@Serializable data class JobRecord(val id: String = newId(), val account: AccountProfile, val recipe: String,
    val state: String, val results: List<String> = emptyList(), val detail: String = "", val createdAt: Long = System.currentTimeMillis())
@Serializable data class StudioState(
    val version: Int = 1,
    val accounts: List<AccountProfile> = emptyList(),
    val projects: List<EditProject> = emptyList(),
    val designs: List<DesignRecord> = emptyList(),
    val jobs: List<JobRecord> = emptyList(),
    val creation: CreationState? = null,
) {
    fun validate() {
        if(version !in 1..2) throw DataProblem("UNSUPPORTED_VERSION", "Studio document version $version retained")
        if(version == 1) require(accounts.all {it.provider == "SYNTHETIC"} && creation == null) { "Legacy Preview is synthetic-only" }
        require(accounts.size <= 100 && accounts.map { it.id }.distinct().size == accounts.size)
        accounts.forEach { it.validate() }
        creation?.validate()
        require(projects.map { it.id }.distinct().size == projects.size)
        projects.forEach { p ->
            UUID.fromString(p.id); require(p.name.length in 1..100)
            (listOf(p.current)+p.undo+p.redo).forEach { s ->
                require(s.pages.map { it.assetId }.distinct().size == s.pages.size)
                s.pages.forEach { page -> UUID.fromString(page.assetId); require(page.layers.size <= 500); page.layers.forEach { it.validate() } }
            }
        }
    }
}

/** Versioned application state; authoritative draft and conversation models stay unchanged. */
@Serializable data class CreationState(
    val draft: NovelAiStudioDraft = NovelAiStudioDraft(),
    val undo: List<NovelAiStudioDraft> = emptyList(),
    val redo: List<NovelAiStudioDraft> = emptyList(),
    val conversations: List<NovelAiDesignConversation> = emptyList(),
    val currentConversationId: String? = null,
    val composer: String = "",
    val attachCurrentPrompt: Boolean = false,
    val llmAccountId: String? = null,
    val naiAccountId: String? = null,
) {
    fun change(next: NovelAiStudioDraft) = copy(draft=next, undo=(undo+draft).takeLast(30), redo=emptyList())
    fun undoDraft() = if(undo.isEmpty()) this else copy(draft=undo.last(),undo=undo.dropLast(1),redo=(redo+draft).takeLast(30))
    fun redoDraft() = if(redo.isEmpty()) this else copy(draft=redo.last(),redo=redo.dropLast(1),undo=(undo+draft).takeLast(30))
    fun validate() {
        require(composer.length <= 100000 && undo.size <= 30 && redo.size <= 30)
        require(conversations.map {it.id}.distinct().size == conversations.size)
        conversations.forEach {c -> UUID.fromString(c.id); require(c.turns.map {it.id}.distinct().size == c.turns.size); c.turns.forEach {UUID.fromString(it.id)} }
        require(currentConversationId == null || conversations.any {it.id == currentConversationId})
        listOfNotNull(llmAccountId,naiAccountId).forEach {UUID.fromString(it)}
    }
}
class DataProblem(val status: String, message: String, cause: Throwable?=null): java.io.IOException(message,cause)
