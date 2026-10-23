package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.PopupProperties

/** Menu and trigger use the same 13sp presentation scale on every Suite page/window. */
@Composable internal fun StudioMenu(expanded:Boolean,onDismissRequest:()->Unit,modifier:Modifier=Modifier,offset:DpOffset=DpOffset.Zero,
    scrollState:androidx.compose.foundation.ScrollState=androidx.compose.foundation.rememberScrollState(),properties:PopupProperties=PopupProperties(focusable=true),content:@Composable ColumnScope.()->Unit) {
    val style=MaterialTheme.typography.body1.copy(fontSize=13.sp,lineHeight=20.sp)
    MaterialTheme(typography=MaterialTheme.typography.copy(body1=style,subtitle1=style)) {
        DropdownMenu(expanded,onDismissRequest,modifier,offset,scrollState,properties,content)
    }
}
@Composable internal fun StudioMenuItem(onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,
    contentPadding:PaddingValues=PaddingValues(horizontal=16.dp),interactionSource:MutableInteractionSource=remember{MutableInteractionSource()},content:@Composable RowScope.()->Unit) {
    DropdownMenuItem(onClick,modifier,enabled,contentPadding,interactionSource) {
        ProvideTextStyle(MaterialTheme.typography.body1.copy(fontSize=13.sp,lineHeight=20.sp)){content()}
    }
}
