package io.github.misakapiano.ccbstudio.core

import java.nio.file.*
import java.util.concurrent.TimeUnit
import kotlin.test.*

class WorkspaceProcessTest {
    private fun root()=Paths.get(System.getProperty("preview.test.root")).resolve(newId())
    private fun run(mode:String,root:Path):Pair<Int,String> {
        val java=Paths.get(System.getProperty("java.home"),"bin","java.exe").toString()
        val p=ProcessBuilder(java,"-Xmx128m","-XX:ActiveProcessorCount=2","-cp",System.getProperty("java.class.path"),WorkspaceProcessProbe::class.java.name,mode,root.toString()).redirectErrorStream(true).start()
        assertTrue(p.waitFor(30,TimeUnit.SECONDS),"Owned child process did not complete")
        return p.exitValue() to p.inputStream.readBytes().decodeToString()
    }
    @Test fun abruptStagedAndCommittedDocumentCrashesPreserveOriginals() {
        for(point in listOf("DOCUMENT_STAGED","DOCUMENT_COMMITTED")) {
            val r=root();val original=EditorRenderer.synthetic(128,128)
            val id=StudioWorkspace.open(r).use {it.importImage("original.png",original)}
            val result=run(point,r);assertEquals(73,result.first,result.second)
            if(point=="DOCUMENT_STAGED") {
                assertEquals("RECOVERY_REQUIRED",assertFailsWith<DataProblem>{StudioWorkspace.open(r)}.status)
                io.github.misakapiano.ccbstudio.assets.AssetStore.open(r.resolve("assets")).use {a->assertEquals(sha256(original),a.read(java.util.UUID.fromString(id)).require().sha256())}
            } else StudioWorkspace.open(r).use {assertEquals("after-crash",it.state.accounts.single().alias);assertContentEquals(original,it.bytes(id))}
        }
    }
    @Test fun anotherProcessCannotOwnOpenWorkspaceThenCanAfterClose() {
        val r=root();StudioWorkspace.open(r).use {assertEquals(42,run("OPEN",r).first)}
        assertEquals(0,run("OPEN",r).first)
    }
}

object WorkspaceProcessProbe {
    @JvmStatic fun main(args:Array<String>) {
        try {
            StudioWorkspace.open(Paths.get(args[1])){if(it==args[0])Runtime.getRuntime().halt(73)}.use {w->
                if(args[0]!="OPEN")w.save(w.state.copy(accounts=listOf(AccountProfile(alias="after-crash",service="NAI",provider="SYNTHETIC",model="fixture"))))
            }
        } catch(e:Exception) {System.exit(42)}
    }
}
