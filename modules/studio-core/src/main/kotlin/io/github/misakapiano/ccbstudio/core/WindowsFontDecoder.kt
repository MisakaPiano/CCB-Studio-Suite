package io.github.misakapiano.ccbstudio.core

import com.sun.jna.*
import com.sun.jna.Function
import com.sun.jna.ptr.*
import com.sun.jna.platform.win32.Guid
import com.sun.jna.win32.StdCallLibrary
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Path

/** OS DirectWrite WOFF/WOFF2 decoder, Windows 10 15063+. No font installation or network.
 * ABI verified against Windows SDK 10.0.22621 dwrite*.h: Factory5 slots 46/47, Stream 3/4/5. */
object WindowsFontDecoder {
    private interface DWrite:StdCallLibrary {fun DWriteCreateFactory(type:Int,iid:Guid.GUID,factory:PointerByReference):Int}
    private fun call(obj:Pointer,slot:Int)=Function.getFunction(obj.getPointer(0).getPointer(slot.toLong()*Native.POINTER_SIZE),Function.ALT_CONVENTION)
    private fun check(result:Int){if(result<0)throw DataProblem("FONT_INVALID","Windows rejected the font container (HRESULT ${result.toUInt().toString(16)})")}
    fun decode(bytes:ByteArray):ByteArray {
        require(bytes.size in 12..64*1024*1024)
        val signature=ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).int
        if(signature !in setOf(0x774f4646,0x774f4632))return bytes
        require(bytes.size>=48)
        val header=ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
        require(header.getInt(8).toLong() and 0xffffffffL==bytes.size.toLong()){"Font container length mismatch"}
        require(header.getShort(12).toInt() and 65535 in 1..512)
        val advertised=header.getInt(16).toLong() and 0xffffffffL
        require(advertised in 12..64L*1024*1024){"Unpacked font exceeds 64 MiB"}
        if(!System.getProperty("os.name").startsWith("Windows"))throw DataProblem("UNSUPPORTED_PLATFORM","WOFF import requires the Windows font decoder")
        val library=Native.load(Path.of(System.getenv("WINDIR"),"System32","dwrite.dll").toString(),DWrite::class.java)
        val factoryRef=PointerByReference();check(library.DWriteCreateFactory(1,Guid.GUID("{958DB99A-BE2A-4F09-AF7D-65189803D1D3}"),factoryRef))
        val factory=requireNotNull(factoryRef.value)
        try{Memory(bytes.size.toLong()).use{memory->memory.write(0,bytes,0,bytes.size)
            val container=call(factory,46).invokeInt(arrayOf(factory,memory,bytes.size));require(container in 1..2){"Unrecognized font container"}
            val streamRef=PointerByReference();check(call(factory,47).invokeInt(arrayOf(factory,container,memory,bytes.size,streamRef)))
            val stream=requireNotNull(streamRef.value)
            try{val size=LongByReference();check(call(stream,5).invokeInt(arrayOf(stream,size)));require(size.value in 12..64L*1024*1024)
                val fragment=PointerByReference();val context=PointerByReference();check(call(stream,3).invokeInt(arrayOf(stream,fragment,0L,size.value,context)))
                try{return fragment.value.getByteArray(0,size.value.toInt())}finally{call(stream,4).invokeVoid(arrayOf(stream,context.value))}
            }finally{call(stream,2).invokeInt(arrayOf(stream))}
        }}finally{call(factory,2).invokeInt(arrayOf(factory))}
    }
}
