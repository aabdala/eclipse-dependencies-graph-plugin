package org.mule.tools.eclipse;

import java.util.List;
import java.util.Map;

public interface GraphEmitter<T> {

  String getGraph(Map<T, List<T>> dependencies, Map<String, String> options);

}