package io.github.misakapiano.ccbstudio.core

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable data class JobReceiptRef(val assetId:String,val sha256:String)

/** Batch receipts are immutable assets; the hot workspace journal only retains their verified references. */
fun JobRecord.receipt(workspace:StudioWorkspace):String {
    val ref=receiptRef?:return recipe
    require(recipe.isEmpty()){"Conflicting receipt representations"}
    val asset=workspace.assets.read(UUID.fromString(ref.assetId)).require()
    require(asset.size()<=1024*1024 && asset.sha256()==ref.sha256 && asset.request().project().decodeToString()=="CCB_REQUEST_PAYLOAD_V1"){"Receipt reference mismatch"}
    val bytes=workspace.bytes(ref.assetId);require(sha256(bytes)==ref.sha256)
    return bytes.decodeToString(throwOnInvalidSequence=true)
}
