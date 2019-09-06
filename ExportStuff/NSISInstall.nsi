

Name "Grading Assistant Installer"


OutFile "gradAsstSetup.exe"
InstallDir $PROGRAMFILES\gradingAssistant
DirText "This will install Grading Assistant on your computer.  Choose a directory"

Section ""
SetOutPath $INSTDIR

File gradingAssistant.exe
File jplag-2.12.1.jar

; Tell the compiler to write an uninstaller and to look for a "Uninstall" section 
WriteUninstaller $INSTDIR\Uninstall.exe

; Create shortcuts
CreateDirectory "$SMPROGRAMS\Grading Assistant"
CreateShortCut "$SMPROGRAMS\Grading Assistant\Grading Assistant.lnk" "$INSTDIR\gradingAssistant.exe"
CreateShortCut "$DESKTOP\Grading Assistant.lnk" "$INSTDIR\gradingAssistant.exe"


WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Grading Assistant" "DisplayName"\
"Grading Assistant (remove only)"

WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Grading Assistant" "UninstallString" \
"$INSTDIR\Uninstall.exe"
 

SectionEnd

Section "Uninstall"

Delete $INSTDIR\Uninstall.exe
Delete $INSTDIR\gradingAssistant.exe
Delete $INSTDIR\jplag-2.12.1.jar
RMDir $INSTDIR

Delete "$DESKTOP\Grading Assistant.lnk" 
Delete "$SMPROGRAMS\Grading Assistant\Grading Assistant.lnk"
RMDIR "$SMPROGRAMS\Grading Assistant"
DeleteRegKey HKEY_LOCAL_MACHINE "SOFTWARE\Grading Assistant"
DeleteRegKey HKEY_LOCAL_MACHINE "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\Grading Assistant"
SectionEnd