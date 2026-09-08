# syntax=docker/dockerfile:1
# Match the tool versions in the original S2I builder.
FROM node:14.17.1-buster AS node
FROM maven:3.8.5-openjdk-11 AS build
COPY --from=node /usr/local/bin/node /usr/local/bin/node
COPY --from=node /usr/local/lib/node_modules /usr/local/lib/node_modules
RUN ln -s /usr/local/lib/node_modules/npm/bin/npm-cli.js /usr/local/bin/npm
WORKDIR /workspace
COPY sdnnt/src/main/ui/package*.json sdnnt/src/main/ui/
RUN --mount=type=cache,target=/root/.npm cd sdnnt/src/main/ui && npm install
COPY pom.xml ./
COPY openapi/ openapi/
COPY sdnnt/ sdnnt/
# The Maven git-commit-id plugin reads the checkout metadata.
COPY .git/ .git/
# Windows checkouts may contain CRLF, while Spotless runs in this Linux stage.
RUN find openapi sdnnt -type f -name '*.java' -exec sed -i 's/\r$//' {} +
RUN --mount=type=cache,target=/root/.m2 mvn -B clean install --file pom.xml

FROM tomcat:9.0.63-jdk11-temurin
ENV JAVA_OPTS="-Duser.home=/opt/app-root/src -Dfile.encoding=UTF-8"
RUN groupadd -g 8983 sdnnt && useradd -u 8983 -g sdnnt -m -d /opt/app-root/src sdnnt \
    && mkdir -p /opt/app-root/src/.sdnnt /opt/app-root/src/bin \
    && chown -R sdnnt:sdnnt /usr/local/tomcat /opt/app-root/src
COPY --from=build --chown=sdnnt:sdnnt /workspace/sdnnt/target/sdnnt-*.war /usr/local/tomcat/webapps/sdnnt.war
USER 8983
EXPOSE 8080
CMD ["catalina.sh", "run"]
