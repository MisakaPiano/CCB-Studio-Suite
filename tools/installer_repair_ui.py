"""NSIS mode selection for fresh installation or retained-original program repair."""
def header():
    return [
        '!include "nsDialogs.nsh"','!include "LogicLib.nsh"','!include "FileFunc.nsh"',
        'Var RepairMode','Var RepairTarget','Var RepairSeed','Var InstallRadio','Var RepairRadio',
        'Function .onInit','StrCpy $RepairMode 0','${GetParameters} $0','ClearErrors',
        '${GetOptions} $0 "/REPAIR" $1','IfErrors mode_init_done','StrCpy $RepairMode 1','mode_init_done:','FunctionEnd',
        'Function SelectInstallMode','IfSilent mode_page_skip','nsDialogs::Create 1018','Pop $0',
        '${NSD_CreateLabel} 0 0 100% 42u "安装新版本，或修复无法打开的程序。修复会保留原程序和资料，不修复资料内容。"',
        'Pop $0','${NSD_CreateRadioButton} 0 48u 100% 18u "安装到新目录"','Pop $InstallRadio',
        '${NSD_CreateRadioButton} 0 74u 100% 18u "修复已有程序（保留资料）"','Pop $RepairRadio',
        '${If} $RepairMode == 1','${NSD_Check} $RepairRadio','${Else}','${NSD_Check} $InstallRadio','${EndIf}',
        'nsDialogs::Show','Return','mode_page_skip:','Abort','FunctionEnd',
        'Function SelectInstallModeLeave','${NSD_GetState} $RepairRadio $RepairMode','FunctionEnd',
        'Page custom SelectInstallMode SelectInstallModeLeave',
        'DirText "安装请选择新目录；修复请选择当前程序所在目录，不要选择资料文件夹。"']
def prepare():
    return [
        '${If} $RepairMode == 1','StrCpy $RepairTarget "$INSTDIR"',
        'GetTempFileName $RepairSeed "$TEMP"','IfErrors failed',
        'StrCpy $INSTDIR "$RepairSeed-payload"','${EndIf}']
def finish():
    command='ExecWait \'"$INSTDIR\\updater\\pythonw.exe" -B "$INSTDIR\\updater\\lib\\repair_install.py" --package "$INSTDIR" --target "$RepairTarget"\' $0'
    return [
        '${If} $RepairMode == 1','ClearErrors',command,'IfErrors failed',
        'IntCmp $0 0 repair_completed failed failed','repair_completed:',
        'IfSilent repair_notice_done',
        'MessageBox MB_OK "程序文件已修复，原程序和资料已保留。请从固定入口打开 Studio。资料内容未由安装器修改或修复。"',
        'repair_notice_done:','${EndIf}']
