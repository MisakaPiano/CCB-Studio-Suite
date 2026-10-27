package io.github.misakapiano.ccbstudio.core

import com.sun.jna.*
import com.sun.jna.ptr.IntByReference
import com.sun.jna.win32.StdCallLibrary
import java.nio.file.Path
import java.security.MessageDigest

/** Hold ancestors against rename and hash/delete the SAME non-reparse file handle.
 * No recursive deletion, no glob, no generic asset-store cleanup. Windows only. */
internal object WindowsOwnedOutputDeletion {
    private interface Api:StdCallLibrary {
        fun CreateFileW(path:WString,access:Int,share:Int,security:Pointer?,creation:Int,flags:Int,template:Pointer?):Pointer?
        fun GetFileInformationByHandle(handle:Pointer,info:Pointer):Boolean
        fun ReadFile(handle:Pointer,buffer:ByteArray,length:Int,read:IntByReference,overlapped:Pointer?):Boolean
        fun SetFileInformationByHandle(handle:Pointer,kind:Int,info:Pointer,length:Int):Boolean
        fun CloseHandle(handle:Pointer):Boolean
        fun GetLastError():Int
    }
    private val api by lazy{Native.load("kernel32",Api::class.java)}
    fun delete(path:Path,size:Long,sha:String) {
        require(System.getProperty("os.name").startsWith("Windows")){"仅支持已验证的Windows文件删除"}
        val p=path.toAbsolutePath().normalize();safePath(p);val handles=mutableListOf<Pointer>()
        fun open(file:Path,leaf:Boolean):Pair<Pointer,Memory>{
            val raw=file.toString();val extended=if(raw.startsWith("\\\\?\\"))raw else if(raw.startsWith("\\\\"))"\\\\?\\UNC\\"+raw.drop(2)else "\\\\?\\"+raw
            val h=api.CreateFileW(WString(extended),if(leaf)0x80010000.toInt() else 0x80,if(leaf)1 else 3,null,3,0x02200000,null)
            require(h!=null&&Pointer.nativeValue(h)!=-1L){"无法锁定文件（${api.GetLastError()}）"};handles+=h
            val info=Memory(52);require(api.GetFileInformationByHandle(h,info));val attrs=info.getInt(0)
            require(attrs and 0x400==0){"联接／符号链接拒绝"};require((attrs and 0x10!=0)!=leaf){"文件类型改变"};return h to info
        }
        try {
            val ancestors=generateSequence(p.parent){it.parent}.toList().asReversed();ancestors.forEach{open(it,false)}
            val (h,info)=open(p,true);require(info.getInt(40)==1){"硬链接文件保留"}
            val length=(info.getInt(32).toLong() shl 32)+(info.getInt(36).toLong() and 0xffffffffL);require(length==size)
            val digest=MessageDigest.getInstance("SHA-256");val buffer=ByteArray(65536);val read=IntByReference();var total=0L
            while(true){require(api.ReadFile(h,buffer,buffer.size,read,null));val n=read.value;if(n==0)break;total+=n;require(total<=size);digest.update(buffer,0,n)}
            require(total==size&&digest.digest().joinToString(""){"%02x".format(it)}==sha){"文件内容已改变"}
            val disposition=Memory(4);disposition.setInt(0,1);require(api.SetFileInformationByHandle(h,4,disposition,4)){"删除未获文件系统允许（${api.GetLastError()}）"}
        }finally{handles.asReversed().forEach{api.CloseHandle(it)}}
    }
}
