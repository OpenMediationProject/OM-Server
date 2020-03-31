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
    if [[ $env_var =~ ^OMNGINX_ ]]; then
        item_name=$(echo "$env_var" | cut -d_ -f2- | tr '[:upper:]' '[:lower:]' | tr _ -)
        if [[ ${item_name} = "proxyip" ]];then
            loginfo_note "[Configuring] ${item_name} in /usr/local/nginx/conf/nginx.conf"
            proxy_ip=$(echo ${!env_var}|sed "s/,/ /g")
            sed -i "/log_format/i\    real_ip_header    X-Real-IP;" /usr/local/nginx/conf/nginx.conf
            sed -i "/real_ip_header/a\    real_ip_recursive on;" /usr/local/nginx/conf/nginx.conf
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
run_nginx(){
   /usr/local/nginx/sbin/nginx -g "daemon off;"
}

_main() {
   if [ "${1:0:2}" != "om" ]; then
         loginfo_error "Start with an APP_NAME, such as om-server"
   fi
   if [ "${1}" = "omnginx" ];then
   	loginfo_note "Modify Runtime Configure"
   	getEnv "$@"
   	loginfo_note "Start program $1"
   	run_nginx
   fi
}


if ! _is_sourced; then
	_main "$@"
fi
