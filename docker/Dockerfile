FROM adtiming/omjava
MAINTAINER openmediation
ENV SYSTEMNAME "omsystem"
ENV SERVERNAME "om-server"
ENV OMCONF_JAVA_OPTS "-Dapp.snode=1 -Dapp.dcenter=1 -Dapp.dtask=192.168.1.58"
RUN mkdir ${SERVERNAME}
ARG JAR_FILE
ADD docker/docker-entrypoint.sh /
ADD ${JAR_FILE} /${SERVERNAME}/
ADD docker/rsync /bin/
WORKDIR /
HEALTHCHECK --interval=30s --timeout=3s \
  CMD curl -f http://localhost:19011/ || exit 1
RUN chmod +x /docker-entrypoint.sh /bin/rsync
EXPOSE 19011
ENTRYPOINT ["/docker-entrypoint.sh"] 
CMD ["om-server"]
