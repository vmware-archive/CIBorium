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

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import hudson.model.AbstractBuild;
import hudson.model.Node;

public final class CIBorium {
  private static final Splitter LIST_SPLITTER = Splitter.on(CharMatcher.WHITESPACE).omitEmptyStrings();

  private static final String IMAGE_PREFIX = "jenkins/";
  private static final String DEFAULT_HOSTNAME = "jenkins.docker.io";

  private CIBorium() {
  }

  /**
   * Creates a string representing the build.  For all builds under a given project, the same
   * string will be returned.
   * <p/>
   * The format of the string is {@code jenkins/projectName}
   */
  public static String getImageName(final AbstractBuild build) {
    return IMAGE_PREFIX + getName(build);
  }

  /**
   * Returns the name of the project.  This is equivalent to calling {@code build.getProject().getName()}.
   */
  public static String getName(final AbstractBuild build) {
    return build.getProject().getName();
  }

  /**
   * Attempt to extract the hostname from the node.  If the hostname can not be extracted, then
   * {@link #DEFAULT_HOSTNAME} is returned.
   */
  public static String getHostname(final Node builtOn) {
    if (builtOn == null || Strings.isNullOrEmpty(builtOn.getDisplayName())) {
      return DEFAULT_HOSTNAME;
    } else {
      return builtOn.getDisplayName();
    }
  }

  /**
   * Converts a string like instance into a list of strings.  The string is split off whitespace
   * and no empty strings will be in the list.
   */
  public static ImmutableList<String> tokenize(final CharSequence input) {
    if (input == null) return ImmutableList.of();
    return FluentIterable.from(LIST_SPLITTER.split(input)).toList();
  }
}
