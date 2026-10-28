package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import com.example.chatbar.desktop.*

/** Native production hosts opt in; an offscreen composition has no OS window to control. */
internal val LocalStudioNativeWindows=staticCompositionLocalOf{false}

/** Each image/tool window owns its HWND, caption layout and close callback. Never the workbench's. */
@Composable internal fun StudioImageWindow(title:String,close:()->Unit,width:Dp=1100.dp,height:Dp=820.dp,
    onKeyEvent:(KeyEvent)->Boolean={false},content:@Composable ()->Unit) {
    if(!LocalStudioNativeWindows.current){
        StudioImageWindowProjection(title,close,content);return
    }
    Window(onCloseRequest=close,title=title,icon=StudioBrand.painter,
        undecorated=DesktopWindowChrome.isWindows,state=rememberWindowState(width=width,height=height),
        onKeyEvent={event->if(onKeyEvent(event))true else if(event.type==KeyEventType.KeyDown&&event.key==Key.Escape){close();true}else false}) {
        val chrome=rememberDesktopWindowChrome(window,close)
        val dark=StudioAppearanceRuntime.palette.dark
        SideEffect{chrome.appearance(dark)}
        CompositionLocalProvider(LocalStudioNativeWindows provides true){StudioTheme{
            StudioImageWindowFrame(title,chrome,content)
        }}
    }
}

@Composable internal fun StudioImageWindowFrame(title:String,chrome:DesktopWindowChrome,content:@Composable ()->Unit){
    val recorder=remember(chrome){WorkbenchChrome(chrome)}
    val state by chrome.state.collectAsState()
    Surface(color=Ink,contentColor=DesktopBootstrapColors.foreground,modifier=Modifier.fillMaxSize()
        .semantics{contentDescription="独立图片窗口";stateDescription=if(state.maximized)"已最大化"else"普通大小"}){
        Column(Modifier.fillMaxSize().onGloballyPositioned{recorder.width=it.size.width.toFloat();recorder.height=it.size.height.toFloat();recorder.publish()}){
            if(DesktopWindowChrome.isWindows)Row(Modifier.fillMaxWidth().height(40.dp).background(Panel)
                .onGloballyPositioned{recorder.title=recorder.rect(it);recorder.publish()},verticalAlignment=Alignment.CenterVertically){
                Text(title,Modifier.weight(1f).padding(horizontal=12.dp),maxLines=1,overflow=TextOverflow.Ellipsis,fontSize=13.sp)
                for(hit in listOf(ChromeHit.MINIMIZE,ChromeHit.MAXIMIZE,ChromeHit.CLOSE))
                    StudioCaptionButton(hit,state,false,Modifier.onGloballyPositioned{recorder.captions[hit]=recorder.rect(it);recorder.publish()},chrome::command)
            }
            Divider()
            Box(Modifier.weight(1f).fillMaxWidth()){content()}
        }
    }
}

/** Offscreen component projection only. Its size switch is NOT evidence of native maximize. */
@Composable private fun StudioImageWindowProjection(title:String,close:()->Unit,content:@Composable ()->Unit){
    var expanded by remember{mutableStateOf(false)}
    Dialog(onDismissRequest=close,properties=DialogProperties(usePlatformDefaultWidth=false)){
        Box(Modifier.fillMaxSize().padding(if(expanded)0.dp else 16.dp),contentAlignment=Alignment.Center){
            Surface(if(expanded)Modifier.fillMaxSize()else Modifier.widthIn(max=1100.dp).fillMaxWidth().heightIn(max=820.dp).fillMaxHeight(),
                shape=MaterialTheme.shapes.medium){Column(Modifier.fillMaxSize().semantics{contentDescription="预览窗口";stateDescription=if(expanded)"已最大化"else"普通大小"}){
                Row(Modifier.fillMaxWidth().height(40.dp).padding(start=12.dp),verticalAlignment=Alignment.CenterVertically){
                    Text(title,Modifier.weight(1f),maxLines=1,overflow=TextOverflow.Ellipsis)
                    IconButton({expanded=!expanded},Modifier.size(32.dp)){Icon(if(expanded)TypesetIcons.Restore else TypesetIcons.Maximize,if(expanded)"还原预览窗口"else"最大化预览窗口",Modifier.size(18.dp))}
                    IconButton(close,Modifier.size(32.dp)){Icon(TypesetIcons.Close,"关闭预览窗口",Modifier.size(18.dp))}
                }
                Divider();Box(Modifier.weight(1f).fillMaxWidth()){content()}
            }}
        }
    }
}
