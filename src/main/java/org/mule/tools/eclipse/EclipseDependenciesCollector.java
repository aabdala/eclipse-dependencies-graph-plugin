package org.mule.tools.eclipse;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

public class EclipseDependenciesCollector {

  private static final String META_INF_DIR = "META-INF";
  private static final String MANIFEST_MF_FILE = "MANIFEST.MF";
  private static final String TARGET_DIR = "target";

  private static final String BUNDLE_NAME_HEADER = "Bundle-Name: ";
  private static final String REQ_BUNDLE_HEADER = "Require-Bundle: ";
  private static final String BUNDLE_VERSION_HEADER = "Bundle-Version: ";
  private static final String BUNDLE_SYMBOLIC_NAME_HEADER = "Bundle-SymbolicName: ";

  private static final String REQ_BUNDLE_REGEX = "^ ([a-zA-Z0-9\\._-]+)(?:;.+)?,?";
  private static final Pattern REQ_BUNDLE_PATTERN = Pattern.compile(REQ_BUNDLE_REGEX);

  public Map<Project, List<Project>> getDependencies(File projectFolder) throws IOException {
    Map<Project, List<String>> results = new HashMap<>();
    recurse(projectFolder, results);
    return replaceProjectIds(results);
  }

  private void recurse(File projectFolder, Map<Project, List<String>> results) throws IOException {
    File manifestFile = Paths.get(projectFolder.toURI()).resolve(META_INF_DIR).resolve(MANIFEST_MF_FILE).toFile();
    if (manifestFile.exists()) {
      List<String> manifestLines = FileUtils.readLines(manifestFile, Charset.defaultCharset());
      Project project = getProject(manifestLines);
      if (project != null) {
        List<String> dependencies = getDependencies(projectFolder, manifestLines);
        results.put(project, dependencies);
      }
    } else if (projectFolder.isDirectory()) {
      File[] potentialProjectFolders = projectFolder.listFiles((file, name) -> !file.isHidden() && file.isDirectory());
      for (File folder : potentialProjectFolders) {
        if (!TARGET_DIR.equals(folder.getName())) {
          recurse(folder, results);
        }
      }
    }
  }

  private List<String> getDependencies(File projectFolder, List<String> manifestLines) throws IOException {
    Optional<String> requireBundleLine = manifestLines.stream().filter(line -> line.startsWith(REQ_BUNDLE_HEADER))
        .findFirst();

    return requireBundleLine.map(reqBundleLine -> {
      int start = manifestLines.indexOf(reqBundleLine);
      List<String> results = new ArrayList<>();
      results.add(getFirstResult(reqBundleLine));
      for (int i = start + 1; (i < manifestLines.size()); i++) {
        Matcher matcher = REQ_BUNDLE_PATTERN.matcher(manifestLines.get(i));
        if (matcher.matches()) {
          String group = matcher.group(1);
          results.add(group);
        } else {
          break;
        }
      }
      return results;
    }).orElse(Collections.emptyList()).stream().filter(this::accepts).collect(toList());
  }

  private String getFirstResult(String reqBundleLine) {
    int endIndex = getEndIndex(reqBundleLine, ',', ';');
    endIndex = endIndex > -1 ? endIndex : reqBundleLine.length();
    String firstResult = reqBundleLine.substring(REQ_BUNDLE_HEADER.length(), endIndex);
    return firstResult;
  }

  private Project getProject(List<String> manifestLines) {
    String name = "";
    String symbolicName = "";
    String version = "";
    for (String line : manifestLines) {
      int endIndex = getEndIndex(line, ',', ';');
      if (line.startsWith(BUNDLE_NAME_HEADER)) {
        name = line.substring(BUNDLE_NAME_HEADER.length(), endIndex);
      } else if (line.startsWith(BUNDLE_SYMBOLIC_NAME_HEADER)) {
        symbolicName = line.substring(BUNDLE_SYMBOLIC_NAME_HEADER.length(), endIndex);
      } else if (line.startsWith(BUNDLE_VERSION_HEADER)) {
        version = line.substring(BUNDLE_VERSION_HEADER.length(), endIndex);
      }
    }
    return accepts(symbolicName) ? Project.create(symbolicName, version, name) : null;
  }

  private boolean accepts(String bundleId) {
    return true; // TODO: add project filtering by regex
  }

  private Map<Project, List<Project>> replaceProjectIds(Map<Project, List<String>> results) {
    Map<Project, List<Project>> replacedResult = new HashMap<>(results.size());
    Map<String, Project> projectsMap = results.keySet().stream()
        .collect(toMap(project -> project.getBundleId(), Function.identity()));
    Set<Entry<Project, List<String>>> entrySet = results.entrySet();
    for (Entry<Project, List<String>> entry : entrySet) {
      List<Project> projects = entry.getValue().stream().map(id -> projectsMap.get(id)).filter(Objects::nonNull)
          .collect(toList());
      if (!projects.isEmpty()) {
        replacedResult.put(entry.getKey(), projects);
      }
    }
    return replacedResult;
  }

  private int getEndIndex(String string, char... characters) {
    int endIndex = string.length();
    for (int i = 0; i < characters.length; i++) {
      int indexOf = string.indexOf(characters[i]);
      if (indexOf > -1) {
        endIndex = Math.min(indexOf, endIndex);
      }
    }
    return endIndex;
  }
}
