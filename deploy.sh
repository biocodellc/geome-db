#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

target="${1:-auto}"
if [[ $# -gt 0 ]]; then
    shift
fi

if [[ "$target" == "auto" ]]; then
    if [[ -d /usr/share/jetty9 && ! -d /usr/local/opt/jetty/libexec ]]; then
        target="server"
    else
        target="local"
    fi
fi

case "$target" in
    local)
        exec ./deploy-local.sh "$@"
        ;;
    server|production|prod)
        exec ./deploy-server.sh "$@"
        ;;
    *)
        echo "Usage: $0 [local|server]" >&2
        exit 2
        ;;
esac
