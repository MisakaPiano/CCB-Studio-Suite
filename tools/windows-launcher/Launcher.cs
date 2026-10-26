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
            string name=Path.GetFileName(app);if(!Regex.IsMatch(name,@"^[1-9][0-9]*-[0-9a-f]{12}$"))throw new IOException("Invalid relocated installation identity");
            app=Path.Combine(root,name);Safe(Path.Combine(app,"update-config.json"));
            if(new FileInfo(Path.Combine(app,"update-config.json")).Length>16384)throw new IOException("Oversized relocated configuration");
            var config=Json.Deserialize<Dictionary<string,object>>(File.ReadAllText(Path.Combine(app,"update-config.json"),Encoding.UTF8));
            if(!Object.Equals(config["version"],v["version"])||config["product"].ToString()!="ccb-studio-suite"||config["platform"].ToString()!="windows-x86_64"||name!=config["version"]+"-"+config["sourceSha"].ToString().Substring(0,12))throw new IOException("Relocated target identity mismatch");
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
    [STAThread] static int Main(string[] args){try{
        if(args.Length==2&&args[0]=="--shortcut"){Shortcut(args[1]);return 0;}
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
    static void Shortcut(string destination){
        if(!Path.IsPathRooted(destination)||!destination.EndsWith(".lnk",StringComparison.OrdinalIgnoreCase))throw new IOException("Absolute shortcut path required");
        Safe(destination);string stage=destination+"."+Guid.NewGuid().ToString("N")+".tmp.lnk";
        if(File.Exists(destination)){var priorLink=(IShellLinkW)new ShellLink();try{((System.Runtime.InteropServices.ComTypes.IPersistFile)priorLink).Load(destination,0);var old=new StringBuilder(32768);priorLink.GetPath(old,old.Capacity,IntPtr.Zero,0);string prior=old.ToString(),root=Path.GetDirectoryName(Self);bool own=String.Equals(prior,Self,StringComparison.OrdinalIgnoreCase)||(String.Equals(Path.GetFileName(prior),"CCBStudioSuitePreview.exe",StringComparison.OrdinalIgnoreCase)&&String.Equals(Path.GetDirectoryName(Path.GetDirectoryName(prior)),root,StringComparison.OrdinalIgnoreCase));if(!own&&Path.GetFileName(prior).Equals("CCBStudioLauncher.exe",StringComparison.OrdinalIgnoreCase)&&Read((IPropertyStore)priorLink,5)==AppId)own=ShortcutOrigins(root).Exists(p=>Same(prior,Path.Combine(p,"CCBStudioLauncher.exe")));if(!own)throw new IOException("同名快捷方式属于其他入口，请保留并检查");}finally{Marshal.FinalReleaseComObject(priorLink);}}
        var link=(IShellLinkW)new ShellLink();try{
            link.SetPath(Self);link.SetRelativePath(Self,0);link.SetWorkingDirectory(Path.GetDirectoryName(Self));link.SetDescription("CCB Studio Suite");link.SetIconLocation(Self,0);
            var store=(IPropertyStore)link;Properties(store,Self);store.Commit();((System.Runtime.InteropServices.ComTypes.IPersistFile)link).Save(stage,true);
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
