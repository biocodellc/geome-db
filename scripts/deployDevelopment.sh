#!/usr/bin/env bash

ssh deploy@biscicol3.acis.ufl.edu <<'ENDSSH'

    cd code/dev/geome-db
    ./scripts/development_deploy.sh

ENDSSH