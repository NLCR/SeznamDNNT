FROM openshift/base-centos7

MAINTAINER NKP <todo@nkp.cz>

## Maven version
ENV MVN_MAJOR=3
ENV MVN_VERSION=3.8.5

## Tomcat version
ENV TOMCAT_MAJOR 9
ENV TOMCAT_VERSION 9.0.59
ENV CATALINA_HOME /usr/local/tomcat
ENV JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF8

## Node js versions
ENV NODEJS=14.17.1



# check https://github.com/ceskaexpedice/kramerius/issues/470
ENV TOMCAT_TGZ_URL https://downloads.apache.org/tomcat/tomcat-$TOMCAT_MAJOR/v$TOMCAT_VERSION/bin/apache-tomcat-$TOMCAT_VERSION.tar.gz
ENV MAVEN_TGZ_URL https://dlcdn.apache.org/maven/maven-3/$MVN_VERSION/binaries/apache-maven-$MVN_VERSION-bin.tar.gz
ENV NODEJS_URL  https://nodejs.org/dist/v$NODEJS/node-v$NODEJS-linux-x64.tar.gz

# Set the labels that are used for Openshift to describe the builder image.
LABEL io.k8s.description="SDNNT" \
    io.k8s.display-name="SDNNT" \
    io.openshift.expose-services="8080:http" \
    io.openshift.tags="builder,sdnnt" \
    io.openshift.s2i.scripts-url="image:///usr/libexec/s2i"

RUN INSTALL_PKGS="tar zip" && \
    yum install -y --enablerepo=centosplus $INSTALL_PKGS && \
    rpm -V $INSTALL_PKGS && \
    yum clean all -y && \
	curl --insecure "$MAVEN_TGZ_URL" --output mvn.tar.gz && \
	curl --insecure "$NODEJS_URL" --output nodejs.tar.gz && \
	ls mvn.* nodejs.* && \
	tar xvzf mvn.tar.gz -C /usr/local && \ 
	tar xvzf nodejs.tar.gz -C /usr/local && \ 
	ls /usr/local && \
    rm mvn.tar.gz nodejs.tar.gz	

RUN  ln -sf /usr/local/apache-maven-$MVN_VERSION/bin/mvn /usr/local/bin/mvn
RUN  ln -sf /usr/local/node-v$NODEJS-linux-x64/bin/node /usr/local/bin/node
RUN  ln -sf /usr/local/node-v$NODEJS-linux-x64/bin/npm /usr/local/bin/npm

ADD adoptopenjdk.repo /etc/yum.repos.d/adoptopenjdk.repo 
RUN yum install -y adoptopenjdk-11-hotspot

WORKDIR $CATALINA_HOME

RUN  curl -fSL --insecure "$TOMCAT_TGZ_URL" -o tomcat.tar.gz && \
	tar -xvf tomcat.tar.gz --strip-components=1 && \
	rm bin/*.bat && \
	rm tomcat.tar.gz*

COPY  ["run", "assemble", "save-artifacts", "usage", "/usr/libexec/s2i/"]


ENV TOMCAT_USER tomcat
ENV TOMCAT_UID 8983
RUN groupadd -r $TOMCAT_USER && \
    useradd -r -u $TOMCAT_UID -g $TOMCAT_USER $TOMCAT_USER -d $HOME

RUN chown -R $TOMCAT_USER:$TOMCAT_USER $HOME $CATALINA_HOME
RUN chmod -R ugo+rwx $HOME $CATALINA_HOME

USER 8983
EXPOSE 8080

CMD ["/usr/libexec/s2i/usage"]
