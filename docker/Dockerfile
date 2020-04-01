FROM adtiming/ombuilder as builder
ARG BUILDER_URL="https://github.com/AdTiming/OM-Server.git"
ARG BUILDER_DIR="OM-Server"
RUN git clone ${BUILDER_URL} && \
    cd ${BUILDER_DIR} && \
    mvn clean package -Dmaven.test.skip=true

FROM adtiming/omjava
MAINTAINER openmediation
ARG GET_BUILDER_DIR="/data/OM-Server"
ENV SYSTEMNAME "omsystem"
ENV SERVERNAME "om-server"
RUN mkdir -p /${SERVERNAME}/log 
ADD docker-entrypoint.sh /
ADD rsync /bin/
ADD nginx/ /usr/local/nginx/
COPY --from=builder ${GET_BUILDER_DIR}/target/${SERVERNAME}.jar /${SERVERNAME}/
WORKDIR /
HEALTHCHECK --interval=30s --timeout=3s \
  CMD curl -f http://localhost:19011/ || exit 1
EXPOSE 19011 80 443
ENTRYPOINT ["/docker-entrypoint.sh"] 
CMD ["om-server"]
