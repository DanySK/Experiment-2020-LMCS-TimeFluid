#!/bin/bash
kubectl cp lmcs-2020-timefluid-experiments:/lmcs/data datanew
for file in datanew/*; do
    LASTLINE="$(tail -n 1 "$file")"
    if [ "${LASTLINE:0:1}" == "#" ]; then
        destination="data${file:7}"
        echo "$destination"
        cp "$file" "$destination"
        git add -f "$destination"
    fi
done
git commit -m "Pull data from K8S"
