package io.github.misakapiano.ccbstudio.desktop

import io.github.misakapiano.ccbstudio.core.*

/** Retain the candidate itself, without applying it or merging the current creation draft. */
internal fun saveDesignPromptReference(c:WorkbenchController,conversationId:String,turnId:String):String = c.workspace.operation {
    val conversation=c.workspace.state.creation!!.conversations.single{it.id==conversationId}
    val reply=requireNotNull(conversation.turns.single{it.id==turnId}.reply)
    val plan=reply.plan
    val source=mapOf("设计会话" to conversationId,"设计轮次" to turnId,
        "目标生图模型" to reply.targetImageModel.apiId,"设计语言模型" to reply.designModelId)
    val sections=buildList<PromptSection> {
        fun part(id:String,label:String,text:String,role:String?=null,attributes:Map<String,String> = emptyMap()) {
            if(text.isNotBlank())add(PromptSection(id,label,text,role,source+attributes))
        }
        part("style","风格 Prompt",plan.stylePrompt)
        part("base","基础 Prompt",plan.baseCaption)
        part("negative","负向 Prompt",plan.negativePrompt)
        plan.characterCaptions.forEachIndexed{i,role->
            val key="role:$i";val attributes=mapOf("原始角色位置" to (i+1).toString(),"位置" to role.center.toString())
            part("$key:positive","角色正向",role.prompt,key,attributes)
            part("$key:negative","角色负向",role.negativePrompt,key,attributes)
        }
    }
    require(sections.isNotEmpty()){"设计候选没有可比较的非空 Prompt"}
    val text=sections.joinToString("\n\n"){"[${it.label}]\n${it.text}"}
    val library=c.library.repository;val board=PromptReferences(library);val origin="ai-design:$conversationId:$turnId"
    val archived=AssetArchive(c.workspace).ids()
    val existing=library.prompts().firstOrNull{asset->val id=asset.request().id().toString()
        id !in archived && library.prompt(id).let{it.origin==origin && it.text==text && it.sections==sections && it.image==null}
    }?.request()?.id()?.toString()
    // Check capacity before creating an asset which cannot be added to the board.
    require(existing in board.load().ids || board.load().ids.size<16){"参照板最多保留 16 个来源，请先移除一项"}
    val id=existing?:library.savePrompt(text,"AI 设计 · ${reply.targetImageModel.displayName} · ${turnId.take(8)}",origin,sections=sections)
    board.add(id);c.library.refresh();id
}
