@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class,androidx.compose.material.ExperimentalMaterialApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*
import com.example.chatbar.desktop.*

/** Presentation tokens from the approved gallery demo; no request or storage semantics. */
internal val LibraryBackground=Color(0xff0d1219)
internal val LibraryPanel=Color(0xff171e28)
internal val LibraryLine=Color(0xff344254)
internal val LibraryText=Color(0xffe0e7f1)
internal val LibraryMuted=Color(0xffa4b2c5)
internal val LibrarySelected=Color(0xff233e5e)

@Composable internal fun LibraryPresentation(content:@Composable ()->Unit) {
    val base=MaterialTheme.typography
    MaterialTheme(colors=MaterialTheme.colors.copy(primary=Gold,background=LibraryBackground,surface=LibraryPanel,onSurface=LibraryText),
        typography=base.copy(body1=base.body1.copy(fontSize=13.sp,lineHeight=20.sp),body2=base.body2.copy(fontSize=13.sp),caption=base.caption.copy(fontSize=12.sp))) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
            Surface(Modifier.fillMaxSize(),color=LibraryBackground,contentColor=LibraryText,content=content)
        }
    }
}

@Composable internal fun LibraryAction(label:String,enabled:Boolean=true,selected:Boolean=false,
    icon:ImageVector?=studioActionIcon(label),style:StudioActionStyle=studioActionStyle(label),accessibilityLabel:String=label,expanded:Boolean?=null,onClick:()->Unit) {
    val interaction=remember{MutableInteractionSource()};val hover by interaction.collectIsHoveredAsState();val focus by interaction.collectIsFocusedAsState()
    val quiet=style==StudioActionStyle.TERTIARY||style==StudioActionStyle.GHOST
    val shape=RoundedCornerShape(5.dp)
    TooltipArea(tooltip={Surface(color=LibraryPanel){Text(accessibilityLabel,Modifier.padding(8.dp))}}) {
        Row(Modifier.heightIn(min=31.dp).alpha(if(enabled)1f else .4f)
            .background(if(selected||hover)LibrarySelected else if(quiet)Color.Transparent else LibraryPanel,shape)
            .border(1.dp,if(selected||focus)Gold else if(quiet)Color.Transparent else LibraryLine,shape)
            .semantics{contentDescription=accessibilityLabel;expanded?.let{stateDescription=if(it)"已展开"else"已收起"}}
            .hoverable(interaction).clickable(enabled=enabled,interactionSource=interaction,indication=null,role=Role.Button,onClick=onClick)
            .padding(horizontal=8.dp,vertical=5.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)) {
            expanded?.let{Text(if(it)"▾"else"▸")}
            icon?.let{Icon(it,null,Modifier.size(16.dp))}
            if(label.isNotBlank())Text(label,fontSize=13.sp,maxLines=1)
        }
    }
}

@Composable internal fun LibraryToggle(label:String,selected:Boolean,action:()->Unit) {
    Row(Modifier.height(31.dp).clickable(role=Role.Checkbox,onClick=action).semantics{contentDescription=label;this.selected=selected},verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)) {
        Checkbox(selected,{action()},Modifier.size(18.dp),colors=CheckboxDefaults.colors(checkedColor=Gold,uncheckedColor=LibraryMuted))
        Text(label,fontSize=13.sp)
    }
}

@Composable internal fun LibraryBand(panel:Boolean=false,content:@Composable RowScope.()->Unit) {
    Row(Modifier.fillMaxWidth().background(if(panel)LibraryPanel else LibraryBackground).drawBehind{drawLine(LibraryLine,Offset.Zero,Offset(size.width,0f));drawLine(LibraryLine,Offset(0f,size.height),Offset(size.width,size.height))}.padding(horizontal=12.dp,vertical=7.dp),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp),content=content)
}
