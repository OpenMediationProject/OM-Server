#!/bin/sh

#set -eo pipefail
shopt -s nullglob

# logging functions
function loginfo() {
	local type="$1"; shift
	printf '%s [%s] [Entrypoint]: %s\n' "$(date --rfc-3339=seconds)" "$type" "$*"
}
function loginfo_note() {
	loginfo Note "$@"
}
function loginfo_warn() {
	loginfo Warn "$@" >&2
}
function loginfo_error() {
	loginfo ERROR "$@" >&2
	exit 1
}


function _is_sourced() {
	[ "${#FUNCNAME[@]}" -ge 2 ] \
		&& [ "${FUNCNAME[0]}" = '_is_sourced' ] \
		&& [ "${FUNCNAME[1]}" = 'source' ]
}

function updateConfig() {
    key=$1
    value=$2
    file=$3

    # Omit $value here, in case there is sensitive information
    loginfo_note "[Configuring] '$key' in '$file'"

    # If config exists in file, replace it. Otherwise, append to file.
    if grep -E -q "^$key=" "$file"; then
        sed -r -i "s@^(.*$key=).*@\1$value@g" "$file" #note that no config values may contain an '@' char
    fi
}

function updateymlConfig() {
    key=$1
    value=$2
    file=$3

    # Omit $value here, in case there is sensitive information
    loginfo_note "[Configuring] '$key' in '$file'"

    # If config exists in file, replace it. Otherwise, append to file.
    if grep -E -q "^* $key: " "$file"; then
        sed -r -i "s@^(.*$key:).*@\1 $value@g" "$file" #note that no config values may contain an '@' char
    fi
}


