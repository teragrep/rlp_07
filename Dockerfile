FROM rockylinux:8
COPY rpm/target/rpm/com.teragrep-rlp_07/RPMS/noarch/com.teragrep-rlp_07-*.rpm /rpm/
COPY src/main/resources/keystore-server.jks /keystore/keystore-server.jks
RUN dnf -y install /rpm/*.rpm && yum clean all
WORKDIR /opt/teragrep/rlp_07
ENTRYPOINT /usr/bin/java -Dport="${RLP_07_PORT:-1601}" -Dtls="${RLP_07_TLS:-false}" -DtlsKeystorePassword="${RLP_07_TLS_KEYSTOREPASSWORD:-changeit}" -DtlsKeystore="${RLP_07_TLS_KEYSTORE:-/keystore/keystore-server.jks}" -jar lib/rlp_07.jar
