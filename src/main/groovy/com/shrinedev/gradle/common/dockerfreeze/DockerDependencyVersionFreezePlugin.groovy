package com.shrinedev.gradle.common.dockerfreeze

import java.io.Reader
import java.io.FileReader
import java.io.Writer
import java.io.FileWriter

import org.gradle.api.Plugin
import org.gradle.api.Project

class DockerDependencyVersionFreezePlugin implements Plugin<Project> {
    void apply (Project project) {
      project.configure(project) {
        extensions.create("shrineDocker", ShrineDockerExtension)
      }
      project.task('generateDockerLock', type: GenerateDockerLockTask)
	  if (project.hasProperty('build')) {
	    project.task('publishDocker', type: PublishDockerTask, dependsOn: project.build)
	    project.artifactoryPublish.dependsOn(project.publishDocker)
	  }

	  project.afterEvaluate {
	    def shrineDocker = project.shrineDocker
	    Properties props = new Properties()
        println 'Docker Dependency Lock File: ' + GenerateDockerLockTask.LOCK_FILE.getAbsolutePath()
        println 'Docker Dependency Lock File Exists? ' + GenerateDockerLockTask.LOCK_FILE.exists()
	    if (GenerateDockerLockTask.LOCK_FILE.exists()) {
	      Reader inStream = null
	      try {
	        inStream = new FileReader(GenerateDockerLockTask.LOCK_FILE)
	        props.load(inStream)
	      } finally {
	        if (inStream != null) {
	          inStream.close()
	        }
	      }
	    }
	    def lockFileLoaded = !props.isEmpty()
	    def dockerImages = [:];
	    if (shrineDocker.dependencies != null) {
	      for (String key : shrineDocker.dependencies.keySet()) {
	        String imageName
	        if (lockFileLoaded && props.containsKey(key)) {
	          imageName = props.get(key)
	        } else {
	          imageName = shrineDocker.dependencies.get(key)
	        }
	        dockerImages.put(key, imageName)
	      }
	    }
	    project.ext.dockerImages = dockerImages
	  }
    }
}
