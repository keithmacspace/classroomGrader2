; Turn off old selected section
; 12 06 2005: Luis Wong
; Template to generate an installer
; Especially for the generation of EasyPlayer installers
; Trimedia Interactive Projects
 
; -------------------------------
; 
; Start
!include MUI2.nsh
!include zipdll.nsh

  !define MUI_VERSION "1.0"
  !define MUI_PRODUCT "GradingAssistant";
  !define MUI_FILE "GradeAsst-all.jar"
  !define MUI_JPLAG "jplag-2.12.1.jar"
  !define MUI_BRANDINGTEXT "Classroom Grader Ver. ${MUI_VERSION}"
  !define SOURCE_PATH "..\GradeAsst\build\libs"
  !define JAVA_PATH "$PROGRAMFILES64\java"
  ;!define JAVA_PATH "c:\temp\java"
  !define JAVA_DEST "${JAVA_PATH}\jdk-12.0.2"
  !define ZIP_NAME "openjdk-12.0.2_windows-x64_bin.zip"
  !define ZIP_SOURCE "https://download.java.net/java/GA/jdk12.0.2/e482c34c86bd4bf8b56c0b35558996b9/10/GPL/openjdk-12.0.2_windows-x64_bin.zip"
  !define ZIP_DEST "$PLUGINSDIR\${ZIP_NAME}"
  CRCCheck On
 
 
 
;---------------------------------
;General

  Name "Grading Assistant"
  OutFile "installGradeAsst.exe"
  ShowInstDetails "nevershow"
  ShowUninstDetails "nevershow"
  ;SetCompressor "bzip2"
 

 
;--------------------------------
;Folder selection page
 
  InstallDir "$PROGRAMFILES64\${MUI_PRODUCT}"
 

  RequestExecutionLevel admin

  !define MUI_ABORTWARNING

  ;!insertmacro MUI_WELCOME
 ; !insertmacro MUI_PAGE_COMPONENTS
  !insertmacro MUI_PAGE_DIRECTORY
  !insertmacro MUI_PAGE_INSTFILES
  
  !insertmacro MUI_UNPAGE_CONFIRM
  !insertmacro MUI_UNPAGE_INSTFILES

;--------------------------------
;Languages
 
  !insertmacro MUI_LANGUAGE "English"
 
 
;-------------------------------- 
;Installer Sections     
;
Section "Install Java JDK 12.0.2 From OpenJDK" InstallJDK
  IfFileExists "${JAVA_DEST}" done 0
  MessageBox MB_OK "Grading Assistant needs JDK version 12.0.2 or higher to work.  Installing jdk12.0.2 from openjdk"
  SetOutPath ${JAVA_PATH}
  inetc::get /NOCANCEL "${ZIP_SOURCE}" "${ZIP_DEST}" /end
  nsisunz::Unzip "${ZIP_DEST}" "${JAVA_PATH}"
  goto done
done:
SectionEnd

Section "Grading Assistant" InstallGrader
 
;Add files
  SetOutPath "$INSTDIR"
 
  File "${SOURCE_PATH}\${MUI_FILE}"
  File "${SOURCE_PATH}\${MUI_JPLAG}"
;create desktop shortcut
  CreateShortCut "$DESKTOP\${MUI_PRODUCT}.lnk" "${JAVA_DEST}\bin\javaw.exe" '-jar "$INSTDIR\${MUI_FILE}"' 
 
;create start-menu items
  CreateDirectory "$SMPROGRAMS\${MUI_PRODUCT}"
  CreateShortCut "$SMPROGRAMS\${MUI_PRODUCT}\Uninstall.lnk" "$INSTDIR\Uninstall.exe" "" "$INSTDIR\Uninstall.exe" 0
  CreateShortCut "$SMPROGRAMS\${MUI_PRODUCT}\${MUI_PRODUCT}.lnk" "${JAVA_DEST}\bin\javaw.exe" '-jar "$INSTDIR\${MUI_FILE}"' 
 
;write uninstall information to the registry
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${MUI_PRODUCT}" "DisplayName" "${MUI_PRODUCT} (remove only)"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${MUI_PRODUCT}" "UninstallString" "$INSTDIR\Uninstall.exe"
 
  WriteUninstaller "$INSTDIR\Uninstall.exe"
 
SectionEnd
 
 
;--------------------------------    
;Uninstaller Section  
Section "Uninstall"
 
;Delete Files 
  RMDir /r "$INSTDIR\*.*"    
 
;Remove the installation directory
  RMDir "$INSTDIR"
 
;Delete Start Menu Shortcuts
  Delete "$DESKTOP\${MUI_PRODUCT}.lnk"
  Delete "$SMPROGRAMS\${MUI_PRODUCT}\*.*"
  RmDir  "$SMPROGRAMS\${MUI_PRODUCT}"
 
;Delete Uninstaller And Unistall Registry Entries
  DeleteRegKey HKEY_LOCAL_MACHINE "SOFTWARE\${MUI_PRODUCT}"
  DeleteRegKey HKEY_LOCAL_MACHINE "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${MUI_PRODUCT}"  
 
SectionEnd
 
 
;--------------------------------    
;MessageBox Section
Function .onInit
	InitPluginsDir
FunctionEnd	
 
;Function that calls a messagebox when installation finished correctly
Function .onInstSuccess
  MessageBox MB_OK "You have successfully installed ${MUI_PRODUCT}. Use the desktop icon to start the program.  On the first launch, a guided setup will run."
FunctionEnd
 
Function un.onUninstSuccess
FunctionEnd
 
 
;eof
