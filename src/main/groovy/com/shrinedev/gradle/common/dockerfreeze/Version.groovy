package com.shrinedev.gradle.common.dockerfreeze

class Version implements Comparable<Version> {
  int majorVersion
  int minorVersion
  int buildNumber
  
  public Version (String majorVersion, String minorVersion, String buildNumber) {
    this.majorVersion = majorVersion.toInteger()
    this.minorVersion = minorVersion.toInteger()
    this.buildNumber = buildNumber.toInteger()
  }
  
  public int compareTo (Version other) {
    int result = this.majorVersion - other.majorVersion
    if (result != 0) {
      return result
    }
    result = this.minorVersion - other.minorVersion
    if (result != 0) {
      return result
    }
    result = this.buildNumber - other.buildNumber
    return result
  }
  
  public String toString () {
    return majorVersion + "." + minorVersion + "." + buildNumber
  }
} 
