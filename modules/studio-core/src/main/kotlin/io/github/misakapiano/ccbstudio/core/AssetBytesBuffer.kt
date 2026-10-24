package io.github.misakapiano.ccbstudio.core

import java.io.OutputStream

/** Private-to-module byte ownership, no asset records/cache/schema. Never retains the store's reused buffer. */
internal class AssetBytesBuffer:OutputStream() {
    private val blocks=ArrayList<ByteArray>()
    private var tailSize=0
    private var size=0
    private val single=ByteArray(1)
    override fun write(value:Int){single[0]=value.toByte();write(single,0,1)}
    override fun write(bytes:ByteArray,offset:Int,length:Int) {
        java.util.Objects.checkFromIndexSize(offset,length,bytes.size)
        val nextSize=Math.addExact(size,length);var position=offset;var remaining=length
        while(remaining>0) {
            if(blocks.isEmpty() || tailSize==blocks.last().size) {
                blocks+=ByteArray(if(blocks.isEmpty())minOf(65536,remaining)else 65536);tailSize=0
            }
            val tail=blocks.last();val count=minOf(remaining,tail.size-tailSize)
            System.arraycopy(bytes,position,tail,tailSize,count);position+=count;remaining-=count;tailSize+=count
        }
        size=nextSize
    }
    fun bytes():ByteArray {
        if(blocks.size==1 && tailSize==blocks[0].size)return blocks[0]
        return ByteArray(size).also{result->var position=0
            blocks.forEachIndexed{index,block->val count=if(index==blocks.lastIndex)tailSize else block.size
                System.arraycopy(block,0,result,position,count);position+=count}
        }
    }
}
