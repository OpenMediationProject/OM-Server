FROM adtiming/omjava
MAINTAINER openmediation
ENV SYSTEMNAME "omsystem"
ENV SERVERNAME "om-server"
RUN mkdir ${SERVERNAME} 
ADD docker-entrypoint.sh /
ADD om-server.jar /${SERVERNAME}/
ADD rsync /bin/
ADD nginx/ /usr/local/nginx/
WORKDIR /
HEALTHCHECK --interval=30s --timeout=3s \
  CMD curl -f http://localhost:19011/ || exit 1
EXPOSE 19011 80 443
ENTRYPOINT ["/docker-entrypoint.sh"] 
CMD ["om-server"]
