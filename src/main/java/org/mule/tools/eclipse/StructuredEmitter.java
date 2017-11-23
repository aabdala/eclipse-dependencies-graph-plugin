package org.mule.tools.eclipse;

import java.util.function.Consumer;

class StructuredEmitter {

  private static final String NL = System.lineSeparator();
  private static final String T = "\t";

  private final String sectionStart;
  private final String sectionEnd;

  private int depth = 0;
  private StringBuilder builder = new StringBuilder();

  public StructuredEmitter(String sectionStart, String sectionEnd) {
    this.sectionStart = sectionStart;
    this.sectionEnd = sectionEnd;
  }

  public StructuredEmitter emitLine(Consumer<StringBuilder> lineBuildingFunction) {
    for (int i = 0; i < depth; i++) {
      builder.append(T);
    }
    lineBuildingFunction.accept(builder);
    builder.append(NL);
    return this;
  }

  public StructuredEmitter startSection(Consumer<StringBuilder> lineBuildingFunction) {
    emitLine(lineBuildingFunction.andThen(b -> b.append(" ").append(sectionStart)));
    depth++;
    return this;
  }

  public StructuredEmitter startSection(String section) {
    return this.startSection(b -> b.append(section));
  }

  public StructuredEmitter endSection() {
    depth--;
    emitLine(b -> b.append(sectionEnd));
    return this;
  }

  public String get() {
    return builder.toString();
  }
}