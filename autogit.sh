#!/bin/bash
i=1
count=$(ls -l data/ | wc -l)
for file in data/*; do
  LASTLINE="$(tail -n 1 "$file")"
  if [ "${LASTLINE:0:1}" == "#" ]; then
    git add -f "$file"
  else
    echo -ne "\033[2K\r"
    echo "Still incomplete: $file ($i/$count)"
  fi
  echo -ne "\033[2K\r"
  echo -ne "$i/$count"
  let "i+=1"
done
COMMIT_MESSAGE="[AUTOMATIC ${USER}@${HOSTNAME}] - `date --iso-8601=minutes` data update"
git commit -m "${COMMIT_MESSAGE}"
git fetch
git merge FETCH_HEAD -m "Merge: $COMMIT_MESSAGE"
git commit -m "$COMMIT_MESSAGE (merge commit)"
git push
