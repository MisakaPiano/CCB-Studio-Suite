package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.example.chatbar.desktop.*
import io.github.misakapiano.ccbstudio.core.DisguiseCover

@Composable internal fun DisguiseCoverControls(c:WorkbenchController) {
    val a=c.animation;val state=a.coverState
    a.coverError?.let{Text(it,color=MaterialTheme.colors.error);StudioAction("重新读取封面配置",!c.busy){a.reloadCovers()}}
    if(state==null)return
    val selected=state.covers.firstOrNull{it.id==state.selected}
    Surface(color=Panel,shape=RoundedCornerShape(10.dp),border=BorderStroke(1.dp,DesktopBootstrapColors.border)) {
        Row(Modifier.fillMaxWidth().padding(10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            CoverThumbnail(c,selected,Modifier.size(56.dp).clip(RoundedCornerShape(6.dp)))
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)) {
                Text("当前封面 · ${selected?.name?:"CCB 默认封面"}",maxLines=1,overflow=TextOverflow.Ellipsis)
                Text("已保存 ${state.covers.size} 套自定义封面 · 切换后自动更新预览",style=MaterialTheme.typography.caption,color=Quiet)
            }
            StudioAction("更换封面",!c.busy){a.openCoverManager()}
        }
    }
}

@Composable internal fun DisguiseCoverManagerWindow(c:WorkbenchController) {
    val a=c.animation
    if(!a.coverManagerOpen || a.ids==null || c.maintenance)return
    Window(onCloseRequest=a::closeCoverManager,title="CCB Studio Suite · 伪装封面库",icon=StudioBrand.painter,
        resizable=true,state=rememberWindowState(width=1000.dp,height=740.dp)) {
        SideEffect{window.minimumSize=java.awt.Dimension(760,560)}
        MaterialTheme(colors=darkColors(primary=Gold,background=Ink,surface=Panel,onPrimary=Ink,onSurface=DesktopBootstrapColors.foreground)) {
            DisguiseCoverManagerContent(c,a::closeCoverManager){
                a.addCover(""){StudioFileDialogs.openOwned(window,"添加伪装封面",listOf("png","jpg","jpeg","webp","bmp"))}
            }
        }
    }
}

@Composable internal fun CoverThumbnail(c:WorkbenchController,cover:DisguiseCover?,modifier:Modifier) {
    Box(modifier.background(Color(0xff191b20)),contentAlignment=Alignment.Center) {
        if(cover==null)Image(StudioBrand.painter,"CCB 默认封面缩略图",Modifier.fillMaxSize(.34f))
        else AssetImage(c,cover.id,Modifier.fillMaxSize().semantics{contentDescription="封面缩略图 ${cover.id}"})
    }
}

internal fun studioVisibleScrollbarStyle()=ScrollbarStyle(24.dp,10.dp,RoundedCornerShape(5.dp),0,
    DesktopBootstrapColors.border,Gold)

