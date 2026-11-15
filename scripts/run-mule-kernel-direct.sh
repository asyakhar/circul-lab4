#!/usr/bin/env bash
set -euo pipefail

if [[ -z "${MULE_HOME:-}" || ! -d "$MULE_HOME/lib/boot" ]]; then
    echo "Укажите MULE_HOME установленного Mule Kernel." >&2
    exit 1
fi

MULE_BASE="${MULE_BASE:-$MULE_HOME}"
JAVA_BIN="${JAVA_BIN:-$(command -v java)}"
MULE_XMS="${MULE_XMS:-64m}"
MULE_XMX="${MULE_XMX:-384m}"
MULE_METASPACE="${MULE_METASPACE:-192m}"
MULE_PROCESSORS="${MULE_PROCESSORS:-1}"
HTTP_HOST="${HTTP_HOST:-127.0.0.1}"
HTTP_PORT="${HTTP_PORT:-8083}"
SOAP_WSDL_URL="${SOAP_WSDL_URL:?Укажите SOAP_WSDL_URL}"
SOAP_ENDPOINT_URL="${SOAP_ENDPOINT_URL:?Укажите SOAP_ENDPOINT_URL}"

export MULE_HOME MULE_BASE

exec "$JAVA_BIN" \
    -Xms"$MULE_XMS" \
    -Xmx"$MULE_XMX" \
    -XX:MaxMetaspaceSize="$MULE_METASPACE" \
    -XX:ActiveProcessorCount="$MULE_PROCESSORS" \
    -Djava.net.preferIPv4Stack=true \
    -Djava.locale.providers=COMPAT,CLDR,SPI \
    -Dlog4j2.disable.jmx=true \
    -Dmule.home="$MULE_HOME" \
    -Dmule.base="$MULE_BASE" \
    -Dmule.bootstrap.container.wrapper.class=org.mule.runtime.module.boot.internal.MuleContainerBasicWrapper \
    -Dhttp.host="$HTTP_HOST" \
    -Dhttp.port="$HTTP_PORT" \
    -Dticket.soap.wsdl="$SOAP_WSDL_URL" \
    -Dticket.soap.endpoint="$SOAP_ENDPOINT_URL" \
    --add-modules=java.se,org.mule.runtime.boot.log4j,org.mule.runtime.logging,org.mule.runtime.jpms.utils,com.fasterxml.jackson.core \
    --module-path="$MULE_HOME/lib/boot" \
    --add-opens=java.base/java.lang.reflect=org.mule.runtime.jpms.utils \
    --add-opens=java.base/java.lang=org.mule.runtime.jpms.utils \
    --add-opens=java.sql/java.sql=org.mule.runtime.jpms.utils \
    --add-opens=java.base/java.lang.invoke=org.mule.runtime.jpms.utils \
    --module=org.mule.boot/org.mule.runtime.module.reboot.MuleContainerBootstrap
