package com.intellij.execution.configurations;

import com.intellij.execution.process.UnixProcessManager;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.EnvironmentUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * @author Sergey Simonchik
 */
public class PathEnvironmentVariableUtil {

  public static final String PATH_ENV_VAR_NAME = "PATH";

  private static final String ourFixedMacPathEnvVarValue;

  static {
    ourFixedMacPathEnvVarValue = calcFixedMacPathEnvVarValue();
  }

  private PathEnvironmentVariableUtil() {
  }

  /**
   * Tries to return a real value for PATH environment variable.
   * Workaround for http://youtrack.jetbrains.com/issue/IDEA-99154
   */
  @Nullable
  public static String getFixedPathEnvVarValueOnMac() {
    return ourFixedMacPathEnvVarValue;
  }

  private static String calcFixedMacPathEnvVarValue() {
    if (SystemInfo.isMac) {
      final String originalPath = getOriginalPathEnvVarValue();
      Map<? extends String, ? extends String> envVars = UnixProcessManager.getOrLoadConsoleEnvironment();
      String consolePath = envVars.get(PATH_ENV_VAR_NAME);
      return mergePaths(ContainerUtil.newArrayList(originalPath, consolePath));
    }
    return null;
  }

  @Nullable
  private static String mergePaths(@NotNull List<String> pathStrings) {
    LinkedHashSet<String> paths = ContainerUtil.newLinkedHashSet();
    for (String pathString : pathStrings) {
      if (pathString != null) {
        List<String> locals = StringUtil.split(pathString, File.pathSeparator, true, true);
        for (String local : locals) {
          paths.add(local);
        }
      }
    }
    if (paths.isEmpty()) {
      return null;
    }
    return StringUtil.join(paths, File.pathSeparator);
  }

  @Nullable
  private static String getOriginalPathEnvVarValue() {
    Map<String, String> originalEnvVars = EnvironmentUtil.getEnvironmentProperties();
    return originalEnvVars.get(PATH_ENV_VAR_NAME);
  }

  @NotNull
  private static String getReliablePath() {
    final String value;
    if (SystemInfo.isMac) {
      value = getFixedPathEnvVarValueOnMac();
    }
    else {
      value = getOriginalPathEnvVarValue();
    }
    return StringUtil.notNullize(value);
  }

  /**
   * Finds an executable file with the specified base name, that is located in a directory
   * listed in PATH environment variable.
   *
   * @param fileBaseName file base name
   * @return {@code File} instance or null if not found
   */
  @Nullable
  public static File findInPath(@NotNull String fileBaseName) {
    String pathEnvVarValue = getReliablePath();
    List<String> paths = StringUtil.split(pathEnvVarValue, File.pathSeparator, true, true);
    for (String path : paths) {
      File dir = new File(path);
      if (dir.isAbsolute() && dir.isDirectory()) {
        File file = new File(dir, fileBaseName);
        if (file.isFile() && file.canExecute()) {
          return file;
        }
      }
    }
    return null;
  }

}
