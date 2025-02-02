package dev.sandipchitale.jbdockerdive;

import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.terminal.ui.TerminalWidget;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.terminal.TerminalToolWindowManager;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
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
                    "arm64",
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

    static {
        if (SystemInfo.isLinux || SystemInfo.isMac) {
            try {
                boolean executable = new File(DIVE).setExecutable(true);
            } catch (RuntimeException ignore) {
            }
        }
    }

    private static final Pattern tagPattern = Pattern.compile(".+ \\[IMG]: \\[(\\S+)]");

    @Override
    public void actionPerformed(@NotNull AnActionEvent actionEvent) {
        Project project = actionEvent.getProject();
        Object data = actionEvent.getDataContext().getData(PlatformCoreDataKeys.SELECTED_ITEM);
        String imageId = null;
        if (data != null) {
            String dataString = data.toString();
            if (data.getClass().getName().equals("com.intellij.docker.runtimes.DockerImageRuntimeImpl")) {
                Matcher matcher = tagPattern.matcher(dataString);
                if (matcher.matches()) {
                    imageId = matcher.group(1);
                }
            } else if (data.getClass().getName().equals("com.intellij.docker.runtimes.DockerApplicationRuntime")) {
                try {
                    imageId = (String) data.getClass().getMethod("getImageId").invoke(data);
                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ignore) {
                }
            }
        }
        if (imageId != null) {
            @NotNull TerminalWidget shellTerminalWidget =
                    TerminalToolWindowManager.getInstance(Objects.requireNonNull(project)).createShellWidget(project.getBasePath(),
                            "Dive",
                            true,
                            true);
            shellTerminalWidget.sendCommandToExecute(String.format("\"%s\" \"%s\"",
                    DIVE,
                    imageId));
        }
    }

    @Override
    public void update(@NotNull AnActionEvent actionEvent) {
        boolean visible = false;
        Object data = actionEvent.getDataContext().getData(PlatformCoreDataKeys.SELECTED_ITEM);
        if (data != null && (data.getClass().getName().equals("com.intellij.docker.runtimes.DockerImageRuntimeImpl") ||
                data.getClass().getName().equals("com.intellij.docker.runtimes.DockerApplicationRuntime"))) {
            visible = true;
        }
        actionEvent.getPresentation().setVisible(visible);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
