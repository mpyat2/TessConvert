;VStar InnoSetup Script

#define TheGroupName "Tess Convert"
#define TheAppName "Tess Convert"
; Normally, TheAppVersion defined via ISCC.exe command-line parameter
#define TheAppVersion "1.0"
#define TheAppPublisher "PMAK"
#define TheAppURL "https://www.osokorky-observatory.com/"
#define TheAppExeName "TessConvertW.bat"

[Setup]
; NOTE: The value of AppId uniquely identifies this application.
; Do not use the same AppId value in installers for other applications.
; (To generate a new GUID, click Tools | Generate GUID inside the IDE.)
AppId={{905239C9-0FD7-488E-B3B2-907778C5F7BF}}
AppName={#TheAppName}
AppVersion={#TheAppVersion}
AppPublisher={#TheAppPublisher}
AppPublisherURL={#TheAppURL}
AppSupportURL={#TheAppURL}
AppUpdatesURL={#TheAppURL}
DefaultDirName={%HOMEDRIVE}{%HOMEPATH}\TessConvert
DefaultGroupName={#TheGroupName}
;DisableProgramGroupPage=yes
DisableWelcomePage=no
;WizardImageFile=tenstar_artist_conception1.bmp
;WizardSmallImageFile=aavso.bmp
OutputBaseFilename=TessConvertSetup-{#TheAppVersion}
OutputDir=.\
Compression=lzma
SolidCompression=yes
PrivilegesRequired=lowest

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: 

[Files]
; NOTE: Don't use "Flags: ignoreversion" on any shared system files
Source: "..\{#TheAppExeName}"; DestDir: "{app}"            ; Flags: ignoreversion
Source: "..\TessConvert.bat" ; DestDir: "{app}"            ; Flags: ignoreversion
Source: "..\dist\*.jar"      ; DestDir: "{app}\dist"       ; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "..\extlib\*.jar"    ; DestDir: "{app}\extlib"     ; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\{#TheAppName}"; Filename: "{app}\{#TheAppExeName}"
Name: "{userdesktop}\{#TheAppName}" ; Filename: "{app}\{#TheAppExeName}"; Tasks: desktopicon

