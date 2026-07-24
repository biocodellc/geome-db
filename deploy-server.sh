#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

app_name="${APP_NAME:-geome-db}"
environment="${ENVIRONMENT:-production}"
jetty_base="${JETTY_BASE:-/usr/share/jetty9}"
webapps_dir="${WEBAPPS_DIR:-${jetty_base}/webapps}"
war_source="build/libs/${app_name}.war"
war_dest="${WAR_DEST:-${webapps_dir}/${app_name}.war}"
context_file="${CONTEXT_FILE:-${webapps_dir}/${app_name}.xml}"
service_name="${SERVICE_NAME:-jetty9}"
restart_service="${RESTART_SERVICE:-1}"
write_context="${WRITE_CONTEXT:-1}"
health_url="${HEALTH_URL:-http://127.0.0.1:8080/${app_name}/}"

if [[ ! -d "$jetty_base" ]]; then
    echo "Jetty base not found at ${jetty_base}. Set JETTY_BASE and rerun." >&2
    exit 1
fi

echo "Building ${app_name}.war with environment=${environment}"
./gradlew clean
./gradlew -PforceJars=true -Penvironment="${environment}" war

tmp_context="$(mktemp)"
trap 'rm -f "$tmp_context"' EXIT
cat > "$tmp_context" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE Configure PUBLIC "-//Jetty//Configure//EN" "http://www.eclipse.org/jetty/configure_9_0.dtd">
<Configure class="org.eclipse.jetty.webapp.WebAppContext">
  <Set name="contextPath">/${app_name}</Set>
  <Set name="war">${war_dest}</Set>
</Configure>
EOF

sudo mkdir -p "$webapps_dir"
sudo install -m 0644 "$war_source" "$war_dest"

if [[ "$write_context" == "1" || ! -s "$context_file" ]]; then
    sudo install -m 0644 "$tmp_context" "$context_file"
else
    sudo touch "$context_file"
fi

if [[ "$restart_service" == "1" ]]; then
    if command -v systemctl >/dev/null 2>&1 && systemctl list-unit-files "${service_name}.service" >/dev/null 2>&1; then
        sudo systemctl daemon-reload
        sudo systemctl restart "$service_name"
    else
        sudo service "$service_name" restart
    fi
fi

echo "Deployed ${war_dest}"
echo "Context file: ${context_file}"

if command -v curl >/dev/null 2>&1; then
    echo "Checking ${health_url}"
    for _ in $(seq 1 30); do
        status="$(curl -s -o /dev/null -w '%{http_code}' "$health_url" 2>/dev/null || true)"
        if [[ "$status" =~ ^(200|302|401|403)$ ]]; then
            echo "Jetty responded with HTTP ${status}"
            exit 0
        fi
        sleep 2
    done
    echo "Jetty did not respond successfully at ${health_url}; check service logs." >&2
fi
