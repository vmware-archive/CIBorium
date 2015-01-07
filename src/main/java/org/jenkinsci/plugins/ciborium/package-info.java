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

/**
 * Jenkins integration with Docker.
 * <p />
 * Common Jenkins plugin comments:
 * <ul>
 *   <li>All plugin code is called a {@link hudson.ExtensionPoint}</li>
 *   <li>Extensions are serialized to disk (in XML), and read back from disk</li>
 *   <li>Serialization mimics java's serialization, which uses {@code readResolve} rather than
 *   the defined constructor.</li>
 *   <li>'final' can only be used in a extension if and only if its defined in-place and not from a constructor.</li>
 *   <li>Adding new state to a extension needs to handle the null case, where the current
 *   serialized form doesn't have the field.</li>
 *   <li>Getters are required for the UI to define fields.  All user facing configurations need to
 *   have a getter that corresponds to it.</li>
 * </ul>
 * <p />
 * Build Wrapper comments:
 * <ul>
 *   <li>Life-cycle doesn't match intuition:
 *   {@link hudson.tasks.BuildWrapper#decorateLauncher(hudson.model.AbstractBuild, hudson.Launcher, hudson.model.BuildListener)}
 *   is called before {@link hudson.tasks.BuildWrapper#setUp(hudson.model.AbstractBuild, hudson.Launcher, hudson.model.BuildListener)}</li>
 * </ul>
 */
package org.jenkinsci.plugins.ciborium;
