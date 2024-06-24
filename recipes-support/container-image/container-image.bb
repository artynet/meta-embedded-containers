SUMMARY = "Embed Docker store in the system"
DESCRIPTION = "Pull the container image(s) and install the Docker store"

LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COREBASE}/meta/COPYING.MIT;md5=3da9cfbcb788c80a0384361b4de20420"

DOCKER_STORE = "${WORKDIR}/docker-store"
DOCKER_PID = "/tmp/yocto-docker.pid"
DOCKER_SOCKET = "/tmp/yocto-docker.sock"

MANIFEST = "images.manifest"

inherit systemd
SYSTEMD_SERVICE_${PN} = "container-image.service"
SYSTEMD_AUTO_ENABLE_${PN} = "enable"

RDEPENDS_${PN} += "docker-ce bash mount-noauto"

SRC_URI = "file://container-image.service \
           file://container-image.sh \
           file://images.manifest \
          "

do_pull_image[nostamp] = "1"
do_pull_image[network] = "1"
do_package_qa[noexec] = "1"
INSANE_SKIP_${PN}_append = "already-stripped"
EXCLUDE_FROM_SHLIBS = "1"

do_pull_image() {
    [ -f "${WORKDIR}/${MANIFEST}" ] || bbfatal "${MANIFEST} does not exist"

    sudo rm -rf "${DOCKER_STORE}"/*

    # Kill docker and wait for it to die.
    while [ -f ${DOCKER_PID} ] && sudo /bin/kill -0 "$(cat ${DOCKER_PID})" 2>&1 > /dev/null ; do
        sudo /bin/kill "$(cat ${DOCKER_PID})"
        sleep 1.0
    done
    [ -f ${DOCKER_PID} ] && /bin/rm -rf ${DOCKER_PID}

    # Start the dockerd daemon with the driver vfs in order to store the
    # container layers into vfs layers. The default storage is overlay
    # but it will not work on the target system as /var/lib/docker is
    # mounted as an overlay and overlay storage driver is not compatible
    # with overlayfs.
    sudo /usr/bin/dockerd --storage-driver vfs --data-root "${DOCKER_STORE}" \
        --pidfile ${DOCKER_PID} -H unix://${DOCKER_SOCKET} &

    # Wait for daemon to be ready.
    for i in {1..6}; do
        if /usr/bin/docker -H unix://${DOCKER_SOCKET} info; then
            break;
        fi
        sleep 5
    done

    if ! /usr/bin/docker -H unix://${DOCKER_SOCKET} info; then
        bbfatal "Error launching docker daemon"
    fi

    local name version tag
    while read -r name version tag _; do
        if ! sudo /usr/bin/docker -H unix://${DOCKER_SOCKET} pull --platform linux/arm "${name}:${version}"; then
            bbfatal "Error pulling ${name}"
        fi
    done < "${WORKDIR}/${MANIFEST}"

    sudo /bin/chown -R "${USER}" "${DOCKER_STORE}"

    # Clean temporary folders in the docker store.
    /bin/rm -rf "${DOCKER_STORE}/runtimes"
    /bin/rm -rf "${DOCKER_STORE}/tmp"

    # Kill dockerd daemon after use.
    sudo /bin/kill "$(cat ${DOCKER_PID})"
    sudo /bin/rm -rf ${DOCKER_SOCKET} ${DOCKER_PID}
}

do_install() {
    install -d "${D}${systemd_unitdir}/system"
    install -m 0644 "${WORKDIR}/container-image.service" "${D}${systemd_unitdir}/system/"

    install -d "${D}${bindir}"
    install -m 0755 "${WORKDIR}/container-image.sh" "${D}${bindir}/container-image"

    install -d "${D}${datadir}/container-images"
    install -m 0400 "${WORKDIR}/${MANIFEST}" "${D}${datadir}/container-images/"

    install -d "${D}${localstatedir}/lib/docker"
    cp -R "${DOCKER_STORE}"/* "${D}${localstatedir}/lib/docker/"
}

FILES_${PN} = "\
    ${system_unitdir}/system/container-image.service \
    ${bindir}/container-image \
    ${datadir}/container-images/${MANIFEST} \
    ${datadir}/docker-store \
    ${datadir}/docker-data \
    ${localstatedir}/lib/docker \
    "

addtask pull_image before do_install after do_fetch

REQUIRED_DISTRO_FEATURES= "systemd"
