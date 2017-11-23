package org.mule.tools.eclipse;

public class Project {

  private final String bundleId;
  private final String version;
  private final String bundleName;

  private Project(String bundleId, String version, String bundleName) {
    this.bundleId = bundleId;
    this.version = version;
    this.bundleName = bundleName;
  }

  public static Project create(String bundleId, String version, String bundleName) {
    return new Project(bundleId, version, bundleName);
  }

  public String getBundleId() {
    return bundleId;
  }

  public String getVersion() {
    return version;
  }

  public String getBundleName() {
    return bundleName;
  }

  @Override
  public String toString() {
    return "Project [bundleId=" + bundleId + "]";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((bundleId == null) ? 0 : bundleId.hashCode());
    result = prime * result + ((bundleName == null) ? 0 : bundleName.hashCode());
    result = prime * result + ((version == null) ? 0 : version.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Project other = (Project) obj;
    if (bundleId == null) {
      if (other.bundleId != null)
        return false;
    } else if (!bundleId.equals(other.bundleId))
      return false;
    if (bundleName == null) {
      if (other.bundleName != null)
        return false;
    } else if (!bundleName.equals(other.bundleName))
      return false;
    if (version == null) {
      if (other.version != null)
        return false;
    } else if (!version.equals(other.version))
      return false;
    return true;
  }

}
