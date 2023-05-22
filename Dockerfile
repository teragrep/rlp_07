FROM rockylinux:8
COPY rpm/target/rpm/com.teragrep-rlp_07/RPMS/noarch/com.teragrep-rlp_07-*.rpm /rpm/
RUN dnf -y install /rpm/*.rpm && yum clean all
WORKDIR /opt/teragrep/rlp_07
ENTRYPOINT [ "/usr/bin/java", "-jar", "lib/rlp_07.jar" ]
