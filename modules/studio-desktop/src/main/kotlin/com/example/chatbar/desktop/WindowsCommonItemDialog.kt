package com.example.chatbar.desktop

import java.awt.EventQueue
import java.awt.Toolkit
import java.nio.file.Path
import java.util.Locale
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

internal enum class DesktopDialogKind { OPEN, SAVE, FOLDER }

internal data class DesktopDialogRequest(
    val kind: DesktopDialogKind,
    val type: DesktopFileType? = null,
    val suggestedName: String? = null,
    val title: String = "选择文件",
    val initialFolder: Path? = null,
    val multiple: Boolean = false,
) {
    private fun extension(value: String): String = value.trim().removePrefix("*.").removePrefix(".")
        .lowercase(Locale.ROOT).also { require(it == "*" || it.matches(Regex("[a-z0-9]+"))) { "Invalid file extension" } }
    val extensions = type?.extensions?.map(::extension)?.distinct().orEmpty()
    val defaultExtension = type?.defaultExtension?.let(::extension)
    val pattern = extensions.joinToString(";") { "*.$it" }
    init {
        require(kind == DesktopDialogKind.FOLDER || (extensions.isNotEmpty() && defaultExtension in extensions))
        require(!multiple || kind == DesktopDialogKind.OPEN)
    }
    val requiredOptions: Int get() = 0x40 or 0x800 or when (kind) {
        DesktopDialogKind.OPEN -> 0x1000
        DesktopDialogKind.SAVE -> 0x2
        DesktopDialogKind.FOLDER -> 0x20
    }
    fun resultPath(raw: String): Path {
        val path = Path.of(raw)
        require(path.isAbsolute) { "Native dialog returned a non-filesystem path" }
        val normalized = path.normalize()
        return if (kind == DesktopDialogKind.SAVE && defaultExtension != "*" &&
            normalized.fileName.toString().substringAfterLast('.', "").lowercase(Locale.ROOT) !in extensions
        ) normalized.resolveSibling("${normalized.fileName}.$defaultExtension") else normalized
    }
}

internal const val WINDOWS_DIALOG_CANCELLED = -2147023673 // HRESULT_FROM_WIN32(ERROR_CANCELLED)

internal fun checkDialogResult(operation: String, hresult: Int) {
    check(hresult >= 0) { "$operation failed (HRESULT 0x${hresult.toUInt().toString(16)})" }
}

/** Narrow apartment-local seam; none of these native resources may escape a dialog operation. */
internal interface CommonDialogBackend {
    fun initializeSta(): AutoCloseable
    fun create(kind: DesktopDialogKind): CommonDialogHandle
}
internal interface CommonDialogHandle : AutoCloseable {
    fun getOptions(): Int
    fun setOptions(options: Int)
    fun setFilter(description: String, pattern: String)
    fun setDefaultExtension(extension: String)
    fun setFileName(name: String)
    fun setTitle(title: String)
    fun setFolder(path: Path) { error("Initial folder is not supported by this backend") }
    fun show(owner: Long): Int
    fun result(): CommonDialogItem
    fun results(): CommonDialogItems { error("Multiple selection is not supported by this backend") }
}
internal interface CommonDialogItems : AutoCloseable { fun count(): Int; fun item(index:Int): CommonDialogItem }
internal interface CommonDialogItem : AutoCloseable { fun displayName(): CommonDialogPath }
internal interface CommonDialogPath : AutoCloseable { fun read(): String }

/** One application-owned STA. AWT's secondary loop keeps owner messages flowing during Show. */
internal class WindowsCommonItemDialog(
    private val owner: () -> Long,
    private val backend: CommonDialogBackend = JnaCommonDialogBackend(),
) : AutoCloseable {
    private val executor = Executors.newSingleThreadExecutor { task ->
        Thread(task, "CCB-native-file-dialog-STA").apply { isDaemon = true }
    }
    private val active = AtomicBoolean()

    fun pick(request: DesktopDialogRequest): Path? = pickPaths(request).firstOrNull()
    fun pickPaths(request: DesktopDialogRequest): List<Path> = execute {
        // Resolve only a live top-level owner, immediately before this operation starts.
        val hwnd = owner()
        configured(request) { dialog ->
            val hr = dialog.show(hwnd)
            if (hr == WINDOWS_DIALOG_CANCELLED) emptyList() else {
                checkDialogResult("IFileDialog.Show", hr)
                if(request.multiple) dialog.results().use { items ->
                    val count=items.count();check(count in 1..10000){"Invalid selection count"}
                    (0 until count).map { i -> items.item(i).use { item -> item.displayName().use { request.resultPath(it.read()) } } }
                } else listOf(dialog.result().use { item -> item.displayName().use { request.resultPath(it.read()) } })
            }
        }
    }

    /** Noninteractive real-COM validation uses the same STA, configuration and cleanup as Show. */
    internal fun prepare(request: DesktopDialogRequest) = execute { configured(request) {} }

    private fun <T> configured(request: DesktopDialogRequest, body: (CommonDialogHandle) -> T): T =
        backend.initializeSta().use {
            backend.create(request.kind).use { dialog ->
                val options=(dialog.getOptions() or request.requiredOptions) and 0x200.inv()
                dialog.setOptions(if(request.multiple)options or 0x200 else options)
                dialog.setTitle(request.title)
                request.initialFolder?.let(dialog::setFolder)
                if (request.kind == DesktopDialogKind.FOLDER) {
                    // Folder selection uses the caller's explicit task title.
                } else {
                    dialog.setFilter(requireNotNull(request.type).description, request.pattern)
                    if (request.kind == DesktopDialogKind.SAVE) {
                        if(request.defaultExtension!="*")dialog.setDefaultExtension(requireNotNull(request.defaultExtension))
                        request.suggestedName?.let(dialog::setFileName)
                    }
                }
                body(dialog)
            }
        }

    private fun <T> execute(action: () -> T): T {
        check(active.compareAndSet(false, true)) { "A native file dialog is already active" }
        try {
            val loop = if (EventQueue.isDispatchThread()) Toolkit.getDefaultToolkit().systemEventQueue.createSecondaryLoop() else null
            val future = executor.submit<T> {
                try { action() } finally { if (loop != null) EventQueue.invokeLater { loop.exit() } }
            }
            if (loop != null) check(loop.enter()) { "Could not enter native dialog event loop" }
            // Do not abandon apartment-owned resources when a waiting worker is interrupted.
            var interrupted = false
            try {
                while (true) {
                    try { return future.get() }
                    catch (_: InterruptedException) { interrupted = true }
                    catch (failure: ExecutionException) { throw requireNotNull(failure.cause) }
                }
            } finally { if (interrupted) Thread.currentThread().interrupt() }
        } finally { active.set(false) }
    }

    override fun close() { executor.shutdown() }
}
