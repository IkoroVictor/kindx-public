
address=${ES_ADDRESS:-'http://localhost:9200'}
credentials=${ES_CREDENTIALS:-"test:test"}

# shellcheck disable=SC2044
for f in $(find "./indexes" -regex '.*\.json');
  do  echo $f && curl --user "${credentials}" -X PUT -H "Content-Type: application/json" -d @"${f}" "$address/$(basename $f | cut -f 1 -d '.')";
done

# shellcheck disable=SC2044
for f in $(find "./pipelines" -regex '.*\.json');
  do  echo $f && curl --user "${credentials}" -X PUT -H "Content-Type: application/json" -d @"${f}" "$address/_ingest/pipeline/$(basename $f | cut -f 1 -d '.')";
done