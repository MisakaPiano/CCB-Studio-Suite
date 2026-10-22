package io.github.misakapiano.ccbstudio.core

/** Conservative name hints, never a claim that an unknown provider model supports images. */
enum class ModelPurpose(val label:String,val supportsChatSelection:Boolean) {
    EMBEDDING("文本嵌入 · 不用于识图或对话",false),
    RERANK("重排序 · 不用于识图或对话",false),
    AUDIO("音频用途 · 不用于图片描述",false),
    IMAGE_GENERATION("图片生成 · 不用于图片描述",false),
    VISION_HINT("视觉候选 · 请核对服务商能力",true),
    UNKNOWN("对话能力待确认",true)
}
fun modelPurpose(id:String):ModelPurpose {
    val name=id.lowercase()
    return when {
        Regex("embed|(?:^|[/_-])bge(?:[/_-]|$)|(?:^|[/_-])e5(?:[/_-]|$)").containsMatchIn(name)->ModelPurpose.EMBEDDING
        "rerank" in name->ModelPurpose.RERANK
        Regex("(?:^|[/_-])(?:tts|asr|whisper|speech)(?:[/_-]|$)|cosyvoice|fish-speech").containsMatchIn(name)->ModelPurpose.AUDIO
        Regex("(?:^|[/_-])flux(?:[./_-]|$)|stable-diffusion|(?:^|[/_-])qwen-image(?:[/_-]|$)").containsMatchIn(name)->ModelPurpose.IMAGE_GENERATION
        Regex("(?:^|[/_-])(?:vl|vision|llava)(?:[./_-]|$)|internvl").containsMatchIn(name)->ModelPurpose.VISION_HINT
        else->ModelPurpose.UNKNOWN
    }
}
fun requireChatModelPurpose(id:String) {
    require(modelPurpose(id).supportsChatSelection){"此模型按名称属于${modelPurpose(id).label}，请选择支持对话和图片输入的模型"}
}
