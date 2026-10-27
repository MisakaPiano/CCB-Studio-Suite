package io.github.misakapiano.ccbstudio.desktop

import io.github.misakapiano.ccbstudio.core.*

/** The shipped model is a program resource, never a user path or a per-project copy. */
internal object BundledAutoMaskModel {
    const val RESOURCE="/models/automask/nsfw-anime-medium-x1280.onnx"
    const val NAME="内置动漫区域识别模型"
    const val BYTES=47600269
    private val cached:LocalMaskModel by lazy {
        val data=javaClass.getResourceAsStream(RESOURCE)?.use{it.readNBytes(BYTES+1)}
            ?:error("程序内置识别模型缺失，请修复安装；现有工程和自选模型保留")
        verify(data)
        LocalOnnxMaskDetector.inspect(NAME,data)
    }
    fun verify(bytes:ByteArray){
        require(bytes.size==BYTES&&sha256(bytes)==TypesetAutoMaskResources.BUNDLED_SHA256){"程序内置识别模型校验未通过，请修复安装；未改用未知模型"}
    }
    fun load():LocalMaskModel=cached
}
