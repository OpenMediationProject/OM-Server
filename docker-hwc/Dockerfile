FROM adtiming/ombuilder as builder
ARG BUILDER_URL="https://github.com/AdTiming/OM-Server.git"
ARG BUILDER_DIR="OM-Server"
RUN git clone ${BUILDER_URL} && \
    cd ${BUILDER_DIR} && \
    mvn clean package -Dmaven.test.skip=true

FROM adtiming/ombuilder as nginxbuilder
ARG TENGINE_VERSION='tengine-2.3.3'
ARG TENGINE_URL="http://tengine.taobao.org/download/${TENGINE_VERSION}.tar.gz"
RUN curl -O ${TENGINE_URL} && \
    tar zxf ${TENGINE_VERSION}.tar.gz && \
    cd ${TENGINE_VERSION} && \
    yum install -y gcc pcre-devel openssl-devel automake autoconf libtool make && \
    ./configure --with-http_stub_status_module --with-http_ssl_module --with-http_v2_module --with-stream_ssl_module --with-stream --with-http_realip_module --with-stream_realip_module && \
    make && \
    make install && \
    mkdir /usr/local/nginx/{conf.d,https} && \
    mkdir /usr/local/nginx/html/om-ds-ui && \
    rm -f /usr/local/nginx/html/*.html && \
    sed -i -e "/.*#/d"  -e "/^$/d" /usr/local/nginx/conf/nginx.conf && \
    sed -i "/include/i\    include /usr/local/nginx/conf.d/*.conf;" /usr/local/nginx/conf/nginx.conf && \
    sed -i "s/worker_connections.*/worker_connections 102400;/g" /usr/local/nginx/conf/nginx.conf && \
    sed -i "s/worker_processes.*/worker_processes  2;/g" /usr/local/nginx/conf/nginx.conf

FROM adtiming/omjava
MAINTAINER openmediation
ARG GET_BUILDER_DIR="/data/OM-Server"
ENV SYSTEMNAME "omsystem"
ENV SERVERNAME "om-server"
RUN yum install -y rsync && \
    mkdir -p /${SERVERNAME}/log 
ADD docker-entrypoint.sh /
COPY --from=nginxbuilder /usr/local/nginx /usr/local/nginx
COPY --from=builder ${GET_BUILDER_DIR}/target/${SERVERNAME}.jar /${SERVERNAME}/
WORKDIR /
RUN cp /usr/share/zoneinfo/Etc/GMT0 /etc/localtime && \
    chmod +x /docker-entrypoint.sh /bin/rsync /usr/local/nginx/sbin/nginx /${SERVERNAME}/${SERVERNAME}.jar
HEALTHCHECK --interval=30s --timeout=3s \
  CMD curl -f http://localhost:19011/ || exit 1
EXPOSE 19011 80 443
ENTRYPOINT ["/docker-entrypoint.sh"] 
CMD ["om-server"]
