package io.github.misakapiano.ccbstudio.core

import com.example.chatbar.data.local.entity.CharacterCard
import com.example.chatbar.domain.card.*
import com.example.chatbar.domain.image.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.util.Base64

/** Only image-related fields are retained. Chat/system prompts, attachments and credentials never enter Studio. */
@Serializable data class ImageCardSource(val id:String=newId(),val name:String,val stylePrompt:String,val negativePrompt:String,
    val model:NovelAiImageModel?,val characters:List<NovelAiCharacterPromptSource>,val sourceSha256:String)

fun readImageCardSource(bytes:ByteArray):ImageCardSource {
    require(bytes.size in 1..(16*1024*1024))
    val text=if(PngTextChunks.isPng(bytes)) {
        val encoded=requireNotNull(PngTextChunks.extractTextChunk(bytes,PngTextChunks.CHATBAR_CHARACTER_KEYWORD)){"PNG 缺少 CCB 角色卡"}
        require(encoded.length<=16*1024*1024)
        Base64.getDecoder().decode(encoded).decodeToString(throwOnInvalidSequence=true)
    }else bytes.decodeToString(throwOnInvalidSequence=true).removePrefix("\uFEFF")
    val json=Json{ignoreUnknownKeys=true};val root=json.parseToJsonElement(text).jsonObject
    val source=if(root["card"]!=null) {
        val pkg=json.decodeFromString<CharacterCardPackage>(text);pkg.validateForImport();val card=pkg.card
        ImageCardSource(name=card.name,stylePrompt=card.defaultImagePrompt,negativePrompt=card.defaultImageNegativePrompt,model=card.defaultNovelAiImageModel,
            characters=card.characters.map{NovelAiCharacterPromptSource(it.name,it.imagePrompt)},sourceSha256=sha256(bytes))
    }else {
        // Explicit native CCB card export; do not guess arbitrary chat/SillyTavern fields.
        require(root["id"]!=null && root["createdAt"]!=null && root["updatedAt"]!=null)
        val card=json.decodeFromString<CharacterCard>(text)
        ImageCardSource(name=card.name,stylePrompt=card.defaultImagePrompt,negativePrompt=card.defaultImageNegativePrompt,model=card.defaultNovelAiImageModel,
            characters=card.characters.map{NovelAiCharacterPromptSource(it.name,it.imagePrompt)},sourceSha256=sha256(bytes))
    }
    require(source.name.isNotBlank() && source.name.length<=100 && source.characters.size<=100)
    require(source.stylePrompt.length+source.negativePrompt.length+source.characters.sumOf{it.name.length+it.prompt.length}<=100000)
    return source
}

fun CreationState.resolvedDraft(value:NovelAiStudioDraft):NovelAiStudioDraft=if(!value.followDefaultNovelAiImageModel)value else value.copy(
    selectedModel=NovelAiImageModelResolution.resolve(null,imageCards.firstOrNull{it.id==value.importedCharacterCardId}?.model,defaultImageModel))
