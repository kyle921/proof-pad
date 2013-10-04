!define APPNAME "Proof Pad"
!define DESCRIPTION "An IDE for ACL2"

!define VERSIONMAJOR 0
!define VERSIONMINOR 3

RequestExecutionLevel admin

!include "MUI2.nsh"

Name "${APPNAME}"
!define MUI_ICON "Icons\icon.ico"
!define MUI_UNICON "Icons\icon.ico"
outFile "InstallProofPad.exe"
InstallDir "$PROGRAMFILES\${APPNAME}"

!define MUI_ABORTWARNING

!insertmacro MUI_PAGE_LICENSE "LICENSE"
Var SMFOLDER
!insertmacro MUI_PAGE_STARTMENU "Application" $SMFOLDER
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_INSTFILES
!define MUI_FINISHPAGE_RUN "$INSTDIR\proofpad.exe"
!define MUI_FINISHPAGE_RUN_TEXT "Launch Proof Pad"
!insertmacro MUI_PAGE_FINISH

!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES

!insertmacro MUI_LANGUAGE "English"
BrandingText " "

!include LogicLib.nsh

!macro VerifyUserIsAdmin
UserInfo::GetAccountType
	pop $0
	${If} $0 != "admin"
		messageBox mb_iconstop "Administrator rights required."
		setErrorLevel 740
		quit
	${EndIf}
!macroend

!macro _RunningX64 _a _b _t _f
	!insertmacro _LOGICLIB_TEMP
	System::Call kernel32::GetCurrentProcess()i.s
	System::Call kernel32::IsWow64Process(is,*i.s)
	Pop $_LOGICLIB_TEMP
	!insertmacro _!= $_LOGICLIB_TEMP 0 `${_t}` `${_f}`
!macroend

!define RunningX64 `"" RunningX64 ""`

function .onInit
	System::Call 'kernel32::CreateMutexA(i 0, i 0, t "proofpad-install") i .r1 ?e'
	Pop $R0
	StrCmp $R0 0 +3
		MessageBox MB_OK|MB_ICONEXCLAMATION "The installer is already running."
		Abort

	${If} ${RunningX64}
		StrCpy $INSTDIR "$PROGRAMFILES64\Proof Pad\"
	${EndIf}
	setShellVarContext all
	!insertmacro VerifyUserIsAdmin
functionEnd

section "install"
	setOutPath $INSTDIR
	File "proofpad.jar"
	File "proofpad.exe"
	File "Icons\icon.ico"
	File /r "acl2"

	writeUninstaller "$INSTDIR\uninstall.exe"

	# Start Menu
	createDirectory "$SMPROGRAMS"
	createShortCut "$SMPROGRAMS\${APPNAME}.lnk" "$INSTDIR\proofpad.exe" "" "$INSTDIR\icon.ico"

	# Registry information for add/remove programs
	WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "DisplayName" "${APPNAME} - ${DESCRIPTION}"
	WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "UninstallString" "$\"$INSTDIR\uninstall.exe$\""
	WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "QuietUninstallString" "$\"$INSTDIR\uninstall.exe$\" /S"
	WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "InstallLocation" "$\"$INSTDIR$\""
	WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "DisplayIcon" "$\"$INSTDIR\icons.ico$\""
	WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "Publisher" "$\"Caleb Eggensperger$\""
	WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "DisplayVersion" "$\"${VERSIONMAJOR}.${VERSIONMINOR}$\""
	WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "VersionMajor" ${VERSIONMAJOR}
	WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "VersionMinor" ${VERSIONMINOR}
	WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "NoModify" 1
	WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "NoRepair" 1
sectionEnd

# Uninstaller

function un.onInit
	# Make sure the uninstaller isn't already runnign
	System::Call 'kernel32::CreateMutexA(i 0, i 0, t "proofpad-uninstall") i .r1 ?e'
	Pop $R0
	StrCmp $R0 0 +3
		MessageBox MB_OK|MB_ICONEXCLAMATION "The uninstaller is already running."
		Abort

	SetShellVarContext all
	!insertmacro VerifyUserIsAdmin
functionEnd

section "uninstall"
	delete "$SMPROGRAMS\${APPNAME}.lnk"
	RMDir /r "$INSTDIR\acl2"
	Delete "$INSTDIR\icon.ico"
	Delete "$INSTDIR\proofpad.exe"
	Delete "$INSTDIR\proofpad.jar"
	Delete "$INSTDIR\uninstall.exe"
	Delete "$INSTDIR\user_data.dat"

	RMDir $INSTDIR
	IfFileExists "$INSTDIR\*.*" +1 +2
		MessageBox MB_OK|MB_ICONINFORMATION|MB_SETFOREGROUND \
			"Install directory $INSTDIR contains extra files. Please verify that you wish to remove these \
			files before deleting the directory." \
			/SD IDOK

	# Remove uninstaller information from the registry
	DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}"
sectionEnd