function getEnv(){
CONFFILE=$1
EXCLUSIONS=""
BFIFS=$IFS
IFS=$'\n'
for VAR in $(env)
do
    env_var=$(echo "$VAR" | cut -d= -f1)
    if [[ "${EXCLUSIONS-x}" = *"|$env_var|"* ]]; then
        echo "Excluding $env_var"
        continue
    fi
    if [[ $env_var =~ ^OMYML_ ]]; then
        item_name=$(echo "$env_var" | cut -d_ -f2- | tr '[:upper:]' '[:lower:]' )
        if [[ ${item_name} = "dbaddress" ]];then
            loginfo_note "[Configuring] ${item_name} in ${CONFFILE}/application-loc.yml"
            sed -i "/url/s/127.0.0.1/${!env_var}/g" ${CONFFILE}/application-loc.yml
            continue
        fi
        if [[ ${item_name} = "dbname" ]];then
            loginfo_note "[Configuring] ${item_name} in ${CONFFILE}/application-loc.yml"
            sed -i "/url/s/open_mediation/${!env_var}/g" ${CONFFILE}/application-loc.yml
            continue
        fi
        updateymlConfig "$item_name" "${!env_var}" "${CONFFILE}/application-loc.yml"
    fi

    if [[ $env_var =~ ^OMSERVER_ ]]; then
        item_name=$(echo "$env_var" | cut -d_ -f2- | tr '[:upper:]' '[:lower:]' )
        if [[ ${item_name} = "mountpath" ]]; then
            loginfo_note "[Cloud Storage] Link ${!env_var}/${CONFFILE}/data to /${CONFFILE}/data"
            if [[ -d /${CONFFILE}/data ]];then
                rm -fr /${CONFFILE}/data
            fi
            ln -sf ${!env_var}/${CONFFILE}/data  /${CONFFILE}/data

            loginfo_note "[Cloud Storage] Link ${!env_var}/${CONFFILE}/log to /${CONFFILE}/log"
            if [[ -d /${CONFFILE}/log ]];then
                rm -fr /${CONFFILE}/log
            fi
            ln -sf ${!env_var}/${CONFFILE}/log  /${CONFFILE}/log

            loginfo_note "[Cloud Storage] Link ${!env_var}/${CONFFILE}/conf.d to /usr/local/nginx/conf.d"
            if [[ -d /usr/local/nginx/conf.d ]];then
                rm -fr /usr/local/nginx/conf.d
            fi
            ln -sf ${!env_var}/${CONFFILE}/conf.d  /usr/local/nginx/conf.d

            loginfo_note "[Cloud Storage] Link ${!env_var}/${CONFFILE}/https to /usr/local/nginx/https"
            if [[ -d /usr/local/nginx/https ]];then
                rm -fr /usr/local/nginx/https
            fi
            ln -sf ${!env_var}/${CONFFILE}/https  /usr/local/nginx/https

            continue
        fi 
    fi

    if [[ $env_var =~ ^OMCONF_ ]]; then
        item_name=$(echo "$env_var" | cut -d_ -f2-)
        if [[ ${item_name} = "JAVA_OPTS" ]]; then
            loginfo_note "[Configuring] ${item_name} in ${CONFFILE}/${CONFFILE}.conf"
            loginfo_note "ADD JAVA_OPTS [ ${!env_var} ] to Runtime"
	    JAVA_OPTS="$(sed -n 's/JAVA_OPTS="\(.*\)"/\1/p' ${CONFFILE}/${CONFFILE}.conf) ${!env_var}"
            sed -i "s/JAVA_OPTS=.*/JAVA_OPTS=\"${JAVA_OPTS}\"/g" ${CONFFILE}/${CONFFILE}.conf
            continue
        fi 
        if [[ ${item_name} = "RUN_ARGS" ]]; then
            loginfo_note "[Configuring] ${item_name} in ${CONFFILE}/${CONFFILE}.conf"
            RUN_ARGS="${!env_var}"
            sed -i "s/RUN_ARGS=.*/RUN_ARGS=\"--spring.profiles.active=${RUN_ARGS}\"/g" ${CONFFILE}/${CONFFILE}.conf
            continue
        fi
        updateConfig "$item_name" "${!env_var}" "${CONFFILE}/${CONFFILE}.conf"
    fi
    if [[ $env_var =~ ^OMNGINX_ ]]; then
        item_name=$(echo "$env_var" | cut -d_ -f2- | tr '[:upper:]' '[:lower:]' | tr _ -)
        if [[ ${item_name} = "proxyip" ]];then
            loginfo_note "[Configuring] ${item_name} in /usr/local/nginx/conf/nginx.conf"
            sed -i "/log_format/i\    real_ip_header    X-Real-IP;" /usr/local/nginx/conf/nginx.conf
            sed -i "/real_ip_header/a\    real_ip_recursive on;" /usr/local/nginx/conf/nginx.conf
            proxy_ip=$(echo ${!env_var}|sed "s@,@\n@g")
            for ip in ${proxy_ip}
            do
               sed -i "/real_ip_header/i\    set_real_ip_from ${ip};"  /usr/local/nginx/conf/nginx.conf
            done
            continue
        fi
    fi
done
IFS=$BFIFS
}



function create_conf() {
   CONFILE=$1
   [ ! -d ${CONFILE} ] && mkdir ${CONFILE}
   if [[ -z ${OMJAVA_MAX_MEM} ]]; then
       export OMJAVA_MAX_MEM="512m"       
   fi
   cat >${CONFILE}/${CONFILE}.conf <<EOF
## springboot cfg ###
MODE=service
APP_NAME=${CONFILE}
JAVA_HOME=/usr/local/java/jdk
JAVA_OPTS="-Dapp=\$APP_NAME\
 -Duser.timezone=UTC\
 -Xmx${OMJAVA_MAX_MEM}\
 -Xms${OMJAVA_MAX_MEM}\
 -server"

RUN_ARGS="--spring.profiles.active=prod"
PID_FOLDER=log
LOG_FOLDER=log
LOG_FILENAME=stdout.log
EOF

}

run_program(){
   RUNCMD=$1
   cd ${RUNCMD}/
   [[ ! -x ${RUNCMD}.jar ]] && chmod +x ${RUNCMD}.jar
   ./${RUNCMD}.jar start && /usr/local/nginx/sbin/nginx -g "daemon off;"
}


_main() {
   if [ "${1:0:2}" != "om" ]; then
         loginfo_error "Start with an APP_NAME, such as om-server"
   fi
   loginfo_note "Create Runtime Configure"
   create_conf "$@"
   loginfo_note "Modify Runtime Configure"
   getEnv "$@"
   loginfo_note "Start program $1"
   run_program "$@"
}


if ! _is_sourced; then
	_main "$@"
fi
