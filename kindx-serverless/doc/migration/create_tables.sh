#https://docs.aws.amazon.com/cli/latest/reference/dynamodb/index.html#cli-aws-dynamodb


for f in $(find "./tables" -regex '.*\.json');
  do aws  --profile="${AWS_PROFILE:-default}" dynamodb create-table --cli-input-json "file://./tables/$(basename $f)";
done