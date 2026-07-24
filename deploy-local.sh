#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

app_name="${APP_NAME:-geome-db}"
environment="${ENVIRONMENT:-local}"
jetty_base="${JETTY_BASE:-}"
port="${PORT:-8081}"
start_jetty="${START_JETTY:-0}"
war_source="build/libs/${app_name}.war"

if [[ -z "$jetty_base" ]]; then
    if [[ -d /usr/local/opt/jetty/libexec ]]; then
        jetty_base="/usr/local/opt/jetty/libexec"
    else
        latest_cellar_jetty="$(find /usr/local/Cellar/jetty -maxdepth 2 -type d -name libexec 2>/dev/null | sort | tail -n 1 || true)"
        if [[ -n "$latest_cellar_jetty" ]]; then
            jetty_base="$latest_cellar_jetty"
        fi
    fi
fi

if [[ -z "$jetty_base" ]]; then
    echo "Could not find local Jetty. Set JETTY_BASE=/path/to/jetty/libexec and rerun." >&2
    exit 1
fi

webapps_dir="${WEBAPPS_DIR:-${jetty_base}/webapps}"
war_dest="${WAR_DEST:-${webapps_dir}/${app_name}.war}"
context_file="${CONTEXT_FILE:-${webapps_dir}/${app_name}.xml}"

echo "Building ${app_name}.war with environment=${environment}"
./gradlew -Penvironment="${environment}" war

mkdir -p "$webapps_dir"
install -m 0644 "$war_source" "$war_dest"

if [[ ! -s "$context_file" || "${WRITE_CONTEXT:-0}" == "1" ]]; then
    cat > "$context_file" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE Configure PUBLIC "-//Jetty//Configure//EN" "http://www.eclipse.org/jetty/configure_9_0.dtd">
<Configure class="org.eclipse.jetty.webapp.WebAppContext">
  <Set name="contextPath">/${app_name}</Set>
  <Set name="war">${war_dest}</Set>
</Configure>
EOF
fi

echo "Deployed ${war_dest}"
echo "Context file: ${context_file}"

if [[ "$start_jetty" == "1" ]]; then
    exec java -jar "${jetty_base}/start.jar" \
        --module=servlets \
        "jetty.base=${jetty_base}" \
        "jetty.http.port=${port}"
fi

echo "To start Jetty now: START_JETTY=1 PORT=${port} ./deploy-local.sh"
