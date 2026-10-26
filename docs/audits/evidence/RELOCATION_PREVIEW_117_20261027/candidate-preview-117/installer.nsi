Unicode true
RequestExecutionLevel user
SetCompressor zlib
SetOverwrite off
VIProductVersion "0.2.117.0"
VIAddVersionKey "ProductName" "CCB Studio Suite Preview"
VIAddVersionKey "FileVersion" "0.2.117.0"
Name "CCB Studio Suite Preview"
OutFile "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\studio-installer.exe"
InstallDir "$LOCALAPPDATA\CCBStudioSuite\applications\117-b9b240a248d4"
Page directory
Page instfiles
Section "Install"
StrLen $0 "$INSTDIR"
IntOp $1 $0 + 85
IntCmp $1 259 path_files_ok path_files_ok path_too_long
path_files_ok:
IntOp $1 $0 + 57
IntCmp $1 247 path_lengths_ok path_lengths_ok path_too_long
path_lengths_ok:
IfFileExists "$INSTDIR" occupied
IfFileExists "$INSTDIR\*.*" occupied
SetOutPath "$INSTDIR\app"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\app\.jpackage.xml"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\app\animation-core-desktop-1.10.3-a94bab3d67a320ccdc2cc2c2c973bc4.jar"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\app\animation-desktop-1.10.3-d75724d6eb8bef26a6affbebcbada5.jar"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\app\annotation-jvm-1.9.1-1d6a04b3b9847638d0529df8ef76a.jar"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\app\annotations-23.0.0-8484cd17d040d837983323f760b2c660.jar"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\app\asset-library-ee9417ce5653dbd2beddcf7b8f6462dd.jar"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\app\atomicfu-jvm-0.23.2-5e4f88ed1c37222d791f5dcadf481498.jar"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\app\CCBStudioSuitePreview.cfg"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\app\collection-jvm-1.5.0-4421442c114a4f8c8fe587cf2d668237.jar"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\app\core-common-2.2.0-2212d240dfe1e8a7598ee117ccc316d.jar"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\app\desktop-jvm-1.10.3-98023295d6f48188e5b98b192b9a30.jar"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\app\foundation-desktop-1.10.3-70d7b34e8681a374ed1ed640c3ba8c11.jar"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\app\foundation-layout-desktop-1.10.3-8510263615bf71e2ede4f9451f89f7dd.jar"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\app\icons-lucide-cmp-jvm-2.2.1-41167b6273e4f1763de74dddcba2b6.jar"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\app\icudtl.dat"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\app\image-authority-95f9abde46a167be5c5d2437d288f5.jar"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\app\jbr-api-1.5.0-377e771977886f74954db4278394c51.jar"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\app\jna-5.18.1-cb531ec131e1c68c45b5d45fe5b9878.jar"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\app\jna-platform-5.18.1-a7af0779ec98bfe22dfb07b153283d.jar"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\app\jspecify-1.0.0-9133aba420d0ca3b01dbb6ae9992cf6.jar"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\app\kotlin-stdlib-2.3.20-17212d4ae1611e51ce4c3c4ba28717fa.jar"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\app\kotlin-stdlib-jdk7-2.2.20-b62df9d3ec629ebc7658228fe49529.jar"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\app\kotlin-stdlib-jdk8-2.2.20-c2573686563f7aaca6f7ce98ec9a8b6.jar"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\app\kotlinx-coroutines-core-jvm-1.10.2-16bb58d0777fca3b2efde66862478c.jar"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\app\kotlinx-serialization-core-jvm-1.8.1-ef302ac92198e315d9b084ca66ee5f1e.jar"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\app\kotlinx-serialization-json-jvm-1.8.1-f061d934bcf1d0be1d6c697c320f86b.jar"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\app\lifecycle-common-jvm-2.9.4-6430746dbcfa6c26a0d997b9c76f84c.jar"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\app\lifecycle-runtime-compose-desktop-2.9.4-8f8fe81afd5ce7cf3e8a38142f8aa2.jar"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\app\lifecycle-runtime-compose-desktop-2.9.6-497e8fcabac8a5f5ee68cb2a1c8e70a4.jar"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\app\lifecycle-runtime-desktop-2.9.4-c2c1ed6e1ecd2d037e07bfe64884c5.jar"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\app\lifecycle-viewmodel-desktop-2.9.4-df36d8fe3a9c84b3bd8529b166e73d0.jar"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\app\lifecycle-viewmodel-savedstate-desktop-2.9.4-cf8139ef2d41e155eece234f1b2a358.jar"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\app\material-desktop-1.10.3-8f108d785527e786557d4884fe34a69.jar"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\app\material-ripple-desktop-1.10.3-24324c2656b22bad47558022bfc18265.jar"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\app\msgpack-core-0.9.12-cb664b8c2d992969e89d30fd5a90b5.jar"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\app\navigationevent-desktop-1.0.2-e0291c4f5d6a6b9038dae1d32950806e.jar"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\app\okhttp-4.12.0-6acba053af88fed87e71c6c29911d7c.jar"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\app\okio-jvm-3.6.0-2637180ff99a7e8a12dcaac2a70cc6e.jar"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\app\runtime-annotation-jvm-1.10.5-b09c69fa8ca14488d4f66081b195f82d.jar"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\app\runtime-desktop-1.10.3-c25a54d26562099d86e41defd73c526.jar"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\app\runtime-desktop-1.10.5-b9da3d6a1b578bd2c87d566819b76a65.jar"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\app\runtime-retain-desktop-1.10.5-d8f31750172c21975cc646addacfd80.jar"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\app\runtime-saveable-desktop-1.10.3-5e5be575eafc8b52fe238bc51bdea08e.jar"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\app\runtime-saveable-desktop-1.10.5-db6b23c395f87685dd7a09443777e69.jar"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\app\savedstate-compose-desktop-1.3.3-a131c55f9df3dce54e7daaa9c7f4d17.jar"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\app\savedstate-compose-desktop-1.3.6-8de11644e97845eaa45d9e74fdec755.jar"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\app\savedstate-desktop-1.3.3-c3373bc168f837cab12de655ce867a18.jar"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\app\skiko-awt-0.9.37.4-674c4dff5731dd62c15ac8c1527c8e7.jar"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\app\skiko-awt-runtime-windows-x64-0.9.37.4-1475c01a57ad9d22a52dd19ea5aaf354.jar"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\app\skiko-windows-x64.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\app\skiko-windows-x64.dll.sha256"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\app\sqlite-jdbc-3.53.4.0-f4efbbf74fd258368ebb6529ec099dc.jar"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\app\studio-core-e057314f5d4fa77bae3a8698a81c2b1.jar"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\app\studio-desktop-373ba7498132962667942162e633ce1.jar"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\app\ui-backhandler-desktop-1.10.3-82133368fc2fe04ab219636dc220ee76.jar"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\app\ui-desktop-1.10.3-a87db18fb1c33d7286573c2eb6a58884.jar"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\app\ui-geometry-desktop-1.10.3-74cadef60adf77a87e8bba823806b.jar"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\app\ui-graphics-desktop-1.10.3-b18b35f89c2c5b7d4c839cae550b71f.jar"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\app\ui-text-desktop-1.10.3-f158a42475c13d7691adfa8c41cd7d2.jar"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\app\ui-tooling-preview-desktop-1.10.3-98d4733a7ac1bc0f78fd3bc53ca7966.jar"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\app\ui-unit-desktop-1.10.3-5ba072b3ad846d10195aa20b1dda94a.jar"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\app\ui-util-desktop-1.10.3-e987f56512148332ca73168ad7d270d.jar"
IfErrors failed
SetOutPath "$INSTDIR"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\CCBStudioSuitePreview.exe"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\INSTALL_MANIFEST.json"
IfErrors failed
SetOutPath "$INSTDIR\runtime\bin"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\api-ms-win-core-console-l1-1-0.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\api-ms-win-core-console-l1-2-0.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\api-ms-win-core-datetime-l1-1-0.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\api-ms-win-core-debug-l1-1-0.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\api-ms-win-core-errorhandling-l1-1-0.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\api-ms-win-core-fibers-l1-1-0.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\api-ms-win-core-fibers-l1-1-1.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\api-ms-win-core-file-l1-1-0.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\api-ms-win-core-file-l1-2-0.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\api-ms-win-core-file-l2-1-0.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\api-ms-win-core-handle-l1-1-0.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\api-ms-win-core-heap-l1-1-0.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\api-ms-win-core-interlocked-l1-1-0.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\api-ms-win-core-kernel32-legacy-l1-1-1.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\api-ms-win-core-libraryloader-l1-1-0.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\api-ms-win-core-localization-l1-2-0.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\api-ms-win-core-memory-l1-1-0.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\api-ms-win-core-namedpipe-l1-1-0.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\api-ms-win-core-processenvironment-l1-1-0.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\api-ms-win-core-processthreads-l1-1-0.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\api-ms-win-core-processthreads-l1-1-1.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\api-ms-win-core-profile-l1-1-0.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\api-ms-win-core-rtlsupport-l1-1-0.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\api-ms-win-core-string-l1-1-0.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\api-ms-win-core-synch-l1-1-0.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\api-ms-win-core-synch-l1-2-0.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\api-ms-win-core-sysinfo-l1-1-0.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\api-ms-win-core-sysinfo-l1-2-0.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\api-ms-win-core-timezone-l1-1-0.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\api-ms-win-core-util-l1-1-0.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\api-ms-win-crt-conio-l1-1-0.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\api-ms-win-crt-convert-l1-1-0.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\api-ms-win-crt-environment-l1-1-0.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\api-ms-win-crt-filesystem-l1-1-0.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\api-ms-win-crt-heap-l1-1-0.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\api-ms-win-crt-locale-l1-1-0.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\api-ms-win-crt-math-l1-1-0.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\api-ms-win-crt-multibyte-l1-1-0.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\api-ms-win-crt-private-l1-1-0.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\api-ms-win-crt-process-l1-1-0.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\api-ms-win-crt-runtime-l1-1-0.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\api-ms-win-crt-stdio-l1-1-0.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\api-ms-win-crt-string-l1-1-0.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\api-ms-win-crt-time-l1-1-0.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\api-ms-win-crt-utility-l1-1-0.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\awt.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\fontmanager.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\freetype.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\java.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\javajpeg.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\jawt.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\jimage.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\jli.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\jsound.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\lcms.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\mlib_image.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\msvcp140.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\net.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\nio.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\prefs.dll"
IfErrors failed
SetOutPath "$INSTDIR\runtime\bin\server"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\server\jvm.dll"
IfErrors failed
SetOutPath "$INSTDIR\runtime\bin"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\splashscreen.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\ucrtbase.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\vcruntime140.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\vcruntime140_1.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\verify.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\bin\zip.dll"
IfErrors failed
SetOutPath "$INSTDIR\runtime\conf"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\conf\logging.properties"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\conf\net.properties"
IfErrors failed
SetOutPath "$INSTDIR\runtime\conf\security"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\conf\security\java.policy"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\conf\security\java.security"
IfErrors failed
SetOutPath "$INSTDIR\runtime\conf\security\policy\limited"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\conf\security\policy\limited\default_local.policy"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\conf\security\policy\limited\default_US_export.policy"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\conf\security\policy\limited\exempt_local.policy"
IfErrors failed
SetOutPath "$INSTDIR\runtime\conf\security\policy"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\conf\security\policy\README.txt"
IfErrors failed
SetOutPath "$INSTDIR\runtime\conf\security\policy\unlimited"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\conf\security\policy\unlimited\default_local.policy"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\conf\security\policy\unlimited\default_US_export.policy"
IfErrors failed
SetOutPath "$INSTDIR\runtime\conf"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\conf\sound.properties"
IfErrors failed
SetOutPath "$INSTDIR\runtime\legal\java.base"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\legal\java.base\ADDITIONAL_LICENSE_INFO"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\legal\java.base\aes.md"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\legal\java.base\asm.md"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\legal\java.base\ASSEMBLY_EXCEPTION"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\legal\java.base\c-libutl.md"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\legal\java.base\cldr.md"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\legal\java.base\icu.md"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\legal\java.base\LICENSE"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\legal\java.base\public_suffix.md"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\legal\java.base\siphash.md"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\legal\java.base\unicode.md"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\legal\java.base\wepoll.md"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\legal\java.base\zlib.md"
IfErrors failed
SetOutPath "$INSTDIR\runtime\legal\java.datatransfer"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\legal\java.datatransfer\ADDITIONAL_LICENSE_INFO"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\legal\java.datatransfer\ASSEMBLY_EXCEPTION"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\legal\java.datatransfer\LICENSE"
IfErrors failed
SetOutPath "$INSTDIR\runtime\legal\java.desktop"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\legal\java.desktop\ADDITIONAL_LICENSE_INFO"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\legal\java.desktop\ASSEMBLY_EXCEPTION"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\legal\java.desktop\colorimaging.md"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\legal\java.desktop\freetype.md"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\legal\java.desktop\giflib.md"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\legal\java.desktop\harfbuzz.md"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\legal\java.desktop\jpeg.md"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\legal\java.desktop\lcms.md"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\legal\java.desktop\libpng.md"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\legal\java.desktop\LICENSE"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\legal\java.desktop\mesa3d.md"
IfErrors failed
SetOutPath "$INSTDIR\runtime\legal\java.logging"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\legal\java.logging\ADDITIONAL_LICENSE_INFO"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\legal\java.logging\ASSEMBLY_EXCEPTION"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\legal\java.logging\LICENSE"
IfErrors failed
SetOutPath "$INSTDIR\runtime\legal\java.naming"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\legal\java.naming\ADDITIONAL_LICENSE_INFO"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\legal\java.naming\ASSEMBLY_EXCEPTION"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\legal\java.naming\LICENSE"
IfErrors failed
SetOutPath "$INSTDIR\runtime\legal\java.net.http"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\legal\java.net.http\ADDITIONAL_LICENSE_INFO"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\legal\java.net.http\ASSEMBLY_EXCEPTION"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\legal\java.net.http\LICENSE"
IfErrors failed
SetOutPath "$INSTDIR\runtime\legal\java.prefs"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\legal\java.prefs\ADDITIONAL_LICENSE_INFO"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\legal\java.prefs\ASSEMBLY_EXCEPTION"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\legal\java.prefs\LICENSE"
IfErrors failed
SetOutPath "$INSTDIR\runtime\legal\java.security.sasl"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\legal\java.security.sasl\ADDITIONAL_LICENSE_INFO"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\legal\java.security.sasl\ASSEMBLY_EXCEPTION"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\legal\java.security.sasl\LICENSE"
IfErrors failed
SetOutPath "$INSTDIR\runtime\legal\java.sql"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\legal\java.sql\ADDITIONAL_LICENSE_INFO"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\legal\java.sql\ASSEMBLY_EXCEPTION"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\legal\java.sql\LICENSE"
IfErrors failed
SetOutPath "$INSTDIR\runtime\legal\java.transaction.xa"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\legal\java.transaction.xa\ADDITIONAL_LICENSE_INFO"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\legal\java.transaction.xa\ASSEMBLY_EXCEPTION"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\legal\java.transaction.xa\LICENSE"
IfErrors failed
SetOutPath "$INSTDIR\runtime\legal\java.xml"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\legal\java.xml\ADDITIONAL_LICENSE_INFO"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\legal\java.xml\ASSEMBLY_EXCEPTION"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\legal\java.xml\bcel.md"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\legal\java.xml\dom.md"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\legal\java.xml\jcup.md"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\legal\java.xml\LICENSE"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\legal\java.xml\xalan.md"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\legal\java.xml\xerces.md"
IfErrors failed
SetOutPath "$INSTDIR\runtime\legal\jdk.crypto.ec"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\legal\jdk.crypto.ec\ADDITIONAL_LICENSE_INFO"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\legal\jdk.crypto.ec\ASSEMBLY_EXCEPTION"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\legal\jdk.crypto.ec\LICENSE"
IfErrors failed
SetOutPath "$INSTDIR\runtime\legal\jdk.unsupported"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\legal\jdk.unsupported\ADDITIONAL_LICENSE_INFO"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\legal\jdk.unsupported\ASSEMBLY_EXCEPTION"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\legal\jdk.unsupported\LICENSE"
IfErrors failed
SetOutPath "$INSTDIR\runtime\lib"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\lib\classlist"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\lib\fontconfig.bfc"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\lib\fontconfig.properties.src"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\lib\jawt.lib"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\lib\jrt-fs.jar"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\lib\jvm.cfg"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\lib\jvm.lib"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\lib\modules"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\lib\psfont.properties.ja"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\lib\psfontj2d.properties"
IfErrors failed
SetOutPath "$INSTDIR\runtime\lib\security"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\lib\security\blocked.certs"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\lib\security\cacerts"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\lib\security\default.policy"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\lib\security\public_suffix_list.dat"
IfErrors failed
SetOutPath "$INSTDIR\runtime\lib"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\lib\tzdb.dat"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\lib\tzmappings"
IfErrors failed
SetOutPath "$INSTDIR\runtime"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\runtime\release"
IfErrors failed
SetOutPath "$INSTDIR"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\update-config.json"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\update-root.json"
IfErrors failed
SetOutPath "$INSTDIR\updater"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\_asyncio.pyd"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\_bz2.pyd"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\_ctypes.pyd"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\_decimal.pyd"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\_elementtree.pyd"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\_hashlib.pyd"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\_lzma.pyd"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\_multiprocessing.pyd"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\_overlapped.pyd"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\_queue.pyd"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\_socket.pyd"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\_sqlite3.pyd"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\_ssl.pyd"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\_uuid.pyd"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\_wmi.pyd"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\_zoneinfo.pyd"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\CCBStudioLauncher.exe"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\_cffi_backend.cp313-win_amd64.pyd"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\apply_update.py"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\cffi"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cffi\__init__.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cffi\_cffi_errors.h"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cffi\_cffi_gen_src.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cffi\_cffi_include.h"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cffi\_embedding.h"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cffi\_imp_emulation.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cffi\_shimmed_dist_utils.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cffi\api.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cffi\backend_ctypes.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cffi\cffi_opcode.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cffi\commontypes.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cffi\cparser.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cffi\error.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cffi\ffiplatform.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cffi\gen_src.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cffi\lock.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cffi\model.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cffi\parse_c_type.h"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cffi\pkgconfig.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cffi\recompiler.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cffi\setuptools_ext.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cffi\vengine_cpy.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cffi\vengine_gen.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cffi\verifier.py"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\cffi-2.1.1.dist-info"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cffi-2.1.1.dist-info\entry_points.txt"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\cffi-2.1.1.dist-info\licenses"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cffi-2.1.1.dist-info\licenses\LICENSE"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\cffi-2.1.1.dist-info"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cffi-2.1.1.dist-info\METADATA"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cffi-2.1.1.dist-info\RECORD"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cffi-2.1.1.dist-info\top_level.txt"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cffi-2.1.1.dist-info\WHEEL"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\cryptography"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\__about__.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\__init__.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\cobblestone.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\exceptions.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\fernet.py"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\cryptography\hazmat"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\__init__.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\_oid.py"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\cryptography\hazmat\asn1"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\asn1\__init__.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\asn1\asn1.py"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\cryptography\hazmat\backends"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\backends\__init__.py"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\cryptography\hazmat\backends\openssl"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\backends\openssl\__init__.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\backends\openssl\backend.py"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\cryptography\hazmat\bindings"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\bindings\__init__.py"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\cryptography\hazmat\bindings\_rust"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\bindings\_rust\__init__.pyi"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\bindings\_rust\_openssl.pyi"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\bindings\_rust\asn1.pyi"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\bindings\_rust\cobblestone.pyi"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\bindings\_rust\declarative_asn1.pyi"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\bindings\_rust\exceptions.pyi"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\bindings\_rust\ocsp.pyi"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\cryptography\hazmat\bindings\_rust\openssl"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\bindings\_rust\openssl\__init__.pyi"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\bindings\_rust\openssl\aead.pyi"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\bindings\_rust\openssl\ciphers.pyi"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\bindings\_rust\openssl\cmac.pyi"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\bindings\_rust\openssl\dh.pyi"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\bindings\_rust\openssl\dsa.pyi"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\bindings\_rust\openssl\ec.pyi"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\bindings\_rust\openssl\ed25519.pyi"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\bindings\_rust\openssl\ed448.pyi"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\bindings\_rust\openssl\hashes.pyi"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\bindings\_rust\openssl\hmac.pyi"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\bindings\_rust\openssl\hpke.pyi"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\bindings\_rust\openssl\kdf.pyi"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\bindings\_rust\openssl\keys.pyi"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\bindings\_rust\openssl\keywrap.pyi"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\bindings\_rust\openssl\mldsa.pyi"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\bindings\_rust\openssl\mlkem.pyi"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\bindings\_rust\openssl\poly1305.pyi"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\bindings\_rust\openssl\rsa.pyi"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\bindings\_rust\openssl\x25519.pyi"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\bindings\_rust\openssl\x448.pyi"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\cryptography\hazmat\bindings\_rust"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\bindings\_rust\pkcs12.pyi"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\bindings\_rust\pkcs7.pyi"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\bindings\_rust\test_support.pyi"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\bindings\_rust\x509.pyi"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\cryptography\hazmat\bindings"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\bindings\_rust.pyd"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\cryptography\hazmat\bindings\openssl"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\bindings\openssl\__init__.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\bindings\openssl\_conditional.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\bindings\openssl\binding.py"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\cryptography\hazmat\decrepit"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\decrepit\__init__.py"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\cryptography\hazmat\decrepit\ciphers"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\decrepit\ciphers\__init__.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\decrepit\ciphers\algorithms.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\decrepit\ciphers\modes.py"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\cryptography\hazmat\primitives"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\primitives\__init__.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\primitives\_asymmetric.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\primitives\_cipheralgorithm.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\primitives\_modes.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\primitives\_serialization.py"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\cryptography\hazmat\primitives\asymmetric"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\primitives\asymmetric\__init__.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\primitives\asymmetric\dh.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\primitives\asymmetric\dsa.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\primitives\asymmetric\ec.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\primitives\asymmetric\ed25519.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\primitives\asymmetric\ed448.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\primitives\asymmetric\mldsa.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\primitives\asymmetric\mlkem.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\primitives\asymmetric\padding.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\primitives\asymmetric\rsa.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\primitives\asymmetric\types.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\primitives\asymmetric\utils.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\primitives\asymmetric\x25519.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\primitives\asymmetric\x448.py"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\cryptography\hazmat\primitives\ciphers"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\primitives\ciphers\__init__.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\primitives\ciphers\aead.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\primitives\ciphers\algorithms.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\primitives\ciphers\base.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\primitives\ciphers\modes.py"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\cryptography\hazmat\primitives"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\primitives\cmac.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\primitives\constant_time.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\primitives\hashes.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\primitives\hmac.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\primitives\hpke.py"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\cryptography\hazmat\primitives\kdf"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\primitives\kdf\__init__.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\primitives\kdf\argon2.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\primitives\kdf\concatkdf.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\primitives\kdf\hkdf.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\primitives\kdf\kbkdf.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\primitives\kdf\pbkdf2.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\primitives\kdf\scrypt.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\primitives\kdf\x963kdf.py"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\cryptography\hazmat\primitives"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\primitives\keywrap.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\primitives\padding.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\primitives\poly1305.py"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\cryptography\hazmat\primitives\serialization"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\primitives\serialization\__init__.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\primitives\serialization\base.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\primitives\serialization\pkcs12.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\primitives\serialization\pkcs7.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\primitives\serialization\ssh.py"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\cryptography\hazmat\primitives\twofactor"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\primitives\twofactor\__init__.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\primitives\twofactor\hotp.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\hazmat\primitives\twofactor\totp.py"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\cryptography"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\py.typed"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\utils.py"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\cryptography\x509"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\x509\__init__.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\x509\base.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\x509\certificate_transparency.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\x509\extensions.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\x509\general_name.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\x509\name.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\x509\ocsp.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\x509\oid.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography\x509\verification.py"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\cryptography-50.0.2.dist-info\licenses"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography-50.0.2.dist-info\licenses\LICENSE"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography-50.0.2.dist-info\licenses\LICENSE.APACHE"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography-50.0.2.dist-info\licenses\LICENSE.BSD"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\cryptography-50.0.2.dist-info"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography-50.0.2.dist-info\METADATA"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography-50.0.2.dist-info\RECORD"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\cryptography-50.0.2.dist-info\sboms"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography-50.0.2.dist-info\sboms\cryptography-rust.cyclonedx.json"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography-50.0.2.dist-info\sboms\sbom.json"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\cryptography-50.0.2.dist-info"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\cryptography-50.0.2.dist-info\WHEEL"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\github_transport.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\install_ops.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\owned_files.py"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\pycparser"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\pycparser\__init__.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\pycparser\_ast_gen.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\pycparser\_c_ast.cfg"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\pycparser\ast_transforms.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\pycparser\c_ast.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\pycparser\c_generator.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\pycparser\c_lexer.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\pycparser\c_parser.py"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\pycparser-3.11.dist-info\licenses"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\pycparser-3.11.dist-info\licenses\LICENSE"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\pycparser-3.11.dist-info"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\pycparser-3.11.dist-info\METADATA"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\pycparser-3.11.dist-info\RECORD"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\pycparser-3.11.dist-info\top_level.txt"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\pycparser-3.11.dist-info\WHEEL"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\residue_cleanup.py"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\securesystemslib"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\securesystemslib\__init__.py"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\securesystemslib\_gpg"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\securesystemslib\_gpg\__init__.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\securesystemslib\_gpg\common.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\securesystemslib\_gpg\constants.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\securesystemslib\_gpg\dsa.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\securesystemslib\_gpg\eddsa.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\securesystemslib\_gpg\exceptions.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\securesystemslib\_gpg\functions.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\securesystemslib\_gpg\handlers.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\securesystemslib\_gpg\rsa.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\securesystemslib\_gpg\util.py"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\securesystemslib\_internal"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\securesystemslib\_internal\utils.py"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\securesystemslib\_vendor"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\securesystemslib\_vendor\__init__.py"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\securesystemslib\_vendor\ed25519"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\securesystemslib\_vendor\ed25519\.gitignore"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\securesystemslib\_vendor\ed25519\__init__.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\securesystemslib\_vendor\ed25519\ed25519.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\securesystemslib\_vendor\ed25519\LICENSE"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\securesystemslib\_vendor\ed25519\README.rst"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\securesystemslib\_vendor\ed25519\science.py"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\securesystemslib\_vendor\ed25519\test_data"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\securesystemslib\_vendor\ed25519\test_data\ed25519"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\securesystemslib\_vendor\ed25519"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\securesystemslib\_vendor\ed25519\test_ed25519.py"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\securesystemslib\_vendor"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\securesystemslib\_vendor\README.md"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\securesystemslib\_vendor\test-ed25519-upstream.sh"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\securesystemslib"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\securesystemslib\dsse.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\securesystemslib\exceptions.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\securesystemslib\formats.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\securesystemslib\hash.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\securesystemslib\py.typed"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\securesystemslib\signer"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\securesystemslib\signer\__init__.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\securesystemslib\signer\_aws_signer.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\securesystemslib\signer\_azure_signer.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\securesystemslib\signer\_constants.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\securesystemslib\signer\_crypto_signer.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\securesystemslib\signer\_crypto_utils.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\securesystemslib\signer\_gcp_signer.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\securesystemslib\signer\_gpg_signer.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\securesystemslib\signer\_hsm_signer.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\securesystemslib\signer\_key.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\securesystemslib\signer\_signature.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\securesystemslib\signer\_signer.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\securesystemslib\signer\_sigstore_signer.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\securesystemslib\signer\_tkey_signer.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\securesystemslib\signer\_utils.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\securesystemslib\signer\_vault_signer.py"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\securesystemslib"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\securesystemslib\storage.py"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\securesystemslib-1.6.0.dist-info\licenses"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\securesystemslib-1.6.0.dist-info\licenses\LICENSE"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\securesystemslib-1.6.0.dist-info"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\securesystemslib-1.6.0.dist-info\METADATA"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\securesystemslib-1.6.0.dist-info\RECORD"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\securesystemslib-1.6.0.dist-info\WHEEL"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\studio_update.py"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\tuf"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\tuf\__init__.py"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\tuf\api"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\tuf\api\__init__.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\tuf\api\_payload.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\tuf\api\dsse.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\tuf\api\exceptions.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\tuf\api\metadata.py"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\tuf\api\serialization"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\tuf\api\serialization\__init__.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\tuf\api\serialization\json.py"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\tuf\ngclient"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\tuf\ngclient\__init__.py"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\tuf\ngclient\_internal"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\tuf\ngclient\_internal\__init__.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\tuf\ngclient\_internal\proxy.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\tuf\ngclient\_internal\trusted_metadata_set.py"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\tuf\ngclient"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\tuf\ngclient\config.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\tuf\ngclient\fetcher.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\tuf\ngclient\README.md"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\tuf\ngclient\requests_fetcher.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\tuf\ngclient\updater.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\tuf\ngclient\urllib3_fetcher.py"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\tuf"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\tuf\py.typed"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\tuf\repository"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\tuf\repository\__init__.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\tuf\repository\_repository.py"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\tuf-7.0.1.dist-info\licenses"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\tuf-7.0.1.dist-info\licenses\LICENSE"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\tuf-7.0.1.dist-info\licenses\LICENSE-MIT"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\tuf-7.0.1.dist-info"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\tuf-7.0.1.dist-info\METADATA"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\tuf-7.0.1.dist-info\RECORD"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\tuf-7.0.1.dist-info\WHEEL"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\typing_extensions-4.16.0.dist-info\licenses"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\typing_extensions-4.16.0.dist-info\licenses\LICENSE"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\typing_extensions-4.16.0.dist-info"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\typing_extensions-4.16.0.dist-info\METADATA"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\typing_extensions-4.16.0.dist-info\RECORD"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\typing_extensions-4.16.0.dist-info\WHEEL"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\typing_extensions.py"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\urllib3"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\urllib3\__init__.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\urllib3\_base_connection.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\urllib3\_collections.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\urllib3\_request_methods.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\urllib3\_version.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\urllib3\connection.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\urllib3\connectionpool.py"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\urllib3\contrib"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\urllib3\contrib\__init__.py"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\urllib3\contrib\emscripten"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\urllib3\contrib\emscripten\__init__.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\urllib3\contrib\emscripten\connection.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\urllib3\contrib\emscripten\emscripten_fetch_worker.js"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\urllib3\contrib\emscripten\fetch.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\urllib3\contrib\emscripten\request.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\urllib3\contrib\emscripten\response.py"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\urllib3\contrib"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\urllib3\contrib\pyopenssl.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\urllib3\contrib\socks.py"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\urllib3"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\urllib3\exceptions.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\urllib3\fields.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\urllib3\filepost.py"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\urllib3\http2"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\urllib3\http2\__init__.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\urllib3\http2\connection.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\urllib3\http2\probe.py"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\urllib3"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\urllib3\poolmanager.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\urllib3\py.typed"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\urllib3\response.py"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\urllib3\util"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\urllib3\util\__init__.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\urllib3\util\connection.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\urllib3\util\proxy.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\urllib3\util\request.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\urllib3\util\response.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\urllib3\util\retry.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\urllib3\util\ssl_.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\urllib3\util\ssl_match_hostname.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\urllib3\util\ssltransport.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\urllib3\util\timeout.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\urllib3\util\url.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\urllib3\util\util.py"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\urllib3\util\wait.py"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\urllib3-2.8.0.dist-info\licenses"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\urllib3-2.8.0.dist-info\licenses\LICENSE.txt"
IfErrors failed
SetOutPath "$INSTDIR\updater\lib\urllib3-2.8.0.dist-info"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\urllib3-2.8.0.dist-info\METADATA"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\urllib3-2.8.0.dist-info\RECORD"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\lib\urllib3-2.8.0.dist-info\WHEEL"
IfErrors failed
SetOutPath "$INSTDIR\updater"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\libcrypto-3.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\libffi-8.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\libssl-3.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\LICENSE.txt"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\pyexpat.pyd"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\python.cat"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\python.exe"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\python3.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\python313._pth"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\python313.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\python313.zip"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\pythonw.exe"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\RUNTIME_MANIFEST.json"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\select.pyd"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\sqlite3.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\unicodedata.pyd"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\vcruntime140.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\vcruntime140_1.dll"
IfErrors failed
ClearErrors
File "H:\CCB-Studio-Suite\.local\relocation-117-20261027\candidate-preview-117\payload\updater\winsound.pyd"
IfErrors failed
SetOutPath "$INSTDIR"
WriteUninstaller "$INSTDIR\Uninstall.exe"
IfErrors failed
FileOpen $0 "$INSTDIR\INSTALL_COMPLETE" w
IfErrors failed
FileWrite $0 "117:b9b240a248d428ec158c341b8c127969fd54cb64"
FileClose $0
SetErrorLevel 0
Goto done
occupied:
SetErrorLevel 17
Quit
failed:
SetErrorLevel 18
Quit
path_too_long:
IfSilent +2
MessageBox MB_OK|MB_ICONSTOP "Installation path is too long. Choose a shorter folder. No files were installed."
SetErrorLevel 19
Quit
done:
SectionEnd
UninstPage uninstConfirm
UninstPage instfiles
Section "Uninstall"
Delete "$INSTDIR\app\.jpackage.xml"
Delete "$INSTDIR\app\animation-core-desktop-1.10.3-a94bab3d67a320ccdc2cc2c2c973bc4.jar"
Delete "$INSTDIR\app\animation-desktop-1.10.3-d75724d6eb8bef26a6affbebcbada5.jar"
Delete "$INSTDIR\app\annotation-jvm-1.9.1-1d6a04b3b9847638d0529df8ef76a.jar"
Delete "$INSTDIR\app\annotations-23.0.0-8484cd17d040d837983323f760b2c660.jar"
Delete "$INSTDIR\app\asset-library-ee9417ce5653dbd2beddcf7b8f6462dd.jar"
Delete "$INSTDIR\app\atomicfu-jvm-0.23.2-5e4f88ed1c37222d791f5dcadf481498.jar"
Delete "$INSTDIR\app\CCBStudioSuitePreview.cfg"
Delete "$INSTDIR\app\collection-jvm-1.5.0-4421442c114a4f8c8fe587cf2d668237.jar"
Delete "$INSTDIR\app\core-common-2.2.0-2212d240dfe1e8a7598ee117ccc316d.jar"
Delete "$INSTDIR\app\desktop-jvm-1.10.3-98023295d6f48188e5b98b192b9a30.jar"
Delete "$INSTDIR\app\foundation-desktop-1.10.3-70d7b34e8681a374ed1ed640c3ba8c11.jar"
Delete "$INSTDIR\app\foundation-layout-desktop-1.10.3-8510263615bf71e2ede4f9451f89f7dd.jar"
Delete "$INSTDIR\app\icons-lucide-cmp-jvm-2.2.1-41167b6273e4f1763de74dddcba2b6.jar"
Delete "$INSTDIR\app\icudtl.dat"
Delete "$INSTDIR\app\image-authority-95f9abde46a167be5c5d2437d288f5.jar"
Delete "$INSTDIR\app\jbr-api-1.5.0-377e771977886f74954db4278394c51.jar"
Delete "$INSTDIR\app\jna-5.18.1-cb531ec131e1c68c45b5d45fe5b9878.jar"
Delete "$INSTDIR\app\jna-platform-5.18.1-a7af0779ec98bfe22dfb07b153283d.jar"
Delete "$INSTDIR\app\jspecify-1.0.0-9133aba420d0ca3b01dbb6ae9992cf6.jar"
Delete "$INSTDIR\app\kotlin-stdlib-2.3.20-17212d4ae1611e51ce4c3c4ba28717fa.jar"
Delete "$INSTDIR\app\kotlin-stdlib-jdk7-2.2.20-b62df9d3ec629ebc7658228fe49529.jar"
Delete "$INSTDIR\app\kotlin-stdlib-jdk8-2.2.20-c2573686563f7aaca6f7ce98ec9a8b6.jar"
Delete "$INSTDIR\app\kotlinx-coroutines-core-jvm-1.10.2-16bb58d0777fca3b2efde66862478c.jar"
Delete "$INSTDIR\app\kotlinx-serialization-core-jvm-1.8.1-ef302ac92198e315d9b084ca66ee5f1e.jar"
Delete "$INSTDIR\app\kotlinx-serialization-json-jvm-1.8.1-f061d934bcf1d0be1d6c697c320f86b.jar"
Delete "$INSTDIR\app\lifecycle-common-jvm-2.9.4-6430746dbcfa6c26a0d997b9c76f84c.jar"
Delete "$INSTDIR\app\lifecycle-runtime-compose-desktop-2.9.4-8f8fe81afd5ce7cf3e8a38142f8aa2.jar"
Delete "$INSTDIR\app\lifecycle-runtime-compose-desktop-2.9.6-497e8fcabac8a5f5ee68cb2a1c8e70a4.jar"
Delete "$INSTDIR\app\lifecycle-runtime-desktop-2.9.4-c2c1ed6e1ecd2d037e07bfe64884c5.jar"
Delete "$INSTDIR\app\lifecycle-viewmodel-desktop-2.9.4-df36d8fe3a9c84b3bd8529b166e73d0.jar"
Delete "$INSTDIR\app\lifecycle-viewmodel-savedstate-desktop-2.9.4-cf8139ef2d41e155eece234f1b2a358.jar"
Delete "$INSTDIR\app\material-desktop-1.10.3-8f108d785527e786557d4884fe34a69.jar"
Delete "$INSTDIR\app\material-ripple-desktop-1.10.3-24324c2656b22bad47558022bfc18265.jar"
Delete "$INSTDIR\app\msgpack-core-0.9.12-cb664b8c2d992969e89d30fd5a90b5.jar"
Delete "$INSTDIR\app\navigationevent-desktop-1.0.2-e0291c4f5d6a6b9038dae1d32950806e.jar"
Delete "$INSTDIR\app\okhttp-4.12.0-6acba053af88fed87e71c6c29911d7c.jar"
Delete "$INSTDIR\app\okio-jvm-3.6.0-2637180ff99a7e8a12dcaac2a70cc6e.jar"
Delete "$INSTDIR\app\runtime-annotation-jvm-1.10.5-b09c69fa8ca14488d4f66081b195f82d.jar"
Delete "$INSTDIR\app\runtime-desktop-1.10.3-c25a54d26562099d86e41defd73c526.jar"
Delete "$INSTDIR\app\runtime-desktop-1.10.5-b9da3d6a1b578bd2c87d566819b76a65.jar"
Delete "$INSTDIR\app\runtime-retain-desktop-1.10.5-d8f31750172c21975cc646addacfd80.jar"
Delete "$INSTDIR\app\runtime-saveable-desktop-1.10.3-5e5be575eafc8b52fe238bc51bdea08e.jar"
Delete "$INSTDIR\app\runtime-saveable-desktop-1.10.5-db6b23c395f87685dd7a09443777e69.jar"
Delete "$INSTDIR\app\savedstate-compose-desktop-1.3.3-a131c55f9df3dce54e7daaa9c7f4d17.jar"
Delete "$INSTDIR\app\savedstate-compose-desktop-1.3.6-8de11644e97845eaa45d9e74fdec755.jar"
Delete "$INSTDIR\app\savedstate-desktop-1.3.3-c3373bc168f837cab12de655ce867a18.jar"
Delete "$INSTDIR\app\skiko-awt-0.9.37.4-674c4dff5731dd62c15ac8c1527c8e7.jar"
Delete "$INSTDIR\app\skiko-awt-runtime-windows-x64-0.9.37.4-1475c01a57ad9d22a52dd19ea5aaf354.jar"
Delete "$INSTDIR\app\skiko-windows-x64.dll"
Delete "$INSTDIR\app\skiko-windows-x64.dll.sha256"
Delete "$INSTDIR\app\sqlite-jdbc-3.53.4.0-f4efbbf74fd258368ebb6529ec099dc.jar"
Delete "$INSTDIR\app\studio-core-e057314f5d4fa77bae3a8698a81c2b1.jar"
Delete "$INSTDIR\app\studio-desktop-373ba7498132962667942162e633ce1.jar"
Delete "$INSTDIR\app\ui-backhandler-desktop-1.10.3-82133368fc2fe04ab219636dc220ee76.jar"
Delete "$INSTDIR\app\ui-desktop-1.10.3-a87db18fb1c33d7286573c2eb6a58884.jar"
Delete "$INSTDIR\app\ui-geometry-desktop-1.10.3-74cadef60adf77a87e8bba823806b.jar"
Delete "$INSTDIR\app\ui-graphics-desktop-1.10.3-b18b35f89c2c5b7d4c839cae550b71f.jar"
Delete "$INSTDIR\app\ui-text-desktop-1.10.3-f158a42475c13d7691adfa8c41cd7d2.jar"
Delete "$INSTDIR\app\ui-tooling-preview-desktop-1.10.3-98d4733a7ac1bc0f78fd3bc53ca7966.jar"
Delete "$INSTDIR\app\ui-unit-desktop-1.10.3-5ba072b3ad846d10195aa20b1dda94a.jar"
Delete "$INSTDIR\app\ui-util-desktop-1.10.3-e987f56512148332ca73168ad7d270d.jar"
Delete "$INSTDIR\CCBStudioSuitePreview.exe"
Delete "$INSTDIR\INSTALL_MANIFEST.json"
Delete "$INSTDIR\runtime\bin\api-ms-win-core-console-l1-1-0.dll"
Delete "$INSTDIR\runtime\bin\api-ms-win-core-console-l1-2-0.dll"
Delete "$INSTDIR\runtime\bin\api-ms-win-core-datetime-l1-1-0.dll"
Delete "$INSTDIR\runtime\bin\api-ms-win-core-debug-l1-1-0.dll"
Delete "$INSTDIR\runtime\bin\api-ms-win-core-errorhandling-l1-1-0.dll"
Delete "$INSTDIR\runtime\bin\api-ms-win-core-fibers-l1-1-0.dll"
Delete "$INSTDIR\runtime\bin\api-ms-win-core-fibers-l1-1-1.dll"
Delete "$INSTDIR\runtime\bin\api-ms-win-core-file-l1-1-0.dll"
Delete "$INSTDIR\runtime\bin\api-ms-win-core-file-l1-2-0.dll"
Delete "$INSTDIR\runtime\bin\api-ms-win-core-file-l2-1-0.dll"
Delete "$INSTDIR\runtime\bin\api-ms-win-core-handle-l1-1-0.dll"
Delete "$INSTDIR\runtime\bin\api-ms-win-core-heap-l1-1-0.dll"
Delete "$INSTDIR\runtime\bin\api-ms-win-core-interlocked-l1-1-0.dll"
Delete "$INSTDIR\runtime\bin\api-ms-win-core-kernel32-legacy-l1-1-1.dll"
Delete "$INSTDIR\runtime\bin\api-ms-win-core-libraryloader-l1-1-0.dll"
Delete "$INSTDIR\runtime\bin\api-ms-win-core-localization-l1-2-0.dll"
Delete "$INSTDIR\runtime\bin\api-ms-win-core-memory-l1-1-0.dll"
Delete "$INSTDIR\runtime\bin\api-ms-win-core-namedpipe-l1-1-0.dll"
Delete "$INSTDIR\runtime\bin\api-ms-win-core-processenvironment-l1-1-0.dll"
Delete "$INSTDIR\runtime\bin\api-ms-win-core-processthreads-l1-1-0.dll"
Delete "$INSTDIR\runtime\bin\api-ms-win-core-processthreads-l1-1-1.dll"
Delete "$INSTDIR\runtime\bin\api-ms-win-core-profile-l1-1-0.dll"
Delete "$INSTDIR\runtime\bin\api-ms-win-core-rtlsupport-l1-1-0.dll"
Delete "$INSTDIR\runtime\bin\api-ms-win-core-string-l1-1-0.dll"
Delete "$INSTDIR\runtime\bin\api-ms-win-core-synch-l1-1-0.dll"
Delete "$INSTDIR\runtime\bin\api-ms-win-core-synch-l1-2-0.dll"
Delete "$INSTDIR\runtime\bin\api-ms-win-core-sysinfo-l1-1-0.dll"
Delete "$INSTDIR\runtime\bin\api-ms-win-core-sysinfo-l1-2-0.dll"
Delete "$INSTDIR\runtime\bin\api-ms-win-core-timezone-l1-1-0.dll"
Delete "$INSTDIR\runtime\bin\api-ms-win-core-util-l1-1-0.dll"
Delete "$INSTDIR\runtime\bin\api-ms-win-crt-conio-l1-1-0.dll"
Delete "$INSTDIR\runtime\bin\api-ms-win-crt-convert-l1-1-0.dll"
Delete "$INSTDIR\runtime\bin\api-ms-win-crt-environment-l1-1-0.dll"
Delete "$INSTDIR\runtime\bin\api-ms-win-crt-filesystem-l1-1-0.dll"
Delete "$INSTDIR\runtime\bin\api-ms-win-crt-heap-l1-1-0.dll"
Delete "$INSTDIR\runtime\bin\api-ms-win-crt-locale-l1-1-0.dll"
Delete "$INSTDIR\runtime\bin\api-ms-win-crt-math-l1-1-0.dll"
Delete "$INSTDIR\runtime\bin\api-ms-win-crt-multibyte-l1-1-0.dll"
Delete "$INSTDIR\runtime\bin\api-ms-win-crt-private-l1-1-0.dll"
Delete "$INSTDIR\runtime\bin\api-ms-win-crt-process-l1-1-0.dll"
Delete "$INSTDIR\runtime\bin\api-ms-win-crt-runtime-l1-1-0.dll"
Delete "$INSTDIR\runtime\bin\api-ms-win-crt-stdio-l1-1-0.dll"
Delete "$INSTDIR\runtime\bin\api-ms-win-crt-string-l1-1-0.dll"
Delete "$INSTDIR\runtime\bin\api-ms-win-crt-time-l1-1-0.dll"
Delete "$INSTDIR\runtime\bin\api-ms-win-crt-utility-l1-1-0.dll"
Delete "$INSTDIR\runtime\bin\awt.dll"
Delete "$INSTDIR\runtime\bin\fontmanager.dll"
Delete "$INSTDIR\runtime\bin\freetype.dll"
Delete "$INSTDIR\runtime\bin\java.dll"
Delete "$INSTDIR\runtime\bin\javajpeg.dll"
Delete "$INSTDIR\runtime\bin\jawt.dll"
Delete "$INSTDIR\runtime\bin\jimage.dll"
Delete "$INSTDIR\runtime\bin\jli.dll"
Delete "$INSTDIR\runtime\bin\jsound.dll"
Delete "$INSTDIR\runtime\bin\lcms.dll"
Delete "$INSTDIR\runtime\bin\mlib_image.dll"
Delete "$INSTDIR\runtime\bin\msvcp140.dll"
Delete "$INSTDIR\runtime\bin\net.dll"
Delete "$INSTDIR\runtime\bin\nio.dll"
Delete "$INSTDIR\runtime\bin\prefs.dll"
Delete "$INSTDIR\runtime\bin\server\jvm.dll"
Delete "$INSTDIR\runtime\bin\splashscreen.dll"
Delete "$INSTDIR\runtime\bin\ucrtbase.dll"
Delete "$INSTDIR\runtime\bin\vcruntime140.dll"
Delete "$INSTDIR\runtime\bin\vcruntime140_1.dll"
Delete "$INSTDIR\runtime\bin\verify.dll"
Delete "$INSTDIR\runtime\bin\zip.dll"
Delete "$INSTDIR\runtime\conf\logging.properties"
Delete "$INSTDIR\runtime\conf\net.properties"
Delete "$INSTDIR\runtime\conf\security\java.policy"
Delete "$INSTDIR\runtime\conf\security\java.security"
Delete "$INSTDIR\runtime\conf\security\policy\limited\default_local.policy"
Delete "$INSTDIR\runtime\conf\security\policy\limited\default_US_export.policy"
Delete "$INSTDIR\runtime\conf\security\policy\limited\exempt_local.policy"
Delete "$INSTDIR\runtime\conf\security\policy\README.txt"
Delete "$INSTDIR\runtime\conf\security\policy\unlimited\default_local.policy"
Delete "$INSTDIR\runtime\conf\security\policy\unlimited\default_US_export.policy"
Delete "$INSTDIR\runtime\conf\sound.properties"
Delete "$INSTDIR\runtime\legal\java.base\ADDITIONAL_LICENSE_INFO"
Delete "$INSTDIR\runtime\legal\java.base\aes.md"
Delete "$INSTDIR\runtime\legal\java.base\asm.md"
Delete "$INSTDIR\runtime\legal\java.base\ASSEMBLY_EXCEPTION"
Delete "$INSTDIR\runtime\legal\java.base\c-libutl.md"
Delete "$INSTDIR\runtime\legal\java.base\cldr.md"
Delete "$INSTDIR\runtime\legal\java.base\icu.md"
Delete "$INSTDIR\runtime\legal\java.base\LICENSE"
Delete "$INSTDIR\runtime\legal\java.base\public_suffix.md"
Delete "$INSTDIR\runtime\legal\java.base\siphash.md"
Delete "$INSTDIR\runtime\legal\java.base\unicode.md"
Delete "$INSTDIR\runtime\legal\java.base\wepoll.md"
Delete "$INSTDIR\runtime\legal\java.base\zlib.md"
Delete "$INSTDIR\runtime\legal\java.datatransfer\ADDITIONAL_LICENSE_INFO"
Delete "$INSTDIR\runtime\legal\java.datatransfer\ASSEMBLY_EXCEPTION"
Delete "$INSTDIR\runtime\legal\java.datatransfer\LICENSE"
Delete "$INSTDIR\runtime\legal\java.desktop\ADDITIONAL_LICENSE_INFO"
Delete "$INSTDIR\runtime\legal\java.desktop\ASSEMBLY_EXCEPTION"
Delete "$INSTDIR\runtime\legal\java.desktop\colorimaging.md"
Delete "$INSTDIR\runtime\legal\java.desktop\freetype.md"
Delete "$INSTDIR\runtime\legal\java.desktop\giflib.md"
Delete "$INSTDIR\runtime\legal\java.desktop\harfbuzz.md"
Delete "$INSTDIR\runtime\legal\java.desktop\jpeg.md"
Delete "$INSTDIR\runtime\legal\java.desktop\lcms.md"
Delete "$INSTDIR\runtime\legal\java.desktop\libpng.md"
Delete "$INSTDIR\runtime\legal\java.desktop\LICENSE"
Delete "$INSTDIR\runtime\legal\java.desktop\mesa3d.md"
Delete "$INSTDIR\runtime\legal\java.logging\ADDITIONAL_LICENSE_INFO"
Delete "$INSTDIR\runtime\legal\java.logging\ASSEMBLY_EXCEPTION"
Delete "$INSTDIR\runtime\legal\java.logging\LICENSE"
Delete "$INSTDIR\runtime\legal\java.naming\ADDITIONAL_LICENSE_INFO"
Delete "$INSTDIR\runtime\legal\java.naming\ASSEMBLY_EXCEPTION"
Delete "$INSTDIR\runtime\legal\java.naming\LICENSE"
Delete "$INSTDIR\runtime\legal\java.net.http\ADDITIONAL_LICENSE_INFO"
Delete "$INSTDIR\runtime\legal\java.net.http\ASSEMBLY_EXCEPTION"
Delete "$INSTDIR\runtime\legal\java.net.http\LICENSE"
Delete "$INSTDIR\runtime\legal\java.prefs\ADDITIONAL_LICENSE_INFO"
Delete "$INSTDIR\runtime\legal\java.prefs\ASSEMBLY_EXCEPTION"
Delete "$INSTDIR\runtime\legal\java.prefs\LICENSE"
Delete "$INSTDIR\runtime\legal\java.security.sasl\ADDITIONAL_LICENSE_INFO"
Delete "$INSTDIR\runtime\legal\java.security.sasl\ASSEMBLY_EXCEPTION"
Delete "$INSTDIR\runtime\legal\java.security.sasl\LICENSE"
Delete "$INSTDIR\runtime\legal\java.sql\ADDITIONAL_LICENSE_INFO"
Delete "$INSTDIR\runtime\legal\java.sql\ASSEMBLY_EXCEPTION"
Delete "$INSTDIR\runtime\legal\java.sql\LICENSE"
Delete "$INSTDIR\runtime\legal\java.transaction.xa\ADDITIONAL_LICENSE_INFO"
Delete "$INSTDIR\runtime\legal\java.transaction.xa\ASSEMBLY_EXCEPTION"
Delete "$INSTDIR\runtime\legal\java.transaction.xa\LICENSE"
Delete "$INSTDIR\runtime\legal\java.xml\ADDITIONAL_LICENSE_INFO"
Delete "$INSTDIR\runtime\legal\java.xml\ASSEMBLY_EXCEPTION"
Delete "$INSTDIR\runtime\legal\java.xml\bcel.md"
Delete "$INSTDIR\runtime\legal\java.xml\dom.md"
Delete "$INSTDIR\runtime\legal\java.xml\jcup.md"
Delete "$INSTDIR\runtime\legal\java.xml\LICENSE"
Delete "$INSTDIR\runtime\legal\java.xml\xalan.md"
Delete "$INSTDIR\runtime\legal\java.xml\xerces.md"
Delete "$INSTDIR\runtime\legal\jdk.crypto.ec\ADDITIONAL_LICENSE_INFO"
Delete "$INSTDIR\runtime\legal\jdk.crypto.ec\ASSEMBLY_EXCEPTION"
Delete "$INSTDIR\runtime\legal\jdk.crypto.ec\LICENSE"
Delete "$INSTDIR\runtime\legal\jdk.unsupported\ADDITIONAL_LICENSE_INFO"
Delete "$INSTDIR\runtime\legal\jdk.unsupported\ASSEMBLY_EXCEPTION"
Delete "$INSTDIR\runtime\legal\jdk.unsupported\LICENSE"
Delete "$INSTDIR\runtime\lib\classlist"
Delete "$INSTDIR\runtime\lib\fontconfig.bfc"
Delete "$INSTDIR\runtime\lib\fontconfig.properties.src"
Delete "$INSTDIR\runtime\lib\jawt.lib"
Delete "$INSTDIR\runtime\lib\jrt-fs.jar"
Delete "$INSTDIR\runtime\lib\jvm.cfg"
Delete "$INSTDIR\runtime\lib\jvm.lib"
Delete "$INSTDIR\runtime\lib\modules"
Delete "$INSTDIR\runtime\lib\psfont.properties.ja"
Delete "$INSTDIR\runtime\lib\psfontj2d.properties"
Delete "$INSTDIR\runtime\lib\security\blocked.certs"
Delete "$INSTDIR\runtime\lib\security\cacerts"
Delete "$INSTDIR\runtime\lib\security\default.policy"
Delete "$INSTDIR\runtime\lib\security\public_suffix_list.dat"
Delete "$INSTDIR\runtime\lib\tzdb.dat"
Delete "$INSTDIR\runtime\lib\tzmappings"
Delete "$INSTDIR\runtime\release"
Delete "$INSTDIR\update-config.json"
Delete "$INSTDIR\update-root.json"
Delete "$INSTDIR\updater\_asyncio.pyd"
Delete "$INSTDIR\updater\_bz2.pyd"
Delete "$INSTDIR\updater\_ctypes.pyd"
Delete "$INSTDIR\updater\_decimal.pyd"
Delete "$INSTDIR\updater\_elementtree.pyd"
Delete "$INSTDIR\updater\_hashlib.pyd"
Delete "$INSTDIR\updater\_lzma.pyd"
Delete "$INSTDIR\updater\_multiprocessing.pyd"
Delete "$INSTDIR\updater\_overlapped.pyd"
Delete "$INSTDIR\updater\_queue.pyd"
Delete "$INSTDIR\updater\_socket.pyd"
Delete "$INSTDIR\updater\_sqlite3.pyd"
Delete "$INSTDIR\updater\_ssl.pyd"
Delete "$INSTDIR\updater\_uuid.pyd"
Delete "$INSTDIR\updater\_wmi.pyd"
Delete "$INSTDIR\updater\_zoneinfo.pyd"
Delete "$INSTDIR\updater\CCBStudioLauncher.exe"
Delete "$INSTDIR\updater\lib\_cffi_backend.cp313-win_amd64.pyd"
Delete "$INSTDIR\updater\lib\apply_update.py"
Delete "$INSTDIR\updater\lib\cffi\__init__.py"
Delete "$INSTDIR\updater\lib\cffi\_cffi_errors.h"
Delete "$INSTDIR\updater\lib\cffi\_cffi_gen_src.py"
Delete "$INSTDIR\updater\lib\cffi\_cffi_include.h"
Delete "$INSTDIR\updater\lib\cffi\_embedding.h"
Delete "$INSTDIR\updater\lib\cffi\_imp_emulation.py"
Delete "$INSTDIR\updater\lib\cffi\_shimmed_dist_utils.py"
Delete "$INSTDIR\updater\lib\cffi\api.py"
Delete "$INSTDIR\updater\lib\cffi\backend_ctypes.py"
Delete "$INSTDIR\updater\lib\cffi\cffi_opcode.py"
Delete "$INSTDIR\updater\lib\cffi\commontypes.py"
Delete "$INSTDIR\updater\lib\cffi\cparser.py"
Delete "$INSTDIR\updater\lib\cffi\error.py"
Delete "$INSTDIR\updater\lib\cffi\ffiplatform.py"
Delete "$INSTDIR\updater\lib\cffi\gen_src.py"
Delete "$INSTDIR\updater\lib\cffi\lock.py"
Delete "$INSTDIR\updater\lib\cffi\model.py"
Delete "$INSTDIR\updater\lib\cffi\parse_c_type.h"
Delete "$INSTDIR\updater\lib\cffi\pkgconfig.py"
Delete "$INSTDIR\updater\lib\cffi\recompiler.py"
Delete "$INSTDIR\updater\lib\cffi\setuptools_ext.py"
Delete "$INSTDIR\updater\lib\cffi\vengine_cpy.py"
Delete "$INSTDIR\updater\lib\cffi\vengine_gen.py"
Delete "$INSTDIR\updater\lib\cffi\verifier.py"
Delete "$INSTDIR\updater\lib\cffi-2.1.1.dist-info\entry_points.txt"
Delete "$INSTDIR\updater\lib\cffi-2.1.1.dist-info\licenses\LICENSE"
Delete "$INSTDIR\updater\lib\cffi-2.1.1.dist-info\METADATA"
Delete "$INSTDIR\updater\lib\cffi-2.1.1.dist-info\RECORD"
Delete "$INSTDIR\updater\lib\cffi-2.1.1.dist-info\top_level.txt"
Delete "$INSTDIR\updater\lib\cffi-2.1.1.dist-info\WHEEL"
Delete "$INSTDIR\updater\lib\cryptography\__about__.py"
Delete "$INSTDIR\updater\lib\cryptography\__init__.py"
Delete "$INSTDIR\updater\lib\cryptography\cobblestone.py"
Delete "$INSTDIR\updater\lib\cryptography\exceptions.py"
Delete "$INSTDIR\updater\lib\cryptography\fernet.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\__init__.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\_oid.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\asn1\__init__.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\asn1\asn1.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\backends\__init__.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\backends\openssl\__init__.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\backends\openssl\backend.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\bindings\__init__.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\bindings\_rust\__init__.pyi"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\bindings\_rust\_openssl.pyi"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\bindings\_rust\asn1.pyi"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\bindings\_rust\cobblestone.pyi"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\bindings\_rust\declarative_asn1.pyi"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\bindings\_rust\exceptions.pyi"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\bindings\_rust\ocsp.pyi"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\bindings\_rust\openssl\__init__.pyi"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\bindings\_rust\openssl\aead.pyi"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\bindings\_rust\openssl\ciphers.pyi"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\bindings\_rust\openssl\cmac.pyi"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\bindings\_rust\openssl\dh.pyi"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\bindings\_rust\openssl\dsa.pyi"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\bindings\_rust\openssl\ec.pyi"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\bindings\_rust\openssl\ed25519.pyi"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\bindings\_rust\openssl\ed448.pyi"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\bindings\_rust\openssl\hashes.pyi"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\bindings\_rust\openssl\hmac.pyi"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\bindings\_rust\openssl\hpke.pyi"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\bindings\_rust\openssl\kdf.pyi"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\bindings\_rust\openssl\keys.pyi"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\bindings\_rust\openssl\keywrap.pyi"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\bindings\_rust\openssl\mldsa.pyi"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\bindings\_rust\openssl\mlkem.pyi"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\bindings\_rust\openssl\poly1305.pyi"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\bindings\_rust\openssl\rsa.pyi"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\bindings\_rust\openssl\x25519.pyi"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\bindings\_rust\openssl\x448.pyi"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\bindings\_rust\pkcs12.pyi"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\bindings\_rust\pkcs7.pyi"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\bindings\_rust\test_support.pyi"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\bindings\_rust\x509.pyi"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\bindings\_rust.pyd"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\bindings\openssl\__init__.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\bindings\openssl\_conditional.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\bindings\openssl\binding.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\decrepit\__init__.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\decrepit\ciphers\__init__.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\decrepit\ciphers\algorithms.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\decrepit\ciphers\modes.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\primitives\__init__.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\primitives\_asymmetric.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\primitives\_cipheralgorithm.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\primitives\_modes.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\primitives\_serialization.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\primitives\asymmetric\__init__.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\primitives\asymmetric\dh.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\primitives\asymmetric\dsa.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\primitives\asymmetric\ec.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\primitives\asymmetric\ed25519.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\primitives\asymmetric\ed448.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\primitives\asymmetric\mldsa.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\primitives\asymmetric\mlkem.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\primitives\asymmetric\padding.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\primitives\asymmetric\rsa.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\primitives\asymmetric\types.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\primitives\asymmetric\utils.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\primitives\asymmetric\x25519.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\primitives\asymmetric\x448.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\primitives\ciphers\__init__.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\primitives\ciphers\aead.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\primitives\ciphers\algorithms.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\primitives\ciphers\base.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\primitives\ciphers\modes.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\primitives\cmac.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\primitives\constant_time.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\primitives\hashes.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\primitives\hmac.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\primitives\hpke.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\primitives\kdf\__init__.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\primitives\kdf\argon2.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\primitives\kdf\concatkdf.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\primitives\kdf\hkdf.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\primitives\kdf\kbkdf.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\primitives\kdf\pbkdf2.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\primitives\kdf\scrypt.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\primitives\kdf\x963kdf.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\primitives\keywrap.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\primitives\padding.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\primitives\poly1305.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\primitives\serialization\__init__.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\primitives\serialization\base.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\primitives\serialization\pkcs12.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\primitives\serialization\pkcs7.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\primitives\serialization\ssh.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\primitives\twofactor\__init__.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\primitives\twofactor\hotp.py"
Delete "$INSTDIR\updater\lib\cryptography\hazmat\primitives\twofactor\totp.py"
Delete "$INSTDIR\updater\lib\cryptography\py.typed"
Delete "$INSTDIR\updater\lib\cryptography\utils.py"
Delete "$INSTDIR\updater\lib\cryptography\x509\__init__.py"
Delete "$INSTDIR\updater\lib\cryptography\x509\base.py"
Delete "$INSTDIR\updater\lib\cryptography\x509\certificate_transparency.py"
Delete "$INSTDIR\updater\lib\cryptography\x509\extensions.py"
Delete "$INSTDIR\updater\lib\cryptography\x509\general_name.py"
Delete "$INSTDIR\updater\lib\cryptography\x509\name.py"
Delete "$INSTDIR\updater\lib\cryptography\x509\ocsp.py"
Delete "$INSTDIR\updater\lib\cryptography\x509\oid.py"
Delete "$INSTDIR\updater\lib\cryptography\x509\verification.py"
Delete "$INSTDIR\updater\lib\cryptography-50.0.2.dist-info\licenses\LICENSE"
Delete "$INSTDIR\updater\lib\cryptography-50.0.2.dist-info\licenses\LICENSE.APACHE"
Delete "$INSTDIR\updater\lib\cryptography-50.0.2.dist-info\licenses\LICENSE.BSD"
Delete "$INSTDIR\updater\lib\cryptography-50.0.2.dist-info\METADATA"
Delete "$INSTDIR\updater\lib\cryptography-50.0.2.dist-info\RECORD"
Delete "$INSTDIR\updater\lib\cryptography-50.0.2.dist-info\sboms\cryptography-rust.cyclonedx.json"
Delete "$INSTDIR\updater\lib\cryptography-50.0.2.dist-info\sboms\sbom.json"
Delete "$INSTDIR\updater\lib\cryptography-50.0.2.dist-info\WHEEL"
Delete "$INSTDIR\updater\lib\github_transport.py"
Delete "$INSTDIR\updater\lib\install_ops.py"
Delete "$INSTDIR\updater\lib\owned_files.py"
Delete "$INSTDIR\updater\lib\pycparser\__init__.py"
Delete "$INSTDIR\updater\lib\pycparser\_ast_gen.py"
Delete "$INSTDIR\updater\lib\pycparser\_c_ast.cfg"
Delete "$INSTDIR\updater\lib\pycparser\ast_transforms.py"
Delete "$INSTDIR\updater\lib\pycparser\c_ast.py"
Delete "$INSTDIR\updater\lib\pycparser\c_generator.py"
Delete "$INSTDIR\updater\lib\pycparser\c_lexer.py"
Delete "$INSTDIR\updater\lib\pycparser\c_parser.py"
Delete "$INSTDIR\updater\lib\pycparser-3.11.dist-info\licenses\LICENSE"
Delete "$INSTDIR\updater\lib\pycparser-3.11.dist-info\METADATA"
Delete "$INSTDIR\updater\lib\pycparser-3.11.dist-info\RECORD"
Delete "$INSTDIR\updater\lib\pycparser-3.11.dist-info\top_level.txt"
Delete "$INSTDIR\updater\lib\pycparser-3.11.dist-info\WHEEL"
Delete "$INSTDIR\updater\lib\residue_cleanup.py"
Delete "$INSTDIR\updater\lib\securesystemslib\__init__.py"
Delete "$INSTDIR\updater\lib\securesystemslib\_gpg\__init__.py"
Delete "$INSTDIR\updater\lib\securesystemslib\_gpg\common.py"
Delete "$INSTDIR\updater\lib\securesystemslib\_gpg\constants.py"
Delete "$INSTDIR\updater\lib\securesystemslib\_gpg\dsa.py"
Delete "$INSTDIR\updater\lib\securesystemslib\_gpg\eddsa.py"
Delete "$INSTDIR\updater\lib\securesystemslib\_gpg\exceptions.py"
Delete "$INSTDIR\updater\lib\securesystemslib\_gpg\functions.py"
Delete "$INSTDIR\updater\lib\securesystemslib\_gpg\handlers.py"
Delete "$INSTDIR\updater\lib\securesystemslib\_gpg\rsa.py"
Delete "$INSTDIR\updater\lib\securesystemslib\_gpg\util.py"
Delete "$INSTDIR\updater\lib\securesystemslib\_internal\utils.py"
Delete "$INSTDIR\updater\lib\securesystemslib\_vendor\__init__.py"
Delete "$INSTDIR\updater\lib\securesystemslib\_vendor\ed25519\.gitignore"
Delete "$INSTDIR\updater\lib\securesystemslib\_vendor\ed25519\__init__.py"
Delete "$INSTDIR\updater\lib\securesystemslib\_vendor\ed25519\ed25519.py"
Delete "$INSTDIR\updater\lib\securesystemslib\_vendor\ed25519\LICENSE"
Delete "$INSTDIR\updater\lib\securesystemslib\_vendor\ed25519\README.rst"
Delete "$INSTDIR\updater\lib\securesystemslib\_vendor\ed25519\science.py"
Delete "$INSTDIR\updater\lib\securesystemslib\_vendor\ed25519\test_data\ed25519"
Delete "$INSTDIR\updater\lib\securesystemslib\_vendor\ed25519\test_ed25519.py"
Delete "$INSTDIR\updater\lib\securesystemslib\_vendor\README.md"
Delete "$INSTDIR\updater\lib\securesystemslib\_vendor\test-ed25519-upstream.sh"
Delete "$INSTDIR\updater\lib\securesystemslib\dsse.py"
Delete "$INSTDIR\updater\lib\securesystemslib\exceptions.py"
Delete "$INSTDIR\updater\lib\securesystemslib\formats.py"
Delete "$INSTDIR\updater\lib\securesystemslib\hash.py"
Delete "$INSTDIR\updater\lib\securesystemslib\py.typed"
Delete "$INSTDIR\updater\lib\securesystemslib\signer\__init__.py"
Delete "$INSTDIR\updater\lib\securesystemslib\signer\_aws_signer.py"
Delete "$INSTDIR\updater\lib\securesystemslib\signer\_azure_signer.py"
Delete "$INSTDIR\updater\lib\securesystemslib\signer\_constants.py"
Delete "$INSTDIR\updater\lib\securesystemslib\signer\_crypto_signer.py"
Delete "$INSTDIR\updater\lib\securesystemslib\signer\_crypto_utils.py"
Delete "$INSTDIR\updater\lib\securesystemslib\signer\_gcp_signer.py"
Delete "$INSTDIR\updater\lib\securesystemslib\signer\_gpg_signer.py"
Delete "$INSTDIR\updater\lib\securesystemslib\signer\_hsm_signer.py"
Delete "$INSTDIR\updater\lib\securesystemslib\signer\_key.py"
Delete "$INSTDIR\updater\lib\securesystemslib\signer\_signature.py"
Delete "$INSTDIR\updater\lib\securesystemslib\signer\_signer.py"
Delete "$INSTDIR\updater\lib\securesystemslib\signer\_sigstore_signer.py"
Delete "$INSTDIR\updater\lib\securesystemslib\signer\_tkey_signer.py"
Delete "$INSTDIR\updater\lib\securesystemslib\signer\_utils.py"
Delete "$INSTDIR\updater\lib\securesystemslib\signer\_vault_signer.py"
Delete "$INSTDIR\updater\lib\securesystemslib\storage.py"
Delete "$INSTDIR\updater\lib\securesystemslib-1.6.0.dist-info\licenses\LICENSE"
Delete "$INSTDIR\updater\lib\securesystemslib-1.6.0.dist-info\METADATA"
Delete "$INSTDIR\updater\lib\securesystemslib-1.6.0.dist-info\RECORD"
Delete "$INSTDIR\updater\lib\securesystemslib-1.6.0.dist-info\WHEEL"
Delete "$INSTDIR\updater\lib\studio_update.py"
Delete "$INSTDIR\updater\lib\tuf\__init__.py"
Delete "$INSTDIR\updater\lib\tuf\api\__init__.py"
Delete "$INSTDIR\updater\lib\tuf\api\_payload.py"
Delete "$INSTDIR\updater\lib\tuf\api\dsse.py"
Delete "$INSTDIR\updater\lib\tuf\api\exceptions.py"
Delete "$INSTDIR\updater\lib\tuf\api\metadata.py"
Delete "$INSTDIR\updater\lib\tuf\api\serialization\__init__.py"
Delete "$INSTDIR\updater\lib\tuf\api\serialization\json.py"
Delete "$INSTDIR\updater\lib\tuf\ngclient\__init__.py"
Delete "$INSTDIR\updater\lib\tuf\ngclient\_internal\__init__.py"
Delete "$INSTDIR\updater\lib\tuf\ngclient\_internal\proxy.py"
Delete "$INSTDIR\updater\lib\tuf\ngclient\_internal\trusted_metadata_set.py"
Delete "$INSTDIR\updater\lib\tuf\ngclient\config.py"
Delete "$INSTDIR\updater\lib\tuf\ngclient\fetcher.py"
Delete "$INSTDIR\updater\lib\tuf\ngclient\README.md"
Delete "$INSTDIR\updater\lib\tuf\ngclient\requests_fetcher.py"
Delete "$INSTDIR\updater\lib\tuf\ngclient\updater.py"
Delete "$INSTDIR\updater\lib\tuf\ngclient\urllib3_fetcher.py"
Delete "$INSTDIR\updater\lib\tuf\py.typed"
Delete "$INSTDIR\updater\lib\tuf\repository\__init__.py"
Delete "$INSTDIR\updater\lib\tuf\repository\_repository.py"
Delete "$INSTDIR\updater\lib\tuf-7.0.1.dist-info\licenses\LICENSE"
Delete "$INSTDIR\updater\lib\tuf-7.0.1.dist-info\licenses\LICENSE-MIT"
Delete "$INSTDIR\updater\lib\tuf-7.0.1.dist-info\METADATA"
Delete "$INSTDIR\updater\lib\tuf-7.0.1.dist-info\RECORD"
Delete "$INSTDIR\updater\lib\tuf-7.0.1.dist-info\WHEEL"
Delete "$INSTDIR\updater\lib\typing_extensions-4.16.0.dist-info\licenses\LICENSE"
Delete "$INSTDIR\updater\lib\typing_extensions-4.16.0.dist-info\METADATA"
Delete "$INSTDIR\updater\lib\typing_extensions-4.16.0.dist-info\RECORD"
Delete "$INSTDIR\updater\lib\typing_extensions-4.16.0.dist-info\WHEEL"
Delete "$INSTDIR\updater\lib\typing_extensions.py"
Delete "$INSTDIR\updater\lib\urllib3\__init__.py"
Delete "$INSTDIR\updater\lib\urllib3\_base_connection.py"
Delete "$INSTDIR\updater\lib\urllib3\_collections.py"
Delete "$INSTDIR\updater\lib\urllib3\_request_methods.py"
Delete "$INSTDIR\updater\lib\urllib3\_version.py"
Delete "$INSTDIR\updater\lib\urllib3\connection.py"
Delete "$INSTDIR\updater\lib\urllib3\connectionpool.py"
Delete "$INSTDIR\updater\lib\urllib3\contrib\__init__.py"
Delete "$INSTDIR\updater\lib\urllib3\contrib\emscripten\__init__.py"
Delete "$INSTDIR\updater\lib\urllib3\contrib\emscripten\connection.py"
Delete "$INSTDIR\updater\lib\urllib3\contrib\emscripten\emscripten_fetch_worker.js"
Delete "$INSTDIR\updater\lib\urllib3\contrib\emscripten\fetch.py"
Delete "$INSTDIR\updater\lib\urllib3\contrib\emscripten\request.py"
Delete "$INSTDIR\updater\lib\urllib3\contrib\emscripten\response.py"
Delete "$INSTDIR\updater\lib\urllib3\contrib\pyopenssl.py"
Delete "$INSTDIR\updater\lib\urllib3\contrib\socks.py"
Delete "$INSTDIR\updater\lib\urllib3\exceptions.py"
Delete "$INSTDIR\updater\lib\urllib3\fields.py"
Delete "$INSTDIR\updater\lib\urllib3\filepost.py"
Delete "$INSTDIR\updater\lib\urllib3\http2\__init__.py"
Delete "$INSTDIR\updater\lib\urllib3\http2\connection.py"
Delete "$INSTDIR\updater\lib\urllib3\http2\probe.py"
Delete "$INSTDIR\updater\lib\urllib3\poolmanager.py"
Delete "$INSTDIR\updater\lib\urllib3\py.typed"
Delete "$INSTDIR\updater\lib\urllib3\response.py"
Delete "$INSTDIR\updater\lib\urllib3\util\__init__.py"
Delete "$INSTDIR\updater\lib\urllib3\util\connection.py"
Delete "$INSTDIR\updater\lib\urllib3\util\proxy.py"
Delete "$INSTDIR\updater\lib\urllib3\util\request.py"
Delete "$INSTDIR\updater\lib\urllib3\util\response.py"
Delete "$INSTDIR\updater\lib\urllib3\util\retry.py"
Delete "$INSTDIR\updater\lib\urllib3\util\ssl_.py"
Delete "$INSTDIR\updater\lib\urllib3\util\ssl_match_hostname.py"
Delete "$INSTDIR\updater\lib\urllib3\util\ssltransport.py"
Delete "$INSTDIR\updater\lib\urllib3\util\timeout.py"
Delete "$INSTDIR\updater\lib\urllib3\util\url.py"
Delete "$INSTDIR\updater\lib\urllib3\util\util.py"
Delete "$INSTDIR\updater\lib\urllib3\util\wait.py"
Delete "$INSTDIR\updater\lib\urllib3-2.8.0.dist-info\licenses\LICENSE.txt"
Delete "$INSTDIR\updater\lib\urllib3-2.8.0.dist-info\METADATA"
Delete "$INSTDIR\updater\lib\urllib3-2.8.0.dist-info\RECORD"
Delete "$INSTDIR\updater\lib\urllib3-2.8.0.dist-info\WHEEL"
Delete "$INSTDIR\updater\libcrypto-3.dll"
Delete "$INSTDIR\updater\libffi-8.dll"
Delete "$INSTDIR\updater\libssl-3.dll"
Delete "$INSTDIR\updater\LICENSE.txt"
Delete "$INSTDIR\updater\pyexpat.pyd"
Delete "$INSTDIR\updater\python.cat"
Delete "$INSTDIR\updater\python.exe"
Delete "$INSTDIR\updater\python3.dll"
Delete "$INSTDIR\updater\python313._pth"
Delete "$INSTDIR\updater\python313.dll"
Delete "$INSTDIR\updater\python313.zip"
Delete "$INSTDIR\updater\pythonw.exe"
Delete "$INSTDIR\updater\RUNTIME_MANIFEST.json"
Delete "$INSTDIR\updater\select.pyd"
Delete "$INSTDIR\updater\sqlite3.dll"
Delete "$INSTDIR\updater\unicodedata.pyd"
Delete "$INSTDIR\updater\vcruntime140.dll"
Delete "$INSTDIR\updater\vcruntime140_1.dll"
Delete "$INSTDIR\updater\winsound.pyd"
Delete "$INSTDIR\INSTALL_COMPLETE"
Delete "$INSTDIR\Uninstall.exe"
RMDir "$INSTDIR\updater\lib\cryptography\hazmat\bindings\_rust\openssl"
RMDir "$INSTDIR\updater\lib\securesystemslib\_vendor\ed25519\test_data"
RMDir "$INSTDIR\updater\lib\cryptography\hazmat\backends\openssl"
RMDir "$INSTDIR\updater\lib\cryptography\hazmat\bindings\openssl"
RMDir "$INSTDIR\updater\lib\cryptography\hazmat\bindings\_rust"
RMDir "$INSTDIR\updater\lib\cryptography\hazmat\decrepit\ciphers"
RMDir "$INSTDIR\updater\lib\cryptography\hazmat\primitives\asymmetric"
RMDir "$INSTDIR\updater\lib\cryptography\hazmat\primitives\ciphers"
RMDir "$INSTDIR\updater\lib\cryptography\hazmat\primitives\kdf"
RMDir "$INSTDIR\updater\lib\cryptography\hazmat\primitives\serialization"
RMDir "$INSTDIR\updater\lib\cryptography\hazmat\primitives\twofactor"
RMDir "$INSTDIR\updater\lib\urllib3\contrib\emscripten"
RMDir "$INSTDIR\updater\lib\tuf\api\serialization"
RMDir "$INSTDIR\updater\lib\tuf\ngclient\_internal"
RMDir "$INSTDIR\updater\lib\securesystemslib\_vendor\ed25519"
RMDir "$INSTDIR\updater\lib\cryptography\hazmat\asn1"
RMDir "$INSTDIR\updater\lib\cryptography\hazmat\backends"
RMDir "$INSTDIR\updater\lib\cryptography\hazmat\bindings"
RMDir "$INSTDIR\updater\lib\cryptography\hazmat\decrepit"
RMDir "$INSTDIR\updater\lib\cryptography\hazmat\primitives"
RMDir "$INSTDIR\runtime\conf\security\policy\limited"
RMDir "$INSTDIR\runtime\conf\security\policy\unlimited"
RMDir "$INSTDIR\updater\lib\cffi-2.1.1.dist-info\licenses"
RMDir "$INSTDIR\updater\lib\cryptography\hazmat"
RMDir "$INSTDIR\updater\lib\cryptography\x509"
RMDir "$INSTDIR\updater\lib\cryptography-50.0.2.dist-info\licenses"
RMDir "$INSTDIR\updater\lib\cryptography-50.0.2.dist-info\sboms"
RMDir "$INSTDIR\updater\lib\pycparser-3.11.dist-info\licenses"
RMDir "$INSTDIR\updater\lib\securesystemslib\signer"
RMDir "$INSTDIR\updater\lib\securesystemslib\_gpg"
RMDir "$INSTDIR\updater\lib\securesystemslib\_internal"
RMDir "$INSTDIR\updater\lib\securesystemslib\_vendor"
RMDir "$INSTDIR\updater\lib\securesystemslib-1.6.0.dist-info\licenses"
RMDir "$INSTDIR\updater\lib\tuf\api"
RMDir "$INSTDIR\updater\lib\tuf\ngclient"
RMDir "$INSTDIR\updater\lib\tuf\repository"
RMDir "$INSTDIR\updater\lib\tuf-7.0.1.dist-info\licenses"
RMDir "$INSTDIR\updater\lib\typing_extensions-4.16.0.dist-info\licenses"
RMDir "$INSTDIR\updater\lib\urllib3\contrib"
RMDir "$INSTDIR\updater\lib\urllib3\http2"
RMDir "$INSTDIR\updater\lib\urllib3\util"
RMDir "$INSTDIR\updater\lib\urllib3-2.8.0.dist-info\licenses"
RMDir "$INSTDIR\runtime\conf\security\policy"
RMDir "$INSTDIR\updater\lib\cffi"
RMDir "$INSTDIR\updater\lib\cffi-2.1.1.dist-info"
RMDir "$INSTDIR\updater\lib\cryptography"
RMDir "$INSTDIR\updater\lib\cryptography-50.0.2.dist-info"
RMDir "$INSTDIR\updater\lib\pycparser"
RMDir "$INSTDIR\updater\lib\pycparser-3.11.dist-info"
RMDir "$INSTDIR\updater\lib\securesystemslib"
RMDir "$INSTDIR\updater\lib\securesystemslib-1.6.0.dist-info"
RMDir "$INSTDIR\updater\lib\tuf"
RMDir "$INSTDIR\updater\lib\tuf-7.0.1.dist-info"
RMDir "$INSTDIR\updater\lib\typing_extensions-4.16.0.dist-info"
RMDir "$INSTDIR\updater\lib\urllib3"
RMDir "$INSTDIR\updater\lib\urllib3-2.8.0.dist-info"
RMDir "$INSTDIR\runtime\bin\server"
RMDir "$INSTDIR\runtime\conf\security"
RMDir "$INSTDIR\runtime\legal\java.base"
RMDir "$INSTDIR\runtime\legal\java.datatransfer"
RMDir "$INSTDIR\runtime\legal\java.desktop"
RMDir "$INSTDIR\runtime\legal\java.logging"
RMDir "$INSTDIR\runtime\legal\java.naming"
RMDir "$INSTDIR\runtime\legal\java.net.http"
RMDir "$INSTDIR\runtime\legal\java.prefs"
RMDir "$INSTDIR\runtime\legal\java.security.sasl"
RMDir "$INSTDIR\runtime\legal\java.sql"
RMDir "$INSTDIR\runtime\legal\java.transaction.xa"
RMDir "$INSTDIR\runtime\legal\java.xml"
RMDir "$INSTDIR\runtime\legal\jdk.crypto.ec"
RMDir "$INSTDIR\runtime\legal\jdk.unsupported"
RMDir "$INSTDIR\runtime\lib\security"
RMDir "$INSTDIR\app\resources"
RMDir "$INSTDIR\runtime\bin"
RMDir "$INSTDIR\runtime\conf"
RMDir "$INSTDIR\runtime\legal"
RMDir "$INSTDIR\runtime\lib"
RMDir "$INSTDIR\updater\lib"
RMDir "$INSTDIR\app"
RMDir "$INSTDIR\runtime"
RMDir "$INSTDIR\updater"
RMDir "$INSTDIR"
SectionEnd
