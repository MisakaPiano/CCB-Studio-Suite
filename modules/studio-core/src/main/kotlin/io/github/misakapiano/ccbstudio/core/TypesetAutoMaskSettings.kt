package io.github.misakapiano.ccbstudio.core

import io.github.misakapiano.ccbstudio.assets.AssetStore
import kotlinx.serialization.Serializable
import java.io.ByteArrayInputStream
import java.util.UUID

@Serializable data class TypesetAutoMaskSettings(
    val modelAssetId:String,val modelSha256:String,val classes:List<String>,
    val confidence:Double=.3,val iou:Double=.5,val resolution:Int=640,val expansion:Double=4.0,
    val effect:String="mosaic",val strength:Int=16,val opacity:Double=1.0,val selectedPages:Boolean=false,
    val followTool:Boolean=true
) {
    fun validate(){
        UUID.fromString(modelAssetId);require(modelSha256.matches(Regex("[0-9a-f]{64}")))
        require(classes.size in 1..80&&classes.distinct().size==classes.size&&classes.all{it.isNotBlank()&&it.length<=80})
        require(confidence in .05..1.0&&iou in .05..1.0&&resolution in listOf(640,832,1024,1280)&&expansion in 0.0..200.0)
        require(effect in listOf("mosaic","blur","black","white")&&strength in 1..256&&opacity in 0.0..1.0)
    }
}

/** Explicitly selected model bytes are immutable local resources, excluded from the image catalog.
 * The existing asset backup retains them; preset exchange does not distribute the model/config. */
object TypesetAutoMaskResources {
    const val TAG="CCB_LOCAL_AUTOMASK_MODEL_V1"
    const val BUNDLED_SHA256="a12ac5532e93be9dfeb96a77fc3f3647791335c9df0de9a18fcd503f7877a828"
    val BUNDLED_ID:String=UUID.nameUUIDFromBytes(("CCB_BUNDLED_AUTOMASK_MODEL_V1:"+BUNDLED_SHA256).toByteArray()).toString()
    fun isBundled(settings:TypesetAutoMaskSettings)=settings.modelAssetId==BUNDLED_ID&&settings.modelSha256==BUNDLED_SHA256
    const val MAX_BYTES=128*1024*1024
    fun retain(workspace:StudioWorkspace,name:String,bytes:ByteArray):String=workspace.operation {
        require(bytes.size in 1..MAX_BYTES);val hash=sha256(bytes)
        val id=UUID.nameUUIDFromBytes((TAG+hash).toByteArray())
        val existing=workspace.catalog().firstOrNull{it.request().id()==id}
        if(existing==null)workspace.assets.put(AssetStore.Request(id,name.take(200),AssetStore.Kind.ORIGINAL,null,byteArrayOf(),TAG.toByteArray()),ByteArrayInputStream(bytes))
        val asset=workspace.assets.read(id).require();require(asset.request().project().contentEquals(TAG.toByteArray())&&asset.sha256()==hash&&asset.size()==bytes.size.toLong())
        id.toString()
    }
    fun read(workspace:StudioWorkspace,settings:TypesetAutoMaskSettings):Pair<String,ByteArray> = workspace.operation {
        require(!isBundled(settings)){"内置模型由程序资源读取，不在工程资料中复制"}
        settings.validate();val asset=workspace.assets.read(UUID.fromString(settings.modelAssetId)).require()
        require(asset.request().project().contentEquals(TAG.toByteArray())&&asset.size() in 1..MAX_BYTES.toLong()&&asset.sha256()==settings.modelSha256){"自动打码模型缺失或不一致，请重新选择；原工程保留"}
        val bytes=workspace.bytes(settings.modelAssetId);require(sha256(bytes)==settings.modelSha256)
        asset.request().name() to bytes
    }
}