@Composable internal fun DisguiseCoverManagerContent(c:WorkbenchController,onClose:()->Unit,onAdd:()->Unit={c.animation.addCover("")}) {
    val a=c.animation;val state=a.coverState
    var query by remember{mutableStateOf("")}
    var rename by remember{mutableStateOf<DisguiseCover?>(null)}
    var name by remember{mutableStateOf("")}
    var remove by remember{mutableStateOf<DisguiseCover?>(null)}
    val selected=state?.covers?.firstOrNull{it.id==state.selected}
    Surface(color=Ink,contentColor=DesktopBootstrapColors.foreground,modifier=Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("伪装封面库",style=MaterialTheme.typography.h5,fontWeight=FontWeight.SemiBold)
                    Text("保存多套封面，按需切换使用",color=Quiet,style=MaterialTheme.typography.body2)
                }
                StudioAction("添加封面",!c.busy && state!=null && state.covers.size<64,style=StudioActionStyle.PRIMARY,onClick=onAdd)
            }
            if(state==null){Text(a.coverError?:"封面库暂不可用",color=MaterialTheme.colors.error);StudioAction("关闭",onClick=onClose);return@Column}
            OutlinedTextField(query,{query=it},singleLine=true,label={Text("搜索封面名称")},modifier=Modifier.fillMaxWidth())
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val grid=rememberLazyGridState()
                val matches=listOf<DisguiseCover?>(null)+state.covers
                val visible=matches.filter{(it?.name?:"CCB 默认封面").contains(query,true)}
                Row(Modifier.fillMaxSize(),horizontalArrangement=Arrangement.spacedBy(20.dp)) {
                    Box(Modifier.weight(1f).fillMaxHeight()) {
                        LazyVerticalGrid(GridCells.Adaptive(170.dp),Modifier.fillMaxSize().padding(end=16.dp),state=grid,
                            horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                            items(visible,key={it?.id?:"CCB_DEFAULT"}){cover->
                                val active=state.selected==cover?.id
                                Surface(color=if(active)DesktopBootstrapColors.accent else Panel,shape=RoundedCornerShape(10.dp),
                                    border=BorderStroke(if(active)2.dp else 1.dp,if(active)Gold else DesktopBootstrapColors.border),
                                    modifier=Modifier.fillMaxWidth().semantics{contentDescription="选择封面 ${cover?.name?:"CCB 默认封面"}";this.selected=active}
                                        .clickable(enabled=!c.busy){a.selectCover(cover?.id)}) {
                                    Column(Modifier.padding(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                                        Box {
                                            CoverThumbnail(c,cover,Modifier.fillMaxWidth().height(138.dp).clip(RoundedCornerShape(6.dp)))
                                            if(active)Text("✓ 使用中",Modifier.align(Alignment.TopEnd).background(Gold,RoundedCornerShape(4.dp)).padding(horizontal=7.dp,vertical=3.dp),color=Ink,style=MaterialTheme.typography.caption)
                                        }
                                        Text(cover?.name?:"CCB 默认封面",maxLines=2,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.body2)
                                        Text(if(cover==null)"内置封面"else"自定义封面",color=Quiet,style=MaterialTheme.typography.caption)
                                    }
                                }
                            }
                        }
                        if(visible.isEmpty())Text("没有匹配的封面",Modifier.align(Alignment.Center),color=Quiet)
                        VerticalScrollbar(rememberScrollbarAdapter(grid),Modifier.align(Alignment.CenterEnd).fillMaxHeight().semantics{contentDescription="封面库滚动条"},style=studioVisibleScrollbarStyle())
                    }
                    Column(Modifier.width(240.dp).fillMaxHeight().background(Panel,RoundedCornerShape(10.dp)).padding(16.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                        Text("正在使用",color=Gold,style=MaterialTheme.typography.caption)
                        CoverThumbnail(c,selected,Modifier.fillMaxWidth().height(175.dp).clip(RoundedCornerShape(6.dp)))
                        Text(selected?.name?:"CCB 默认封面",fontWeight=FontWeight.Medium,maxLines=3,overflow=TextOverflow.Ellipsis)
                        Text("封面会等比适应图片，空白区域以深色补齐。",color=Quiet,style=MaterialTheme.typography.caption)
                        if(rename!=null) {
                            Text("重命名封面",style=MaterialTheme.typography.subtitle2)
                            OutlinedTextField(name,{name=it.take(100)},singleLine=true,label={Text("新名称")},modifier=Modifier.fillMaxWidth().semantics{contentDescription="封面重命名输入"})
                            StudioAction("保存名称",!c.busy && name.isNotBlank(),style=StudioActionStyle.PRIMARY){a.renameCover(rename!!.id,name);rename=null}
                            StudioAction("取消改名"){rename=null}
                        } else if(remove!=null) {
                            Text("移除这套封面？",color=DesktopBootstrapColors.warning)
                            Text(remove!!.name,maxLines=3,overflow=TextOverflow.Ellipsis)
                            Text("仅从封面库移除，原始文件和已导出的图片保留。",color=Quiet,style=MaterialTheme.typography.caption)
                            StudioAction("确认移除",!c.busy){a.removeCover(remove!!.id);remove=null}
                            StudioAction("取消移除"){remove=null}
                        } else if(selected!=null) {
                            StudioAction("重命名",!c.busy){rename=selected;name=selected.name}
                            StudioAction("移除封面",!c.busy){remove=selected}
                        }
                        Spacer(Modifier.height(8.dp))
                        StudioAction("恢复默认封面",!c.busy && selected!=null){a.selectCover(null)}
                        Text("恢复默认会保留所有自定义封面。",color=Quiet,style=MaterialTheme.typography.caption)
                    }
                }
            }
            Divider(color=DesktopBootstrapColors.border)
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Text("${state.covers.size} / 64 套自定义 · 点击卡片即选用，预览自动更新",Modifier.weight(1f),color=Quiet,style=MaterialTheme.typography.caption)
                StudioAction("完成",style=StudioActionStyle.PRIMARY,onClick=onClose)
            }
        }
    }
}
