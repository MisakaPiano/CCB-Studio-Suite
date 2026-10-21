package com.example.chatbar.desktop

import java.awt.EventQueue
import java.awt.Window
import java.nio.file.Path
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileNameExtensionFilter

/** One live Suite top-level owner; Windows always uses the OS common item dialog. */
internal object StudioFileDialogs {
    private val owner=DesktopDialogOwner()
    private val service by lazy { WindowsCommonItemDialog(owner::requireHandle) }
    private var failure:((String)->Unit)?=null
    fun attach(window:Window,onFailure:(String)->Unit){owner.attach(window);failure=onFailure}
    fun detach(window:Window){owner.detach(window);failure=null}
    fun folder(title:String,initial:Path?=null):Path?=choose(DesktopDialogRequest(DesktopDialogKind.FOLDER,title=title,initialFolder=initial)).firstOrNull()
    fun open(title:String,extensions:List<String> = listOf("*")):Path?=openMany(title,extensions,false).firstOrNull()
    fun openMany(title:String,extensions:List<String> = listOf("*"),multiple:Boolean=true):List<Path> =
        choose(DesktopDialogRequest(DesktopDialogKind.OPEN,DesktopFileType(title,extensions),title=title,multiple=multiple))
    fun save(title:String,name:String,extensions:List<String> = listOf("*")):Path?=
        choose(DesktopDialogRequest(DesktopDialogKind.SAVE,DesktopFileType(title,extensions),name,title)).firstOrNull()
    private fun choose(request:DesktopDialogRequest):List<Path> = try {
        if(DesktopWindowChrome.isWindows)service.pickPaths(request) else swing(request)
    }catch(e:Exception){
        val report=failure?:throw e
        val message="文件选择器未能打开或读取选择结果；现有文件未改变，请重试。"
        if(EventQueue.isDispatchThread())report(message)else EventQueue.invokeLater{report(message)}
        emptyList()
    }
    private fun swing(request:DesktopDialogRequest):List<Path> {
        var result=emptyList<Path>()
        val action={
            val chooser=JFileChooser(request.initialFolder?.toFile()).apply{
                dialogTitle=request.title;isMultiSelectionEnabled=request.multiple
                fileSelectionMode=if(request.kind==DesktopDialogKind.FOLDER)JFileChooser.DIRECTORIES_ONLY else JFileChooser.FILES_ONLY
                if(request.extensions.isNotEmpty() && "*" !in request.extensions)fileFilter=FileNameExtensionFilter(request.title,*request.extensions.toTypedArray())
                request.suggestedName?.let{selectedFile=java.io.File(it)}
            }
            val choice=if(request.kind==DesktopDialogKind.SAVE)chooser.showSaveDialog(null)else chooser.showOpenDialog(null)
            if(choice==JFileChooser.APPROVE_OPTION)result=(if(request.multiple)chooser.selectedFiles.toList()else listOf(chooser.selectedFile)).map{request.resultPath(it.absolutePath)}
        }
        if(EventQueue.isDispatchThread())action()else SwingUtilities.invokeAndWait(action)
        return result
    }
}
