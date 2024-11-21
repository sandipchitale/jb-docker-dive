package dev.sandipchitale.jbdockerdive;

import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.terminal.ShellTerminalWidget;
import org.jetbrains.plugins.terminal.TerminalToolWindowManager;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DockerDiveAction extends AnAction {
    private static String DIVE = "dive";
    static {
        String pluginPath = Objects.requireNonNull(PluginManagerCore.getPlugin(PluginId.getId("dev.sandipchitale.jb-docker-dive"))).getPluginPath().toFile().getAbsolutePath();
        if (SystemInfo.isLinux) {
            DIVE = Path.of(
                    pluginPath,
                    "dive",
                    "linux",
                    "amd64",
                    "dive"
            ).toString();
        } else if (SystemInfo.isMac) {
            DIVE = Path.of(
                    pluginPath,
                    "dive",
                    "darwin",
                    "amd64",
                    "dive"
            ).toString();
        } else if (SystemInfo.isWindows) {
            DIVE = Path.of(
                    pluginPath,
                    "dive",
                    "windows",
                    "amd64",
                    "dive.exe"
            ).toString();
        }
    }

    private static final Pattern tagPattern = Pattern.compile(".+ \\[IMG]: \\[([^\\s]+)]");

    @Override
    public void actionPerformed(@NotNull AnActionEvent actionEvent) {
        Project project = actionEvent.getProject();
        Object data = actionEvent.getDataContext().getData(PlatformCoreDataKeys.SELECTED_ITEM);
        if (data != null && (data.getClass().getName().equals("com.intellij.docker.runtimes.DockerImageRuntimeImpl"))) {
            String dataString = data.toString();
            Matcher matcher = tagPattern.matcher(dataString);
            if (matcher.matches()) {
                @NotNull ShellTerminalWidget shellTerminalWidget =
                        TerminalToolWindowManager.getInstance(Objects.requireNonNull(project)).createLocalShellWidget(project.getBasePath(),
                                "Dive",
                                true,
                                true);
                try {
                    shellTerminalWidget.executeCommand(String.format("%s %s",
                            DIVE,
                            matcher.group(1)));
                } catch (IOException ignore) {
                }
            }
        }
    }

    @Override
    public void update(@NotNull AnActionEvent actionEvent) {
        boolean visible = false;
        Object data = actionEvent.getDataContext().getData(PlatformCoreDataKeys.SELECTED_ITEM);
        if (data != null && (data.getClass().getName().equals("com.intellij.docker.runtimes.DockerImageRuntimeImpl"))) {
            visible = true;
        }
        actionEvent.getPresentation().setVisible(visible);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
