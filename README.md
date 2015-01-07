# CIBorium

CIBorium is a [Jenkins](http://jenkins-ci.org/) plugin that integrates with [Docker](https://www.docker.com/). This will let you create and publish docker images, and will also let you run build steps inside a docker container.

## Licensing

CiBorium is licensed under 3-clause BSD license. More information can be found inside COPYING file.

## Background

Jenkins is a wonderful system for managing builds and complex continuous integration workflows. Unfortunately, as the number of jobs grows and the more heterogeneous your software gets, the harder it is to make sure builds are repeatable.

CIBorium attempts to solve this problem by letting your project define what the build environment should look like.  Using Dockerfiles and docker images, builds can choose what software is installed on the environment running the build.

For example, your project might need install level dependencies in order to run (lib files, dbs for integration testing, etc.).  In a traditional Jenkins environment, you would ether install the software across all slaves, or you have a set of slaves running different software and builds tied to those slaves.  With the CIBorium plugin, the project is able to define what the runtime operating system is, whats installed on it, and the configurations.

## Features

   * Isolate builds inside docker containers
   * Create docker images
   * Pull docker images locally

## Docker Environment

Docker environment is used to run build steps inside a docker container; each build step will run in different containers, but with the same image.

![Docker Environment](https://github.com/pivotalsoftware/CIBorium/blob/master/docs/assets/DockerEnvironment.png)

The default image used is `jenkins/<build name>` which matches the docker image build step. By convention, the first build step for a project is to build the image that this environment will run with.

![Docker Environment and Build Step](https://github.com/pivotalsoftware/CIBorium/blob/master/docs/assets/DockerEnvironmentWithBuildImage.png)

When the build steps are executed, a few different paths are mounted into the container; this is both for simplicity and necessity. For one, the `$WORKSPACE` for the project will be mounted and set as the working directory for the container. This lets builds look the same as if they were run without docker support. The second directory mounted is `/tmp` which is needed because of how jenkins works. When jenkins builds the script that runs the real build, this script is placed in `/tmp`. To make sure that this all works, this directory is mounted. This implies that different builds could influence each other because of the shared directory. Over time this limitation should be removed.

## Jenkins DSL Integration

The plan is to add docker DSLs into the Jenkins DSL project, but that is not there yet.  Until that happens, the following will do the trick.

Build a docker image.

```
job {
  configure {
    it / 'builders' << 'org.jenkinsci.plugins.ciborium.DockerBuildImageBuilder' {
      // file to use.  If this is a directory, then a 'Dockerfile' must found there.  If this is a 
      // file, then the content is piped into docker build
      // Defaults to '.'
      dockerFile()
      // image name to create.
      // Defaults to 'jenkins:<project's name>'
      dockerImage()
      // the content of the Dockerfile to use. This is useful for building source that does
      // not include a docker file or image already.
      // this field is the text content inside of a Dockerfile
      dockerContent()
    }
  }
}

```

Isolate the build inside a docker container

```
it / 'buildWrappers' / 'org.jenkinsci.plugins.ciborium.DockerBuildWrapper' {
  // image name to run in. Most projects build the image as the first build step.
  // Defaults to 'jenkins:<project's name>'
  dockerImage()
}

```

## Building

To build the plugin from source

```
mvn clean package hpi:hpi
```

To run a local jenkins with the plugin installed

```
mvn clean package hpi:run
```

## Run Inside Docker

To test inside a docker container, build the image provided in this repo

```bash
docker build -t "jenkins/server" .
```

This will include the latest hpi file generated, so make sure to build first.

To run jenkins

```
# privileged lets docker run docker
docker run -d --volume /var/lib/docker:/var/lib/docker --privileged=true -p 8080:8080 jenkins/server
```

If using boot2docker on mac, you can view the container by going to http://localhost:8080 if you port-forward

```
boot2docker ssh -L 8080:localhost:8080
```
