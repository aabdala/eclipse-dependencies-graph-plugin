package org.mule.tools.eclipse;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.apache.commons.io.FileUtils;

public class ProjectDependenciesGraphPrinter {

  /*-
   * Example main method demonstrating usage. Should pass the following program arguments:
   * 
   * args[0] = absolute file path of eclipse project root folder 
   * args[1] = optional output folder for the dot graph contents
   * 
   * @param args
   * @throws IOException
   */
  public static void main(String[] args) throws IOException {
    File projectRoot = new File(args[0]);
    File output = args.length > 1 ? new File(args[1]) : null;

    Map<String, String> options = new HashMap<>();
    options.put("rankdir", "LR");
    options.put("splines", "polyline");

    DotGraphEmitter<Project> graphEmitter = DotGraphEmitter.directedGraphEmitter(//
        p -> p.getBundleId(), //
        getClusteringCriteria()//
    );

    Map<Project, List<Project>> dependencies = new EclipseDependenciesCollector().getDependencies(projectRoot);

    String graph = graphEmitter.getGraph(dependencies, options);

    System.out.println(graph);

    if (output != null) {
      FileUtils.writeStringToFile(output, graph, Charset.defaultCharset(), false);
    }
  }

  private static Map<String, Predicate<Project>> getClusteringCriteria() {
    Map<String, Predicate<Project>> clusteringCriteria = new HashMap<>(2);
    clusteringCriteria.put("Test projects", project -> project.getBundleId().contains(".test"));
    clusteringCriteria.put("UI projects", project -> project.getBundleId().contains(".ui"));
    return clusteringCriteria;
  }

}
