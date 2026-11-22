/*
 * JdkUpdater.exe: starts runtime\javaw.exe with the application jar, both next to this exe.
 *
 * Folder layout:
 *   JdkUpdater.exe
 *   app\jdk-updater.jar
 *   runtime\bin\javaw.exe
 */
#include <windows.h>
#include <wchar.h>

#define COMMAND_LENGTH (4 * MAX_PATH)
#define MAIN_CLASS L"ch.jdkupdater.ui.Launcher"

int WINAPI wWinMain(HINSTANCE instance, HINSTANCE previous, PWSTR arguments, int show) {
    wchar_t folder[MAX_PATH];
    wchar_t command[COMMAND_LENGTH];

    DWORD length = GetModuleFileNameW(NULL, folder, MAX_PATH);
    if (length == 0 || length >= MAX_PATH) {
        return 1;
    }
    wchar_t *lastSeparator = wcsrchr(folder, L'\\');
    if (lastSeparator != NULL) {
        *lastSeparator = L'\0';
    }

    _snwprintf(command, COMMAND_LENGTH,
               L"\"%ls\\runtime\\bin\\javaw.exe\" --add-modules javafx.controls "
               L"--enable-native-access=javafx.graphics "
               L"-cp \"%ls\\app\\jdk-updater.jar\" " MAIN_CLASS,
               folder, folder);
    command[COMMAND_LENGTH - 1] = L'\0';

    STARTUPINFOW startup = { sizeof startup };
    PROCESS_INFORMATION process;
    if (!CreateProcessW(NULL, command, NULL, NULL, FALSE, 0, NULL, folder, &startup, &process)) {
        MessageBoxW(NULL, L"Could not start runtime\\bin\\javaw.exe", L"JDK Updater", MB_ICONERROR);
        return 1;
    }
    CloseHandle(process.hThread);
    CloseHandle(process.hProcess);
    return 0;
}
