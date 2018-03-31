# gradle-common-config-plugin
Gradle plugin for common build configuration

# Working with Docker Dependencies
When you are creating a project that has a dependency on a Shrine Docker artifact, you should observe the following conventions.  
Doing so will ensure that your Docker dependencies are properly frozen when each release is cut.  All of your Shrine Docker image
dependencies should be declared in a ```shrineDocker``` block with one entry for each dependency as shown below:

```
shrineDocker {
    dependencies = [
      migrations: 'migrations-app',
      postgres: 'postgres-schema'
    ]
}
```
Each dependency that you declare will consist of a key and an image name.  The key can be anything you want, while the image name should
correspond to the name of the Docker image as it appears within the Artifactory repository.  These image names should not be qualified with 
any tag names.

When you subsequently want to reference these image names from within, say, a ```docker run``` command that you execute within a task, you should 
reference the image name through a special ```dockerImages``` collection called as shown below:

```
task printDockerImageName << {
  println "Docker Image Name for Migrations Project: ${dockerImages.migration}"
}
```

The ```dockerImages.migration``` variable will contain a value of ```migrations-app``` within the development environment.  Once a release cut
has been performed, however, and the ```generateDockerLock``` gradle task has been run, the ```dockerImages.migration``` variable will contain 
a value of ```migrations-app:3.0.5```, assuming that ```3.0.5``` is the latest version of the ```migrations-app``` artifact.
