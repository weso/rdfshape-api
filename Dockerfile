## Build environment.

# Build application inside /app
# Resulting build in /app/target/universal/stage/bin/rdfshape
FROM openjdk:12 as build
WORKDIR /app

# Install git, sbt, required
RUN echo "Installing required programs..."
RUN yum update -y -q && \
    yum install git -y -q

RUN curl -s https://bintray.com/sbt/rpm/rpm | \
    tee /etc/yum.repos.d/bintray-sbt-rpm.repo && \
    yum install sbt -y -q

# ARGs - Override with: --build-arg [ARGUMENT]=[VALUE]
# Values in .env will not be taken into account!
# Permalink service creds.
ARG MONGO_DATABASE=""
ARG MONGO_USER=""
ARG MONGO_PASSWORD=""
# Needed at container runtime.
ENV MONGO_DATABASE=$MONGO_DATABASE
ENV MONGO_USER=$MONGO_USER
ENV MONGO_PASSWORD=$MONGO_PASSWORD
# Github token needed to download weso packages
ARG GITHUB_TOKEN=""
# Port for the app to run
ENV PORT=80
# Add rdfshape output-directory to path
ENV PATH /app/target/universal/stage/bin:$PATH

# Copy all application files and trigger build
RUN echo "Building rdfshape..."
COPY . ./
RUN sbt stage

# Run
RUN echo "Launching rdfshape..."
EXPOSE $PORT
CMD ["rdfshape", "--server", "-Dhttp.port=$PORT"]
RUN echo "Done"
