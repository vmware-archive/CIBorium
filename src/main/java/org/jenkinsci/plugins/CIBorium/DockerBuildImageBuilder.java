/*
 * Copyright (C) 2013-2014 Pivotal Software, Inc.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *     * Neither the name of Pivotal Software, Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.jenkinsci.plugins.CIBorium;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.FreeStyleProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.List;

/**
 * Creates a docker image.
 * <p/>
 * In jenkins there a few different use-cases for how we get the Dockerfile.
 * <ul>
 * <li>Dockerfile is in the project checked out.</li>
 * <li>Unable to add a Dockerfile to a project, so define in jenkins.</li>
 * </ul>
 * This list maps to the {@link #dockerFile}, and {@link #dockerContent}
 * settings.  One interesting thing about Dockerfiles is that there are two ways to read them:
 * <p/>
 * <ul>
 * <li>Path is a directory, Dockerfile is under this directory.</li>
 * <li>Read from stdin.</li>
 * </ul>
 * This needs to be called out because of docker's idea of a context (a location to interact
 * with the builder's file system).  When you point to a directory, everything under that directory
 * can be used with the {@code ADD} instruction in docker, but nothing before it ({@code ..}).  When
 * you use stdin, then the context is not defined, so {@code ADD} from local disk is no longer
 * supported.
 */
public final class DockerBuildImageBuilder extends Builder {
  private static final Joiner COMMAND_JOINER = Joiner.on(" ").skipNulls();

  private String dockerFile;
  private String dockerImage;
  private String dockerContent;

  @DataBoundConstructor
  public DockerBuildImageBuilder(final String dockerFile, final String dockerImage, final String dockerContent) {
    this.dockerFile = dockerFile;
    this.dockerImage = dockerImage;
    this.dockerContent = dockerContent;
  }

  /**
   * Jenkins will serialize this object to config.xml.  When loading from there {@link com.thoughtworks.xstream.XStreamer} is used.
   * This will load the object from XML using Java's Serialization.  That means that if you change the state of the object
   * between versions, then you need to handle migration from XML to code.  This method can be used for that.
   *
   * @return this or a new DockerBuildImageBuilder object
   * @throws IOException if unable to create object
   */
  public Object readResolve() throws IOException {
    return this;
  }

  /**
   * Get the current value of dockerFile. This may be null.
   */
  public String getDockerFile() {
    return dockerFile;
  }

  /**
   * Get the current value of dockerImage. This may be null.
   */
  public String getDockerImage() {
    return dockerImage;
  }

  /**
   * Get the content to use as a Dockerfile. This may be null.
   */
  public String getDockerContent() {
    return dockerContent;
  }

  private String getDockerFileOr(final String defaultValue) {
    if (Strings.isNullOrEmpty(dockerFile)) return defaultValue;
    else return dockerFile;
  }

  private String getDockerImageOr(final String defaultValue) {
    if (Strings.isNullOrEmpty(dockerImage)) return defaultValue;
    else return dockerImage;
  }

  @Override
  public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
      throws InterruptedException, IOException {
    final String buildFile = getDockerFileOr(".");
    final String imageName = getDockerImageOr(CIBorium.getImageName(build));

    listener.getLogger().printf("Attempting to create Docker image '%s' with build file '%s'\n", imageName, buildFile);

    final List<String> cmd = Lists.newArrayList(
        "docker",
        "build",
        "-t",
        "\"" + imageName + "\""
    );

    if (!Strings.isNullOrEmpty(dockerContent)) {
      // user defined content, so use that for the image
      cmd.addAll(useContent());
    } else {
      // docker only takes a directory containing a Dockerfile or - and stdin will contain the file
      if (new FilePath(launcher.getChannel(), buildFile).isDirectory()) {
        cmd.addAll(useDirectory(buildFile));
      } else {
        cmd.addAll(useFile(buildFile));
      }
    }

    // java doesn't run command in a shell so the pipe will be an argument.  Putting cmd in a shell will let me use <
    final Proc ref = launcher.launch().
        cmds("/bin/sh", "-c", COMMAND_JOINER.join(cmd))
        .stderr(listener.getLogger()).stdout(listener.getLogger()) //TODO find better way to handle stderr
        .pwd(build.getModuleRoot())
        .start();
    final int code = ref.join();
    if (code != 0) {
      if (!Strings.isNullOrEmpty(dockerContent)) {
        throw new AbortException("Unable to build docker content: '" + dockerContent + "'");
      } else {
        throw new AbortException("Unable to build docker file: '" + buildFile + "'");
      }
    }

    return true;
  }

  private List<String> useContent() {
    return ImmutableList.of("-", "<<EOF\n" + dockerContent + "\nEOF");
  }

  private List<String> useDirectory(final String buildFile) {
    return ImmutableList.of(buildFile);
  }

  private List<String> useFile(final String buildFile) {
    //TODO file bug against docker to support files
    return ImmutableList.of("-", "<", buildFile);
  }


  @Extension
  public static final class DockerBuildStepDescriptor extends BuildStepDescriptor<Builder> {

    @Override
    public boolean isApplicable(final Class<? extends AbstractProject> jobType) {
      return FreeStyleProject.class.isAssignableFrom(jobType);
    }

    @Override
    public String getDisplayName() {
      return "Build Docker image";
    }
  }
}
