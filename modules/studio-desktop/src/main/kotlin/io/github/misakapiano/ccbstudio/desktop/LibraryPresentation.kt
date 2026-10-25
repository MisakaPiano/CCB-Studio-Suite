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
internal val LibraryBackground get()=StudioAppearanceRuntime.palette.libraryBackground
internal val LibraryPanel get()=StudioAppearanceRuntime.palette.libraryPanel
internal val LibraryLine get()=StudioAppearanceRuntime.palette.libraryLine
internal val LibraryText get()=StudioAppearanceRuntime.palette.libraryText
internal val LibraryMuted get()=StudioAppearanceRuntime.palette.libraryMuted
internal val LibrarySelected get()=StudioAppearanceRuntime.palette.accent

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
            .border(1.dp,if(selected||focus)SelectionInk else if(quiet)Color.Transparent else LibraryLine,shape)
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
        Checkbox(selected,{action()},Modifier.size(18.dp),colors=CheckboxDefaults.colors(checkedColor=Gold,checkmarkColor=StudioAppearanceRuntime.palette.primaryForeground,uncheckedColor=LibraryMuted))
        Text(label,fontSize=13.sp)
    }
}

/** A tile is one focusable/clickable surface, including its icon and empty padding. */
@Composable internal fun LibraryToolTile(label:String,description:String,icon:ImageVector,enabled:Boolean,dropHovered:Boolean,onClick:()->Unit) {
    val interaction=remember{MutableInteractionSource()};val hovered by interaction.collectIsHoveredAsState();val focused by interaction.collectIsFocusedAsState()
    val shape=RoundedCornerShape(6.dp)
    Column(Modifier.size(90.dp,62.dp).alpha(if(enabled)1f else .4f)
        .background(if(dropHovered||hovered)LibrarySelected else LibraryPanel,shape)
        .border(if(dropHovered)2.dp else 1.dp,if(dropHovered||focused)SelectionInk else LibraryLine,shape)
        .semantics{contentDescription=description;stateDescription="磁贴"}
        .hoverable(interaction).clickable(enabled=enabled,interactionSource=interaction,indication=null,role=Role.Button,onClick=onClick)
        .padding(5.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
        Icon(icon,null,Modifier.size(20.dp));Spacer(Modifier.height(3.dp))
        Text(label,fontSize=12.sp,lineHeight=15.sp,maxLines=2,textAlign=androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable internal fun LibrarySelectionCheckbox(selected:Boolean,onSelected:(Boolean)->Unit,modifier:Modifier=Modifier,enabled:Boolean=true) {
    Checkbox(selected,onSelected,modifier.size(24.dp),enabled=enabled,
        colors=CheckboxDefaults.colors(checkedColor=Gold,checkmarkColor=StudioAppearanceRuntime.palette.primaryForeground,uncheckedColor=LibraryMuted))
}

@Composable internal fun LibraryBand(panel:Boolean=false,content:@Composable RowScope.()->Unit) {
    Row(Modifier.fillMaxWidth().background(if(panel)LibraryPanel else LibraryBackground).drawBehind{drawLine(LibraryLine,Offset.Zero,Offset(size.width,0f));drawLine(LibraryLine,Offset(0f,size.height),Offset(size.width,size.height))}.padding(horizontal=12.dp,vertical=7.dp),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp),content=content)
}
