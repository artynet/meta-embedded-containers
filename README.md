# Meta-embedded-containers layer

## Non-maintenance notice

This layer was provided as proof-of-concept code to support a series
of articles and a talk given in 2020, and is not actively maintained.

Articles:
* [Containers on Linux embedded systems](https://blog.savoirfairelinux.com/en-ca/2020/containers-on-linux-embedded-systems/)
* [Embed a container image into a Yocto rootfs](https://blog.savoirfairelinux.com/en-ca/2020/how-to-embed-a-docker-image-into-yoctos-root-filesystem/)

Videos:
* Integrating Docker containers in Yocto - Yocto Project Virtual
  Summit Europe, October 2020. [link to video](https://www.youtube.com/watch?v=rrYnYJ8LxWM)


## Description

The meta-embedded-containers provides two approaches to embed Docker
container(s) into a target root filesystem with Yocto.

The first approach is to embed a Docker archive in the root filesystem.

The second approach is to populate the Docker store (i.e.
/var/lib/docker directory) into the target root filesystem.

## Dependencies

URI: git://git.openembedded.org/meta-openembedded
Branch: dunfell

URI: git://git.yoctoproject.org/cgit/cgit.cgi/poky
Branch: dunfell

## Adding the meta-embedded-container layer to your build

Please run the following command:

$ bitbake-layers add-layer meta-embedded-containers

## Documentation
)
Three blog papers discuss about embedding a container image(s) inside
Yocto root filesystem.

https://blog.savoirfairelinux.com/en-ca/2020/containers-on-linux-embedded-systems/
https://blog.savoirfairelinux.com/en-ca/2020/integrating-container-image-in-yocto/

## Customize your image recipe

The image recipe is located under
`recipes-core/images/embedded-container-image.bb` file. In the
IMAGE_INSTALL Bitbake variable, you can customize which kind of approach
you want:
- container-image: pull the container image(s) and install the Docker
  store in the target root filesystem,
- container-archive: pull the container image(s) and save the container
  archive(s) in the rootfs,
- container-multiple-archives: pull the container image(s), save them in
  the rootfs as archive files and start them with docker-compose at
boot.
