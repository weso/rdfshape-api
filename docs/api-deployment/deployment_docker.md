---
id: deployment_docker
title: Running with Docker
---

# Running with Docker

## Requirements
* Docker must be installed in the machine that will build the images and/or run the containers.

## Building the image
* Pull from our [container registry](@API_CONTAINER_REGISTRY@) or... 
* Use the provided Dockerfile to build @APP_NAME@ images: 
  1. Run `docker build -t {YOUR_IMAGE_NAME} .` from the project folder.
  > No build arguments are required.

## Running containers

* When running a container, you may provide the following environment variables via `--env`:
    - `PORT` [optional]:
        - Port where the API is exposed inside the container. Default is 8080.
    - `USE_HTTPS` [optional]:
        - Any non-empty value to try to serve via HTTPS, leave undefined for HTTP.

## Supported tags

- _:stable_: Stable build updated manually.
- <_:hashed_tags_>: Automated builds by our CI pipeline. With the latest features uploaded to our repository but lacking
  internal testing.

## Serving with HTTPS

Follow the indications above.