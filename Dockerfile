# Copyright (C) 2013-2014 Pivotal Software, Inc.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the under the Apache License,
# Version 2.0 (the "License?); you may not use this file except in compliance
# with the License. You may obtain a copy of the License at
#
# https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Server-Run
# 
# VERSION               1

FROM ubuntu:13.10

RUN apt-get update -y

## fork of https://github.com/jpetazzo/dind/
## Forked since it seems the image is not updated, so was unable to install java 7 and jenkins
RUN \
  apt-get install -y wget && \
  wget -q -O - http://pkg.jenkins-ci.org/debian/jenkins-ci.org.key | apt-key add - && \
  echo "deb http://pkg.jenkins-ci.org/debian/ binary/" > /etc/apt/sources.list.d/jenkins.list && \
  apt-get install -y jenkins openjdk-7-jdk iptables ca-certificates lxc

ADD https://get.docker.io/builds/Linux/x86_64/docker-latest /usr/local/bin/docker
ADD https://raw.githubusercontent.com/jpetazzo/dind/master/wrapdocker /usr/local/bin/wrapdocker
RUN chmod +x /usr/local/bin/docker /usr/local/bin/wrapdocker
VOLUME /var/lib/docker

RUN \
  sed -i 's;127.0.0.1;0.0.0.0;g' /etc/default/jenkins && \
  mkdir -p /var/lib/jenkins/.jenkins/plugins && \
  chown -R jenkins: /var/lib/jenkins && \
  mkdir -p /var/run/jenkins && \
  chown -R jenkins: /var/run/jenkins && \
  groupadd docker && \
  usermod -a -G docker jenkins

# runs docker in the background.  Required for jenkins to run docker commands
ADD ./wrapdocker /usr/local/bin/wrapdocker

# jenkins runs on port 8080
EXPOSE 8080

# allow jenkins to run docker commands

ADD target/CIBorium.hpi /var/lib/jenkins/.jenkins/plugins/CIBorium.hpi

ENTRYPOINT [ "/bin/bash", "-c" ]
CMD /usr/local/bin/wrapdocker && source /etc/default/jenkins && /bin/su -l $JENKINS_USER -c 'source /etc/default/jenkins && $JAVA $JAVA_ARGS -jar $JENKINS_WAR $JENKINS_ARGS'
