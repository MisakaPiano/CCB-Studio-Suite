package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable internal fun StudioPageHelp(page:String,dismiss:()->Unit) {
    val steps=when(page) {
        "图库"->listOf("导入图片后，单击选择，双击查看大图；相册卡片单击进入。","Ctrl 多选、Shift 连选；下方磁贴对所选图片操作。","高级筛选与标签可展开；收起不会取消筛选，清除筛选可恢复范围。","移入回收站需要确认；右侧回收站可恢复。外部原图保留。","目录反复检查时，可到资产管理 → 目录入口暂停自动收集；仍可手动同步。")
        "编辑"->listOf("从图库选择图片进入编辑，或从工程菜单打开已有工程。","当前图是画布显示的图片；所选N张是编辑队列勾选的图片，两者可不同。","自动打码直接选择当前图或所选N张，每次最多100张；AI打码设置只调整识别与效果。","检查检测框，可调整或撤销。保存工程保留可编辑内容；导出才生成处理后的图片。","原图不会被打码覆盖；队列、画布、属性在窄窗口通过页签切换。")
        "资产管理"->listOf("选择要管理的资产类型或目录入口。","目录入口是自动收集设置；可暂停自动收集或手动同步单个目录。","回收站可恢复已移出的资产；现有工程和原图保留。","备份与恢复请先核对目标和回执，勿覆盖现有资料。")
        "账户"->listOf("选择账户并配置本应用使用的凭据。","保存或放弃账户修改后，再切换、退出或更新。","刷新额度只查看状态；未提供的额度保持未知，不代表无限。")
        "更新"->listOf("先检查当前渠道的可信更新。","发现更新后自行确认下载，下载并校验完成不代表已安装。","安装并重启前核对工作区；等待保存与排水，不要强制结束程序。")
        "AI 设计"->listOf("先选择设计目标并填写内容。","检查生成的Prompt和参数，再明确应用到创作。","应用设计不会自动发送生成请求；原有创作内容请先核对。")
        else->listOf("填写正向和负向Prompt，检查模型、尺寸、数量与账户。","需要复用图片参数时，先检查可复用内容及作用范围。","确认后手动生成；右侧预览用于查看结果。","需要编辑图片时，从图库进入编辑；不同页面的选择范围分别管理。")
    }
    AlertDialog(onDismissRequest=dismiss,title={Text("$page · 使用说明")},text={
        Column(Modifier.widthIn(max=560.dp).heightIn(max=500.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            steps.forEachIndexed{i,text->Text("${i+1}. $text")}
            Text("可随时关闭；右上角 ? 可重新打开。",style=MaterialTheme.typography.caption)
        }
    },confirmButton={TextButton(onClick=dismiss){Text("知道了")}})
}
