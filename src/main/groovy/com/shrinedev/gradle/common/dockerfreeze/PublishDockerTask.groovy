package com.shrinedev.gradle.common.dockerfreeze

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class PublishDockerTask extends DefaultTask {
	
	@TaskAction
	def publishDocker () {
		def shrineDocker = project.shrineDocker
		if (shrineDocker == null || shrineDocker.artifacts == null) {
			return
		}
		for (String imageName : shrineDocker.artifacts) {
			if (!project.stableBuild) {  // If we are doing a stable build, we do not want to push to :latest
				getLogger().quiet("docker push ${project.dockerHost}/${imageName}")
				project.exec {
					commandLine "/bin/bash", "-c", "docker push ${project.dockerHost}/${imageName}"
				}
		    }
			getLogger().quiet("docker tag ${project.dockerHost}/${imageName} ${project.dockerHost}/${imageName}:${project.version}")
			project.exec {
				commandLine "/bin/bash", "-c", "docker tag ${project.dockerHost}/${imageName} ${project.dockerHost}/${imageName}:${project.version}"
			}
			getLogger().quiet("docker push ${project.dockerHost}/${imageName}:${project.version}")
			project.exec {
				commandLine "/bin/bash", "-c", "docker push ${project.dockerHost}/${imageName}:${project.version}"
			}
		}
	}
}
