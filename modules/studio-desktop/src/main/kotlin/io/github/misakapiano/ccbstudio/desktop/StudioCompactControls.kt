package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import com.example.chatbar.desktop.*
import java.time.*

@Composable internal fun StudioCompactField(label:String,value:String,onValue:(String)->Unit,modifier:Modifier=Modifier,singleLine:Boolean=true) {
    BasicTextField(value,onValue,modifier.heightIn(min=32.dp).border(1.dp,DesktopBootstrapColors.border,RoundedCornerShape(6.dp))
        .background(Ink,RoundedCornerShape(6.dp)).padding(horizontal=8.dp,vertical=7.dp).semantics{contentDescription=label},
        singleLine=singleLine,textStyle=TextStyle(color=DesktopBootstrapColors.foreground,fontSize=13.sp),
        cursorBrush=androidx.compose.ui.graphics.SolidColor(Gold),decorationBox={input->Box{if(value.isBlank())Text(if(label.startsWith("日期"))"日期 · YYYY-MM-DD"else label,color=Quiet,fontSize=13.sp,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis);input()}})
}

@Composable internal fun GallerySearch(c:WorkbenchController,modifier:Modifier=Modifier) {
    val g=c.gallery;var expanded by remember{mutableStateOf(false)}
    fun change(value:String){g.query=value;c.selection=emptySet();g.anchor=null}
    Row(modifier,verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)) {
        StudioCompactField("查找文件名或历史 Prompt",g.query,::change,Modifier.weight(1f))
        StudioAction("↗",accessibilityLabel="展开搜索"){expanded=true}
    }
    if(expanded)DialogWindow(onCloseRequest={expanded=false},title="展开搜索",state=rememberDialogState(width=760.dp,height=420.dp)) {
        Surface(color=Ink,contentColor=DesktopBootstrapColors.foreground) {
            GalleryLongSearchContent(g.query,::change){expanded=false}
        }
    }
}

@Composable internal fun GalleryLongSearchContent(value:String,onValue:(String)->Unit,onClose:()->Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text("完整检索内容")
        OutlinedTextField(value,onValue,Modifier.weight(1f).fillMaxWidth(),label={Text("查找文件名或历史 Prompt")})
        StudioAction("完成",onClick=onClose)
    }
}

@Composable internal fun GalleryCalendar(value:String,onPick:(String)->Unit) {
    var open by remember{mutableStateOf(false)}
    var month by remember{mutableStateOf(runCatching{YearMonth.from(LocalDate.parse(value))}.getOrDefault(YearMonth.now()))}
    var yearInput by remember(month){mutableStateOf(month.year.toString())}
    Box {
        StudioAction("日历 ▾"){open=true}
        DropdownMenu(open,{open=false}) {
            Column(Modifier.width(300.dp).padding(8.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                    StudioAction("‹",accessibilityLabel="日历上月"){month=month.minusMonths(1)}
                    StudioCompactField("年份",yearInput,{yearInput=it;it.toIntOrNull()?.takeIf{y->y in 1000..9999}?.let{y->month=month.withYear(y)}},Modifier.width(70.dp))
                    CompactChoice("月",(1..12).toList(),month.monthValue,{it.toString()}){month=month.withMonth(it)}
                    StudioAction("›",accessibilityLabel="日历下月"){month=month.plusMonths(1)}
                }
                Row {listOf("一","二","三","四","五","六","日").forEach{Text(it,Modifier.width(40.dp),color=Quiet)}}
                val offset=month.atDay(1).dayOfWeek.value-1
                repeat((offset+month.lengthOfMonth()+6)/7){week->Row {
                    repeat(7){day->val number=week*7+day-offset+1
                        Box(Modifier.width(40.dp).height(32.dp)) {
                            if(number in 1..month.lengthOfMonth())StudioAction(number.toString(),accessibilityLabel="选择日期 ${month.atDay(number)}"){onPick(month.atDay(number).toString());open=false}
                        }
                    }
                }}
                StudioActions {
                    StudioAction("选择整月"){onPick(month.toString());open=false}
                    StudioAction("选择整年"){onPick(month.year.toString());open=false}
                    StudioAction("今天"){onPick(LocalDate.now().toString());open=false}
                }
            }
        }
    }
}
