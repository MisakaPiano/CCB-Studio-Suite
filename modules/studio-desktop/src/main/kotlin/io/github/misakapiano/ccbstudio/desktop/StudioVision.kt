package io.github.misakapiano.ccbstudio.desktop

import com.example.chatbar.data.local.JsonFileStorage
import com.example.chatbar.data.local.entity.*
import com.example.chatbar.data.repository.*
import com.example.chatbar.domain.chat.*
import com.example.chatbar.domain.model.*
import com.example.chatbar.domain.image.*
import com.example.chatbar.domain.prompt.AuxiliaryPromptAuthority
import io.github.misakapiano.ccbstudio.core.*
import java.nio.file.*
import okhttp3.OkHttpClient
import kotlinx.coroutines.flow.collect

/** The authoritative resolver consumes a credential-free snapshot; secrets hydrate only in memory. */
internal suspend fun studioVision(root:Path,accounts:List<AccountProfile>,vault:SecretVault,admission:RequestAdmission,http:OkHttpClient):ImageUnderstandingService {
    Files.createDirectories(root)
    val storage=JsonFileStorage(root)
    val profiles=accounts.filter{it.service=="LLM" && it.provider=="OPENAI_COMPATIBLE"}.associateBy{it.id}
    val policy=object:ModelCredentialPersistencePolicy {
        override val requireVerifiedEntityDeletion=true
        override suspend fun hydrate(persisted:ModelConfig):ModelConfig {
            require(persisted.apiKey.isEmpty())
            val profile=profiles[persisted.id]?:return persisted
            val token=profile.secretRef?.let{runCatching{vault.use(it){bytes->bytes.decodeToString()}}.getOrDefault("")}.orEmpty()
            return profile.modelConfig().copy(apiKey=token)
        }
        override suspend fun persist(runtime:ModelConfig,persistEntity:suspend (ModelConfig)->Unit):ModelConfig {
            val stripped=runtime.copy(apiKey="");persistEntity(stripped);return stripped
        }
        override suspend fun delete(logicalModelId:String,deleteEntity:suspend ()->Unit)=error("Snapshot deletion is not an account operation")
    }
    val models=ModelRepository(storage,credentialPersistencePolicy=policy)
    profiles.values.forEach{models.saveModel(it.modelConfig())}
    val resolver=EffectiveModelResolver(models,SettingsRepository(storage),object:PresetModelCatalogSource {
        override val catalog=PresetModelCatalog(chatModels=emptyList())
        override val modelCatalogVersion:Int?=null
    })
    return ImageUnderstandingService(resolver){encoded,model,delta->
        val profile=requireNotNull(profiles[model.id])
        val transport=StudioTextTransport(profile,vault,admission,http)
        val messages=listOf(ChatApiMessage.text("system",AuxiliaryPromptAuthority.IMAGE_DESCRIPTION_PROMPT),ChatApiMessage.withImage("user","",encoded))
        // PLAN selects the same preservesInputRoles=false auxiliary envelope as Desktop describe().
        val text=StringBuilder();var complete=false
        transport.streamText(NovelAiTextStage.PLAN,messages,model.forImageDescriptionRequest()).collect{event->when(event){
            is StreamEvent.Delta->{text.append(event.text);delta(event.text)}
            is StreamEvent.Error->throw event.asException()
            StreamEvent.Done->complete=true
            else->Unit
        }}
        check(complete && text.isNotBlank())
        text.toString().replace(Regex("\\s+")," ").trim()
    }
}
