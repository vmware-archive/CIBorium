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
package org.jenkinsci.plugins.ciborium;

import com.google.common.base.Strings;
import hudson.AbortException;
import hudson.Extension;
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

/**
 * A build step that pulls an image locally.  This build step delegates to {@code docker pull}.
 */
public final class DockerImagePullBuilder extends Builder {

  private String dockerImage;

  @DataBoundConstructor
  public DockerImagePullBuilder(final String dockerImage) {
    this.dockerImage = dockerImage;
  }

  /**
   * Jenkins will serialize this object to config.xml.  When loading from there {@link com.thoughtworks.xstream.XStreamer} is used.
   * This will load the object from XML using Java's Serialization.  That means that if you change the state of the object
   * between versions, then you need to handle migration from XML to code.  This method can be used for that.
   *
   * @return this or a new DockerImagePullBuilder object
   * @throws IOException if unable to create object
   */
  public Object readResolve() throws IOException {
    return this;
  }

  public boolean isDockerImageDefined() {
    return !Strings.isNullOrEmpty(dockerImage);
  }

  /**
   * Get the current value of dockerImage.  This may be null.
   *
   * Jenkins note: this is needed in order for the value to be sent back to the UI.  If you don't have
   * the getter for each field, then the UI will mark it empty and the next save will make the value empty.
   */
  public String getDockerImage() {
    return dockerImage;
  }

  @Override
  public boolean perform(final AbstractBuild<?, ?> build, final Launcher launcher, final BuildListener listener)
      throws InterruptedException, IOException {
    if (isDockerImageDefined()) {
      final String imageName = getDockerImage();

      listener.getLogger().printf("Attempting to pull image '%s' from docker repository\n", imageName);

      final Proc ref = launcher.launch()
          .cmds(
              "docker",
              "pull",
              imageName
          ).stderr(listener.getLogger()).stdout(listener.getLogger())
          .pwd(build.getModuleRoot())
          .start();
      final int code = ref.join();
      if (code != 0) {
        throw new AbortException("Unable to pull docker image: '"+imageName+"'");
      }
    } else {
      throw new AbortException("No docker image defined.  The 'Pull Docker image' build step requires a image.");
    }
    return true;
  }

  @Extension
  public static final class DockerPullBuilderDescriptor extends BuildStepDescriptor<Builder> {

    @Override
    public boolean isApplicable(final Class<? extends AbstractProject> jobType) {
      return FreeStyleProject.class.isAssignableFrom(jobType);
    }

    @Override
    public String getDisplayName() {
      return "Pull Docker image";
    }
  }
}
