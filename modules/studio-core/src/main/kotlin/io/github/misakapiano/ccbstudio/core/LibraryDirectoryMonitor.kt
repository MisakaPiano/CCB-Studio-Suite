package io.github.misakapiano.ccbstudio.core

import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.TimeUnit

/** Watches only explicit, same-workspace registrations. No file bytes are read by this monitor.
 * Full reconciliation on overflow/periodically is delegated to the existing bounded import scan. */
class LibraryDirectoryMonitor(private val folders:List<LibraryFolder>,private val reconcileMillis:Long=60000,
    private val beforeNewRegistration:(Path)->Unit={}):AutoCloseable {
    private val service=FileSystems.getDefault().newWatchService()
    private val keys=mutableMapOf<WatchKey,Set<String>>()
    private val paths=mutableMapOf<Path,WatchKey>()
    private val initial=folders.map{it.id}.toMutableSet()
    private var lastReconcile=System.nanoTime()
    init { try{register()}catch(e:Exception){service.close();throw e} }
    private fun register() {
        val directories=linkedMapOf<Path,MutableSet<String>>()
        for(folder in folders){val root=Path.of(folder.path);safePath(root)
            if(!Files.isDirectory(root,LinkOption.NOFOLLOW_LINKS))continue
            Files.walkFileTree(root,emptySet(),if(folder.recursive)32 else 1,object:SimpleFileVisitor<Path>() {
                override fun preVisitDirectory(dir:Path,attrs:BasicFileAttributes):FileVisitResult {
                    if(runCatching{safePath(dir)}.isFailure)return FileVisitResult.SKIP_SUBTREE
                    require(directories.size<2000){"Too many watched directories; narrow the selected roots"}
                    directories.getOrPut(dir){mutableSetOf()}.add(folder.id);return FileVisitResult.CONTINUE
                }
            })
        }
        // Keep unchanged registrations live: cancelling every root can lose another root's event
        // while discovering a new subtree. Only replace invalid or removed directory keys.
        paths.filter{(path,key)->path !in directories || !key.isValid}.toList().forEach{(path,key)->key.cancel();keys.remove(key);paths.remove(path)}
        directories.forEach{(path,ids)->
            val key=paths[path]?:run{beforeNewRegistration(path);path.register(service,StandardWatchEventKinds.ENTRY_CREATE,StandardWatchEventKinds.ENTRY_MODIFY,StandardWatchEventKinds.ENTRY_DELETE).also{paths[path]=it}}
            keys[key]=ids
        }
    }
    fun changes(timeoutMillis:Long=500):Set<String> {
        require(timeoutMillis in 0..1000)
        val result=initial.toMutableSet();initial.clear()
        var key=if(result.isEmpty())service.poll(timeoutMillis,TimeUnit.MILLISECONDS)else service.poll()
        var refresh=false
        while(key!=null){val current=key;val ids=keys[current].orEmpty();val events=current.pollEvents()
            if(ids.isEmpty()){current.reset();key=service.poll();continue}
            if(events.isNotEmpty()){result+=ids;refresh=true}
            if(!current.reset()){keys.remove(current);result+=ids;refresh=true}
            key=service.poll()
        }
        if(TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-lastReconcile)>=reconcileMillis){result+=folders.map{it.id};refresh=true;lastReconcile=System.nanoTime()}
        if(refresh)register()
        return result
    }
    override fun close(){service.close()}
}
