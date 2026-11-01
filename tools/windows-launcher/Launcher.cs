using System;
using System.IO;
using System.Diagnostics;
using System.Runtime.InteropServices;
using System.Security.Cryptography;
using System.Text;
using System.Web.Script.Serialization;
using System.Collections.Generic;
using System.Text.RegularExpressions;
using System.Windows.Forms;

// Small, version-independent shell entry. Selection is committed only after update verification.
static class Launcher {
    const string AppId="MisakaPiano.CCBStudioSuite.Preview";
    static string Self {get{return System.Reflection.Assembly.GetExecutingAssembly().Location;}}
    static JavaScriptSerializer Json=new JavaScriptSerializer();
    static string Quote(string s){if(s.IndexOf('"')>=0)throw new IOException("Unsupported quoted path");return "\""+s.TrimEnd('\\')+new string('\\',(s.Length-s.TrimEnd('\\').Length)*2)+"\"";}
    static string Digest(string p){using(var a=SHA256.Create())using(var f=File.OpenRead(p))return BitConverter.ToString(a.ComputeHash(f)).Replace("-","").ToLowerInvariant();}
    static void Safe(string p){for(var d=new DirectoryInfo(Path.GetDirectoryName(p));d!=null;d=d.Parent)if((d.Attributes&FileAttributes.ReparsePoint)!=0)throw new IOException("Unsupported linked directory");if((File.Exists(p)||Directory.Exists(p))&&(File.GetAttributes(p)&FileAttributes.ReparsePoint)!=0)throw new IOException("Unsupported linked file");}
    static bool Same(string a,string b){return String.Equals(Path.GetFullPath(a).TrimEnd('\\'),Path.GetFullPath(b).TrimEnd('\\'),StringComparison.OrdinalIgnoreCase);}
    static string Absolute(object value){var s=value as string;if(s==null||!Path.IsPathRooted(s)||s.IndexOfAny(new[]{'\r','\n','\0'})>=0)throw new IOException("Invalid absolute location");return Path.GetFullPath(s);}
    static List<string> Previous(Dictionary<string,object> v){
        var roots=new List<string>();object raw;
        if(v.TryGetValue("previousRoots",out raw)){var items=raw as System.Collections.IEnumerable;if(items==null||raw is string)throw new IOException("Invalid relocation history");foreach(object item in items){var path=Absolute(item);if(!roots.Exists(p=>Same(p,path)))roots.Add(path);if(roots.Count>16)throw new IOException("Relocation history limit reached");}}
        return roots;
    }
    static List<string> ShortcutOrigins(string root){
        // The app may already hold entrypoints.lock while requesting a shortcut.
        // Read the atomic descriptor without taking that lock again in this child.
        string path=Path.Combine(root,"launch-current.json");Safe(path);
        if(new FileInfo(path).Length>16384)throw new IOException("Invalid launch descriptor");
        return Previous(Json.Deserialize<Dictionary<string,object>>(File.ReadAllText(path,Encoding.UTF8)));
    }
    static string Project(string value,string oldRoot,string root){
        string prefix=oldRoot.TrimEnd('\\')+"\\";
        return value.StartsWith(prefix,StringComparison.OrdinalIgnoreCase)?Path.Combine(root,value.Substring(prefix.Length)):value;
    }
    static Dictionary<string,object> Resolve(){
        string root=Path.GetDirectoryName(Self),path=Path.Combine(root,"launch-current.json");Safe(path);
        string gate=Path.Combine(root,"entrypoints.lock");Safe(gate);
        using(var stream=new FileStream(gate,FileMode.OpenOrCreate,FileAccess.ReadWrite,FileShare.ReadWrite)){
            stream.Lock(0,1);try{return ResolveLocked(root,path);}finally{stream.Unlock(0,1);}
        }
    }
    static Dictionary<string,object> ResolveLocked(string root,string path){
        if(new FileInfo(path).Length>16384)throw new IOException("Invalid launch descriptor");
        var v=Json.Deserialize<Dictionary<string,object>>(File.ReadAllText(path,Encoding.UTF8));
        string app=Absolute(v["installed"]),oldRoot=Path.GetDirectoryName(app);bool moved=!Same(oldRoot,root);
        var history=Previous(v);history.RemoveAll(p=>Same(p,root));
        if(moved){
            if(oldRoot.Length<=3||root.StartsWith(oldRoot.TrimEnd('\\')+"\\",StringComparison.OrdinalIgnoreCase)||oldRoot.StartsWith(root.TrimEnd('\\')+"\\",StringComparison.OrdinalIgnoreCase))throw new IOException("Overlapping relocation roots");
            string name=Path.GetFileName(app);if(!Regex.IsMatch(name,@"^[1-9][0-9]*-[0-9a-f]{12}(?:\.repair-[0-9a-f]{32})?$"))throw new IOException("Invalid relocated installation identity");
            app=Path.Combine(root,name);Safe(Path.Combine(app,"update-config.json"));
            if(new FileInfo(Path.Combine(app,"update-config.json")).Length>16384)throw new IOException("Oversized relocated configuration");
            var config=Json.Deserialize<Dictionary<string,object>>(File.ReadAllText(Path.Combine(app,"update-config.json"),Encoding.UTF8));
            string baseName=config["version"]+"-"+config["sourceSha"].ToString().Substring(0,12);
            if(!Object.Equals(config["version"],v["version"])||config["product"].ToString()!="ccb-studio-suite"||config["platform"].ToString()!="windows-x86_64"||!Regex.IsMatch(name,"^"+Regex.Escape(baseName)+@"(?:\.repair-[0-9a-f]{32})?$"))throw new IOException("Relocated target identity mismatch");
            string complete=Path.Combine(app,"INSTALL_COMPLETE"),manifest=Path.Combine(app,"INSTALL_MANIFEST.json"),bundled=Path.Combine(app,"updater","CCBStudioLauncher.exe");Safe(complete);Safe(manifest);Safe(bundled);
            if(File.ReadAllText(complete,Encoding.UTF8)!=config["version"]+":"+config["sourceSha"]||Digest(bundled)!=Digest(Self))throw new IOException("Relocated program proof mismatch");
            if(new FileInfo(manifest).Length>4*1024*1024)throw new IOException("Oversized relocated inventory");
            var inventory=Json.Deserialize<Dictionary<string,object>>(File.ReadAllText(manifest,Encoding.UTF8));
            if(!Object.Equals(inventory["version"],config["version"])||inventory["sourceSha"].ToString()!=config["sourceSha"].ToString())throw new IOException("Relocated manifest identity mismatch");
            foreach(string k in new[]{"data","runtime"}){var destination=Project(Absolute(v[k]),oldRoot,root);Safe(destination);if(!Directory.Exists(destination))throw new IOException("Moved profile is missing; original location retained");v[k]=destination;}
            if(!history.Exists(p=>Same(p,oldRoot)))history.Add(oldRoot);
            if(history.Count>16)throw new IOException("Relocation history limit reached");
            v["installed"]=app;v["previousRoots"]=history;v["locationRoot"]=root;
        }
        string exe=Path.Combine(app,"CCBStudioSuitePreview.exe");
        Safe(exe);if(!String.Equals(Digest(exe),(string)v["exeSha256"],StringComparison.Ordinal))throw new IOException("Launch target has changed");
        foreach(string k in new[]{"data","runtime"})Absolute(v[k]);
        if(moved){string stage=path+"."+Guid.NewGuid().ToString("N")+".tmp";try{File.WriteAllText(stage,Json.Serialize(v),new UTF8Encoding(false));File.Replace(stage,path,null);}finally{if(File.Exists(stage))File.Delete(stage);}}
        return v;
    }
    static bool RequestCancellation(string folder){
        string gate=Path.Combine(folder,"publication.lock");Safe(gate);
        using(var stream=new FileStream(gate,FileMode.OpenOrCreate,FileAccess.ReadWrite,FileShare.ReadWrite)){
            stream.Lock(0,1);try{
                string admitted=Path.Combine(folder,"PUBLICATION_ADMITTED.json");Safe(admitted);
                if(File.Exists(admitted))return false;
                string path=Path.Combine(folder,"CANCEL");Safe(path);
                if(!File.Exists(path))using(var f=new FileStream(path,FileMode.CreateNew,FileAccess.Write,FileShare.Read)){}
                return true;
            }finally{stream.Unlock(0,1);}
        }
    }
    static void UpdateStatus(string folder){
        folder=Path.GetFullPath(folder);
        if(!String.Equals(new DirectoryInfo(folder).Parent.Name,"handoffs",StringComparison.OrdinalIgnoreCase))throw new IOException("Invalid update status location");
        string receipt=Path.Combine(folder,"result.json");Safe(receipt);
        Application.EnableVisualStyles();
        using(var form=new Form()){
            form.Text="Studio 更新";form.Width=510;form.Height=230;form.StartPosition=FormStartPosition.CenterScreen;
            form.FormBorderStyle=FormBorderStyle.FixedDialog;form.MaximizeBox=false;
            var title=new Label{Left=20,Top=18,Width=455,Height=34,Text="正在准备更新…"};
            var detail=new Label{Left=20,Top=58,Width=455,Height=64,Text="验证完成后会自动打开 Studio。现有资料会保留。"};
            var hide=new Button{Left=345,Top=140,Width=125,Text="隐藏提示"};
            var cancel=new Button{Left=205,Top=140,Width=125,Text="取消等待",Enabled=false};
            bool cancelRequested=false;
            hide.Click+=(o,e)=>form.Close();
            cancel.Click+=(o,e)=>{try{
                if(!RequestCancellation(folder)){cancel.Enabled=false;detail.Text="已进入入口提交阶段，无法再取消。不会强制终止程序。";return;}
                cancelRequested=true;cancel.Enabled=false;detail.Text="已请求取消，等待更新器确认。不会强制终止程序，已有资料保留。";
            }catch(IOException){detail.Text="暂未接受取消请求，请稍后重试或查看更新状态。";}};
            form.Controls.AddRange(new Control[]{title,detail,hide,cancel});
            using(var timer=new Timer{Interval=500}){
                timer.Tick+=(o,e)=>{try{
                    if(!File.Exists(receipt))return;
                    Safe(receipt);if(new FileInfo(receipt).Length>65536)throw new IOException("Oversized update status");
                    var value=Json.Deserialize<Dictionary<string,object>>(File.ReadAllText(receipt,Encoding.UTF8));
                    string state=Convert.ToString(value["status"]);cancel.Enabled=!cancelRequested&&(state=="READY_TO_DRAIN"||state=="VERIFYING_B");
                    if(state=="READY_TO_DRAIN"){title.Text="正在保存并关闭旧版本…";detail.Text="请等待资料写入和后台任务安全结束。";}
                    else if(state=="INSTALLING"){title.Text="正在安装程序…";detail.Text="程序文件安装到独立目录，原资料保持原位。";}
                    else if(state=="STARTING_B"||state=="VERIFYING_B"){
                        title.Text="正在启动新版本并验证资料…";
                        object elapsed;value.TryGetValue("verificationElapsedSeconds",out elapsed);
                        detail.Text="验证可能需要几分钟，尚未完成时不会切换启动入口。"+(elapsed==null?"":"\r\n已等待 "+Convert.ToString(elapsed)+" 秒。");
                    }else if(state=="COMMITTING"){title.Text="正在提交启动入口…";detail.Text="验证已完成，已进入提交阶段，无法再取消。";cancel.Enabled=false;}
                    else if(state=="COMPLETE"||state=="COMPLETE_RECEIPT_INCOMPLETE"||state=="COMPLETE_RECEIPT_RECOVERED"){title.Text="更新入口已提交";detail.Text=state=="COMPLETE"?"新版本已完成验证，固定启动入口已更新。":state=="COMPLETE_RECEIPT_RECOVERED"?"已核对先前提交的入口并恢复状态回执，未再次切换入口。":"启动入口已更新，但状态回执未完整保存。请保留回执目录。";hide.Text="关闭提示";cancel.Enabled=false;}
                    else if(state=="CANCELLED_RETAINED"){title.Text="已取消更新等待";detail.Text="启动入口未切换。已有程序和资料保留，没有强制终止程序。";hide.Text="关闭提示";cancel.Enabled=false;}
                    else if(state=="ENTRYPOINT_RECOVERY_REQUIRED"){title.Text="启动入口需要恢复";detail.Text="入口提交未完整完成，请保留回执并使用可信安装包修复，不要删除程序或资料。";hide.Text="关闭提示";cancel.Enabled=false;}
                    else if(state=="FAILED_RETAINED"||state=="REPAIR_REQUIRED"){title.Text="更新未完成，现有资料已保留";detail.Text="请重新打开 Studio 查看更新。若程序无法打开，请使用新安装包的“修复已有程序”。";hide.Text="关闭提示";cancel.Enabled=false;}
                    if(cancelRequested&&(state=="READY_TO_DRAIN"||state=="VERIFYING_B"))detail.Text="已请求取消，等待更新器确认。已有资料保留。";
                }catch(Exception){title.Text="暂时无法读取更新状态";detail.Text="请保留现有资料，稍后重新打开 Studio 查看。";}};
                timer.Start();Application.Run(form);
            }
        }
    }
    [STAThread] static int Main(string[] args){try{
        if(args.Length==2&&args[0]=="--update-status"){UpdateStatus(args[1]);return 0;}
        if(args.Length==2&&args[0]=="--shortcut"){Shortcut(args[1]);return 0;}
        if(args.Length==4&&args[0]=="--prepare-shortcut"){
            string stage=Absolute(args[1]),target=Absolute(args[2]),existing=Absolute(args[3]);
            if(Path.GetFileName(target)!="CCBStudioLauncher.exe"||!Same(Path.GetDirectoryName(existing),Path.GetDirectoryName(target))||!Same(Directory.GetParent(Path.GetDirectoryName(stage)).FullName,Path.GetDirectoryName(target))||!Path.GetFileName(Path.GetDirectoryName(stage)).StartsWith(".entrypoint-",StringComparison.Ordinal))throw new IOException("Invalid shortcut preparation locations");
            Shortcut(stage,target,existing);return 0;
        }
        if(args.Length==3&&args[0]=="--window"){Window(new IntPtr(long.Parse(args[1])),args[2]);return 0;}
        if(args.Length==3&&args[0]=="--inspect-window"){InspectWindow(new IntPtr(long.Parse(args[1])),args[2]);return 0;}
        if(args.Length==3&&args[0]=="--inspect-shortcut"){InspectShortcut(args[1],args[2]);return 0;}
        var v=Resolve();
        if(args.Length==2&&args[0]=="--resolve-report"){File.WriteAllText(args[1],Json.Serialize(v),Encoding.UTF8);return 0;}
        if(args.Length!=0)throw new IOException("Unsupported launch arguments");
        var p=new ProcessStartInfo(Path.Combine((string)v["installed"],"CCBStudioSuitePreview.exe"),"--studio-root "+Quote((string)v["data"])+" --runtime-root "+Quote((string)v["runtime"]));
        p.UseShellExecute=false;p.WorkingDirectory=(string)v["installed"];Process.Start(p);return 0;
    }catch(Exception e){if(args.Length==2&&args[0]=="--resolve-report"){File.WriteAllText(args[1],Json.Serialize(new{error=e.Message}),Encoding.UTF8);return 1;}if(args.Length!=0){Console.Error.WriteLine(e.Message);return 1;}MessageBox.Show("无法打开 CCB Studio Suite。请通过已验证安装目录检查更新入口。\n"+e.Message,"CCB Studio Suite",MessageBoxButtons.OK,MessageBoxIcon.Error);return 1;}}
    static Guid PropertySet=new Guid("9F4C2855-9F79-4B39-A8D0-E1D42DE1D5F3");
    [StructLayout(LayoutKind.Sequential)] struct PropertyKey {public Guid fmtid;public uint pid;public PropertyKey(uint n){fmtid=PropertySet;pid=n;}}
    [StructLayout(LayoutKind.Explicit,Size=24)] struct PropVariant {[FieldOffset(0)]public ushort type;[FieldOffset(8)]public IntPtr value;}
    static void Set(IPropertyStore store,uint id,string value){var k=new PropertyKey(id);var p=new PropVariant{type=31,value=Marshal.StringToCoTaskMemUni(value)};try{store.SetValue(ref k,ref p);}finally{Marshal.FreeCoTaskMem(p.value);}}
    static void Properties(IPropertyStore s,string launcher){Set(s,2,"CCB Studio Suite");Set(s,3,launcher+",0");Set(s,1,Quote(launcher));Set(s,5,AppId);}
    static string Read(IPropertyStore s,uint id){var k=new PropertyKey(id);PropVariant p;s.GetValue(ref k,out p);try{return p.type==31?Marshal.PtrToStringUni(p.value):"";}finally{if(p.type==31)Marshal.FreeCoTaskMem(p.value);}}
    static void InspectShortcut(string file,string report){var link=(IShellLinkW)new ShellLink();try{((System.Runtime.InteropServices.ComTypes.IPersistFile)link).Load(file,0);var target=new StringBuilder(32768);link.GetPath(target,target.Capacity,IntPtr.Zero,0);File.WriteAllText(report,Json.Serialize(new{target=target.ToString(),appId=Read((IPropertyStore)link,5)}),Encoding.UTF8);}finally{Marshal.FinalReleaseComObject(link);}}
    static void Shortcut(string destination,string target=null,string existing=null){
        target=target??Self;existing=existing??destination;Safe(target);Safe(existing);
        if(!Path.IsPathRooted(destination)||!destination.EndsWith(".lnk",StringComparison.OrdinalIgnoreCase))throw new IOException("Absolute shortcut path required");
        Safe(destination);string stage=destination+"."+Guid.NewGuid().ToString("N")+".tmp.lnk";
        if(File.Exists(existing)){var priorLink=(IShellLinkW)new ShellLink();try{((System.Runtime.InteropServices.ComTypes.IPersistFile)priorLink).Load(existing,0);var old=new StringBuilder(32768);priorLink.GetPath(old,old.Capacity,IntPtr.Zero,0);string prior=old.ToString(),root=Path.GetDirectoryName(target);bool own=String.Equals(prior,target,StringComparison.OrdinalIgnoreCase)||(String.Equals(Path.GetFileName(prior),"CCBStudioSuitePreview.exe",StringComparison.OrdinalIgnoreCase)&&String.Equals(Path.GetDirectoryName(Path.GetDirectoryName(prior)),root,StringComparison.OrdinalIgnoreCase));if(!own&&Path.GetFileName(prior).Equals("CCBStudioLauncher.exe",StringComparison.OrdinalIgnoreCase)&&Read((IPropertyStore)priorLink,5)==AppId)own=ShortcutOrigins(root).Exists(p=>Same(prior,Path.Combine(p,"CCBStudioLauncher.exe")));if(!own)throw new IOException("同名快捷方式属于其他入口，请保留并检查");}finally{Marshal.FinalReleaseComObject(priorLink);}}
        var link=(IShellLinkW)new ShellLink();try{
            link.SetPath(target);link.SetRelativePath(target,0);link.SetWorkingDirectory(Path.GetDirectoryName(target));link.SetDescription("CCB Studio Suite");link.SetIconLocation(target,0);
            var store=(IPropertyStore)link;Properties(store,target);store.Commit();((System.Runtime.InteropServices.ComTypes.IPersistFile)link).Save(stage,true);
        }finally{Marshal.FinalReleaseComObject(link);}
        try{if(File.Exists(destination))File.Replace(stage,destination,null);else File.Move(stage,destination);}finally{if(File.Exists(stage))File.Delete(stage);}
    }
    [DllImport("shell32.dll")]static extern int SHGetPropertyStoreForWindow(IntPtr hwnd,ref Guid iid,[MarshalAs(UnmanagedType.Interface)]out IPropertyStore store);
    static void Window(IntPtr hwnd,string launcher){Guid iid=typeof(IPropertyStore).GUID;IPropertyStore store;Marshal.ThrowExceptionForHR(SHGetPropertyStoreForWindow(hwnd,ref iid,out store));try{Properties(store,launcher);store.Commit();}finally{Marshal.FinalReleaseComObject(store);}}
    static void InspectWindow(IntPtr hwnd,string report){Guid iid=typeof(IPropertyStore).GUID;IPropertyStore s;Marshal.ThrowExceptionForHR(SHGetPropertyStoreForWindow(hwnd,ref iid,out s));try{File.WriteAllText(report,Json.Serialize(new{appId=Read(s,5),command=Read(s,1),icon=Read(s,3)}),Encoding.UTF8);}finally{Marshal.FinalReleaseComObject(s);}}
    [ComImport,Guid("00021401-0000-0000-C000-000000000046")]class ShellLink{}
    [ComImport,Guid("886D8EEB-8CF2-4446-8D02-CDBA1DBDCF99"),InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]interface IPropertyStore {
        void GetCount(out uint count);void GetAt(uint index,out PropertyKey key);void GetValue(ref PropertyKey key,out PropVariant value);void SetValue(ref PropertyKey key,ref PropVariant value);void Commit();
    }
    [ComImport,Guid("000214F9-0000-0000-C000-000000000046"),InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]interface IShellLinkW {
        void GetPath([Out,MarshalAs(UnmanagedType.LPWStr)]StringBuilder file,int max,IntPtr data,uint flags);
        void GetIDList(out IntPtr p);void SetIDList(IntPtr p);void GetDescription([Out,MarshalAs(UnmanagedType.LPWStr)]StringBuilder s,int max);void SetDescription([MarshalAs(UnmanagedType.LPWStr)]string s);
        void GetWorkingDirectory([Out,MarshalAs(UnmanagedType.LPWStr)]StringBuilder s,int max);void SetWorkingDirectory([MarshalAs(UnmanagedType.LPWStr)]string s);
        void GetArguments([Out,MarshalAs(UnmanagedType.LPWStr)]StringBuilder s,int max);void SetArguments([MarshalAs(UnmanagedType.LPWStr)]string s);
        void GetHotkey(out short k);void SetHotkey(short k);void GetShowCmd(out int n);void SetShowCmd(int n);void GetIconLocation([Out,MarshalAs(UnmanagedType.LPWStr)]StringBuilder s,int max,out int i);void SetIconLocation([MarshalAs(UnmanagedType.LPWStr)]string s,int i);
        void SetRelativePath([MarshalAs(UnmanagedType.LPWStr)]string s,uint r);void Resolve(IntPtr h,uint flags);void SetPath([MarshalAs(UnmanagedType.LPWStr)]string s);
    }
}
