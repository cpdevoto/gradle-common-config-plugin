package com.shrinedev.gradle.common.config

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.shrinedev.gradle.common.dockerfreeze.DockerDependencyVersionFreezePlugin

class GradleBuildConfigurationPlugin implements Plugin<Project> {
    Logger logger = LoggerFactory.getLogger(GradleBuildConfigurationPlugin.class.name)

    void apply(Project project) {


        logger.quiet('Applying '+this.class.name+' to project '+project.name);
        def buildNum = project.hasProperty('buildNum') ? project.buildNum : '0'
        def releaseNum = project.hasProperty('releaseNum') ? project.releaseNum : '0'
        def baseVersion = project.hasProperty('baseVersion') ? project.baseVersion : '1'

        project.plugins.apply("com.jfrog.artifactory")
        project.plugins.apply("nebula.dependency-lock")
        project.plugins.apply('jacoco')

        // majorVersion should be read from gradle.properties, defaulting to 1; majorVersion should be set to 3 for all
        // 3.0 projects.
        def majorVersion = baseVersion
        def build = System.getenv("BUILD_NUMBER") ? System.getenv("BUILD_NUMBER") : buildNum ?: { ->
            def user = System.getenv("USER");
            return "dev-${user}";
        }
        def minorVersion = releaseNum

        def calculatedVersion = "${majorVersion}.${minorVersion}.${build}"
        logger.quiet('Setting project version to ' + calculatedVersion)
        project.version = calculatedVersion

        // 3.0 projects will use pure wildcards (i.e. ${baseVersion}.+)
        def calculatedRbiDepVersion
        calculatedRbiDepVersion = "${majorVersion}.+" 
        logger.quiet('Setting rbiDepVersion to ' + calculatedRbiDepVersion)
        project.ext.rbiDepVersion=calculatedRbiDepVersion

        // Determine whether the currently checked out branch in git is the "develop" branch, or a release branch, and set a boolean var accordingly
        def stableBuild
        def branch = ""
        def proc = "git rev-parse --abbrev-ref HEAD".execute()
        proc.in.eachLine { line -> branch = line }
        proc.err.eachLine { line -> println line }
        proc.waitFor()

        if (branch == null) {
          throw new RuntimeException("A problem occurred while attempting to read the name of the current branch")
        } else if (branch.equals("develop")) {
          stableBuild = false
        } else if (branch.startsWith("release") || branch.startsWith("sprint")) {
          stableBuild = true
        } else {
		  branch = System.getenv("GIT_BRANCH")
		  if (branch != null && branch.startsWith("origin/release")) {
			  stableBuild = true
		  } else {
              logger.quiet "WARN: Unrecognized git branch name " + branch + "; we will assume that this is a development build"
              stableBuild = false
		  }
        }
        logger.quiet('Setting stableBuild to ' + stableBuild)
        project.ext.stableBuild = stableBuild

        def stableRepoKey = "shrine-stable"
        def developRepoKey = "shrine-develop"
        def stableDockerRepoKey = "docker-stable"
        def developDockerRepoKey = "docker-develop"
        project.ext.dockerHost = "shrinedevelopment-" + (stableBuild ? stableDockerRepoKey : developDockerRepoKey) + ".jfrog.io"
        logger.quiet('Setting dockerHost to ' + project.ext.dockerHost)
        // It seems that there should be a different resolve repo for develop as opposed to stable, to ensure that dependency wildcards are resolved correctly!
        def resolveRepoKey = stableBuild ? "stable-repo" : "develop-repo"
        logger.quiet('Setting resolveRepoKey to ' + resolveRepoKey)

        project.artifactory {
            contextUrl = "${shrine_artifactory_contextUrl}"
            //The base Artifactory URL if not overridden by the publisher/resolver
            publish {
                repository {
                    repoKey = developRepoKey
                    username = "${shrine_artifactory_user}"
                    password = "${shrine_artifactory_password}"
                }
                repository {
                    repoKey = stableRepoKey
                    username = "${shrine_artifactory_user}"
                    password = "${shrine_artifactory_password}"
                }
                repository {
                    repoKey = developDockerRepoKey
                    username = "${shrine_artifactory_user}"
                    password = "${shrine_artifactory_password}"
                }
                repository {
                    repoKey = stableDockerRepoKey
                    username = "${shrine_artifactory_user}"
                    password = "${shrine_artifactory_password}"
                }
            }
            resolve {
                repository {
                    repoKey = resolveRepoKey
                    username = "${shrine_artifactory_user}"
                    password = "${shrine_artifactory_password}"
                }
            }
        }


        project.artifactoryPublish {
            // NOTE: All docker artifacts will be published out of band via shell commands.
            // For now, we will assume that publications to the docker repo will be done through shell commands, not through artifactoryPublish
            project.clientConfig.publisher.repoKey = stableBuild ? stableRepoKey : developRepoKey
        }

		project.plugins.apply(DockerDependencyVersionFreezePlugin)
		
		project.ext.findFreePort = {
			->
		    try {
		      socket = new ServerSocket(0);
		      socket.setReuseAddress(true);
		      int port = socket.getLocalPort();
		      try {
		        socket.close();
		      } catch (IOException e) {
		        // Ignore IOException on close()
		      }
		      return port;
		    } catch (IOException e) {
		    } finally {
		      if (socket != null) {
		        try {
		          socket.close();
		        } catch (IOException e) {
		        }
		      }
		    }
		    throw new IllegalStateException("Could not find a free TCP/IP port to start embedded Jetty HTTP Server on");
		}
    }
}
