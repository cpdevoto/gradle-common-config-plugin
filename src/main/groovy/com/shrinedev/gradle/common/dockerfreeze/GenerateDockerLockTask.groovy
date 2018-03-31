package com.shrinedev.gradle.common.dockerfreeze

import org.gradle.api.DefaultTask
import org.gradle.api.GradleScriptException
import org.gradle.api.tasks.TaskAction

class GenerateDockerLockTask extends DefaultTask {
    static final def VERSION_PATTERN = ~/(\d+).(\d+).(\d+)/
    static final File LOCK_FILE = new File('docker-dependencies.lock')
    

    @TaskAction
    def generateDockerLock () {
      def shrineDocker = project.shrineDocker
      if (shrineDocker.dependencies == null) {
        println "No Docker dependencies found."
        return
      }
      String dockerRepo = project.stableBuild ? "docker-stable" : "docker-develop"
      Properties props = new Properties()
      for (String key : shrineDocker.dependencies.keySet()) {
         String imageName = shrineDocker.dependencies.get(key)
         String version = getLatestVersion(dockerRepo, imageName)
         props.put(key, imageName + ":" + version) 
      }
      Writer outStream = null
      try {
        outStream = new FileWriter(LOCK_FILE)
        props.store(outStream, null)
      } finally {
        if (outStream != null) {
          outStream.close()
        }
      }
      getLogger().quiet(LOCK_FILE.getName() + " generated successfully!")
    }
    
    String getLatestVersion(String dockerRepo, String imageName) {
       def credentials = "${project.artifactory_user}:${project.artifactory_password}".getBytes().encodeBase64().toString()
       def url =  "https://maddogtechnology.artifactoryonline.com/maddogtechnology/api/docker/${dockerRepo}/v2/${imageName}/tags/list"
       def headers = [ Authorization: "Basic " + credentials ]
       def response = new URL(url).getText( requestProperties: headers)
       def parsedJson = new groovy.json.JsonSlurper().parseText(response)
       if (parsedJson.tags == null) {
         throw new GradleScriptException("Invalid response received when querying Artifactory for a list of tags associated with image ${imageName}:\n${response}", null)
       }
       def versions = []
       for (String tag : parsedJson.tags) {
         def m = tag =~ VERSION_PATTERN
         if (!m) {
           continue
         }
         versions.add(new Version(m.group(1), m.group(2), m.group(3)))
       }
       return versions.sort().get(versions.size() - 1)
    }
}
