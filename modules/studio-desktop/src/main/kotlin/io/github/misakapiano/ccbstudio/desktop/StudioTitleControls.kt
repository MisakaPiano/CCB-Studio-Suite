package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chatbar.desktop.*

/** Drawing and interaction adapted directly from DesktopTitleBar.kt / DesktopTitleBarPresentation.kt
 * at Desktop 37c67c724fe21107d7eaff0306439481c6f2bf07. Suite routes and blue brand are local. */
@Composable internal fun StudioCaptionButton(hit:ChromeHit,state:DesktopChromeState,compact:Boolean,
    modifier:Modifier=Modifier,onCommand:(DesktopWindowCommand)->Unit) {
    val hover=state.hovered==hit
    val down=state.pressed==hit && hover
    val background=when {
        hover && hit==ChromeHit.CLOSE->if(down)Color(0xffc42b1c)else Color(0xffe81123)
        down->DesktopBootstrapColors.border
        hover->DesktopBootstrapColors.muted
        else->Color.Transparent
    }
    val label=when(hit){ChromeHit.MINIMIZE->"最小化";ChromeHit.MAXIMIZE->if(state.maximized)"还原"else"最大化";else->"关闭"}
    Box(modifier.width(if(compact)42.dp else 46.dp).fillMaxHeight().background(background)
        .semantics{contentDescription=label}.clickable(role=Role.Button){desktopCaptionCommand(hit,state.maximized)?.let(onCommand)},contentAlignment=Alignment.Center) {
        val ink=if(hover && hit==ChromeHit.CLOSE)Color.White else DesktopBootstrapColors.foreground
        Canvas(Modifier.size(12.dp)) {
            val stroke=1.dp.toPx()
            when(hit) {
                ChromeHit.MINIMIZE->drawLine(ink,Offset(1.dp.toPx(),size.height/2),Offset(size.width-1.dp.toPx(),size.height/2),stroke)
                ChromeHit.MAXIMIZE->{
                    if(state.maximized) {
                        drawRect(ink,Offset(3.dp.toPx(),1.dp.toPx()),Size(8.dp.toPx(),8.dp.toPx()),style=Stroke(stroke))
                        drawRect(if(hover)background else Panel,Offset(1.dp.toPx(),3.dp.toPx()),Size(8.dp.toPx(),8.dp.toPx()))
                        drawRect(ink,Offset(1.dp.toPx(),3.dp.toPx()),Size(8.dp.toPx(),8.dp.toPx()),style=Stroke(stroke))
                    } else drawRect(ink,Offset(1.dp.toPx(),1.dp.toPx()),Size(10.dp.toPx(),10.dp.toPx()),style=Stroke(stroke))
                }
                else->{
                    drawLine(ink,Offset(1.dp.toPx(),1.dp.toPx()),Offset(size.width-1.dp.toPx(),size.height-1.dp.toPx()),stroke)
                    drawLine(ink,Offset(size.width-1.dp.toPx(),1.dp.toPx()),Offset(1.dp.toPx(),size.height-1.dp.toPx()),stroke)
                }
            }
        }
    }
}

internal val studioRoutes=listOf("创作","AI 设计","图库","编辑","资产管理","账户","更新")

@OptIn(ExperimentalFoundationApi::class)
@Composable internal fun StudioRouteButton(label:String,selected:Boolean,compact:Boolean,modifier:Modifier=Modifier,pending:Boolean=false,onClick:()->Unit) {
    val icon=when(label){"创作"->DesktopAppIcons.Star;"AI 设计"->DesktopAppIcons.Chat;"图库"->DesktopAppIcons.ImageAdd;"编辑"->DesktopAppIcons.Edit;"资产管理"->DesktopAppIcons.Data;"账户"->DesktopAppIcons.Settings;else->DesktopAppIcons.Refresh}
    TooltipArea(tooltip={Surface(shape=RoundedCornerShape(5.dp)){Text(label,Modifier.padding(8.dp))}}) {
        Row(modifier.width(if(compact)36.dp else 88.dp).height(36.dp)
            .background(if(selected)DesktopBootstrapColors.muted else Color.Transparent,RoundedCornerShape(6.dp))
            .semantics{contentDescription=label}.selectable(selected,role=Role.Tab,onClick=onClick),
            verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center) {
            val ink=if(selected)Gold else Quiet
            Icon(icon,null,Modifier.size(17.dp),tint=ink)
            if(!compact){Spacer(Modifier.width(6.dp));Text(label,fontSize=13.sp,color=ink,maxLines=1)}
            if(pending)Box(Modifier.padding(start=3.dp).size(6.dp).background(Gold,RoundedCornerShape(3.dp)).semantics{contentDescription="有待更新版本"})
        }
    }
}
