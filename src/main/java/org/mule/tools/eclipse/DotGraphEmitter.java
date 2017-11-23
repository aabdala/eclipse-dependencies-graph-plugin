package org.mule.tools.eclipse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;

public class DotGraphEmitter<T> implements GraphEmitter<T> {

  public static final String DIGRAPH = "digraph";
  public static final String DIGRAPH_CONNECTOR = "->";

  private final String graphKind;
  private final String connector;
  private final StructuredEmitter emitter;
  private final Function<T, String> nodePrinter;
  private final Map<String, Predicate<T>> clusteringCriteria;

  public static <T> DotGraphEmitter<T> graphEmitter(String graphKind, String connector, Function<T, String> nodePrinter,
      Map<String, Predicate<T>> clusteringCriteria) {
    return new DotGraphEmitter<T>(graphKind, connector, clusteringCriteria, nodePrinter);
  }

  public static <T> DotGraphEmitter<T> directedGraphEmitter(Function<T, String> nodePrinter,
      Map<String, Predicate<T>> clusteringCriteria) {
    return graphEmitter(DIGRAPH, DIGRAPH_CONNECTOR, nodePrinter, clusteringCriteria);
  }

  public static <T> DotGraphEmitter<T> directedGraphEmitter(Function<T, String> nodePrinter) {
    return directedGraphEmitter(nodePrinter, Collections.emptyMap());
  }

  public static <T> DotGraphEmitter<T> directedGraphEmitter(Map<String, Predicate<T>> clusteringCriteria) {
    return directedGraphEmitter(n -> n.toString(), clusteringCriteria);
  }

  public static <T> DotGraphEmitter<T> directedGraphEmitter() {
    return directedGraphEmitter(Collections.emptyMap());
  }

  private DotGraphEmitter(String graphKind, String connector, Map<String, Predicate<T>> clusteringCriteria,
      Function<T, String> nodePrinter) {
    this.graphKind = graphKind;
    this.nodePrinter = nodePrinter;
    this.connector = " " + connector + " ";
    this.clusteringCriteria = clusteringCriteria;
    this.emitter = new StructuredEmitter("{", "}");
  }

  @Override
  public String getGraph(Map<T, List<T>> dependencies, Map<String, String> options) {
    emitter.startSection(graphKind);

    options.entrySet().forEach(entry -> {
      emitter.emitLine(b -> b.append(entry.getKey()).append("=").append(entry.getValue()).append(";"));
    });

    emitClusters(dependencies);

    Set<Entry<T, List<T>>> entrySet = dependencies.entrySet();

    for (Entry<T, List<T>> entry : entrySet) {
      T key = entry.getKey();
      List<T> values = entry.getValue();
      emitter.emitLine(b -> {
        b.append("\"").append(printNode(key)).append("\"") //
            .append(connector).append(" { ");
        for (T value : values) {
          b.append("\"").append(printNode(value)).append("\"");

        }
        b.append(" };");
      });
    }
    emitter.endSection();

    return emitter.get();
  }

  private void emitClusters(Map<T, List<T>> dependencies) {
    Map<String, List<T>> clusters = getClusters(dependencies);
    Set<Entry<String, List<T>>> entrySet = clusters.entrySet();

    AtomicInteger i = new AtomicInteger();
    for (Entry<String, List<T>> entry : entrySet) {
      emitter.startSection(b -> b.append("subgraph ").append("cluster_").append(i.getAndIncrement()));
      emitter.emitLine(b -> b.append("label=\"").append(entry.getKey()).append("\";"));
      List<T> nodesInCluster = entry.getValue();
      emitter.emitLine(b -> {
        for (T node : nodesInCluster) {
          b.append("\"").append(printNode(node)).append("\"; ");
        }
      });
      emitter.endSection();
    }

  }

  private String printNode(T node) {
    return nodePrinter.apply(node);
  }

  private Map<String, List<T>> getClusters(Map<T, List<T>> dependencies) {
    Set<T> nodes = getAllNodes(dependencies);

    Map<String, List<T>> clusters = new HashMap<>(clusteringCriteria.size());
    clusteringCriteria.keySet().forEach(key -> clusters.put(key, new ArrayList<>()));
    Iterator<T> iterator = nodes.iterator();
    while (iterator.hasNext()) {
      T node = iterator.next();
      Optional<Entry<String, Predicate<T>>> matchingCriteria = clusteringCriteria.entrySet().stream()
          .filter(entry -> entry.getValue().test(node)).findFirst();
      matchingCriteria.ifPresent(criteriaEntry -> {
        clusters.get(criteriaEntry.getKey()).add(node);
        iterator.remove();
      });
    }

    if (!nodes.isEmpty()) {
      clusters.put("Other", new ArrayList<>(nodes));
    }

    return clusters;
  }

  private Set<T> getAllNodes(Map<T, List<T>> dependencies) {
    Set<T> nodes = new HashSet<>();
    nodes.addAll(dependencies.keySet());
    dependencies.values().stream().flatMap(each -> each.stream()).forEach(e -> nodes.add(e));
    return nodes;
  }

}
