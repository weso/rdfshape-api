## Build environment.
# Build application inside /app
FROM openjdk:12 as build
WORKDIR /app

# Install git, sbt, required
RUN yum update -y -q && \
    yum install git -y -q

RUN curl -s https://bintray.com/sbt/rpm/rpm | \
    tee /etc/yum.repos.d/bintray-sbt-rpm.repo && \
    yum install sbt -y -q

# ARGs - Override with: --build-arg [ARGUMENT]=[VALUE]
# Github token needed to download weso packages at build time.
ARG GITHUB_TOKEN=""

# Copy all application files
COPY . ./
# Build to /app/target/universal/rdfshape.zip
RUN ["sbt", "Universal / packageBin"]

## Prod environment.
FROM openjdk:12 as prod
WORKDIR /app

# Copy zip with universal executable
COPY --from=build /app/target/universal/rdfshape.zip .

# Download required programs dependencies. Unzip binaries.
RUN yum update -y -q && \
    yum install graphviz -y -q && \
    yum install unzip -y -q && \
    unzip -q rdfshape.zip

# Add rdfshape to path
ENV PATH /app/rdfshape/bin:$PATH

# Run
# Port for the app to run
ENV PORT=80
EXPOSE $PORT
CMD ["rdfshape", "--server", "-Dhttp.port=$PORT", "-Djdk.tls.client.protocols=TLSv1.2"]
