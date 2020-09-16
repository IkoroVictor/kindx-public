set -e

stack=''
region=${AWS_REGION:-'us-east-1'}
template_path=${SAM_TEMPLATES_PATH:-"./generated/templates"}

while getopts r:s:e opt; do
  case $opt in
    s) stack=$OPTARG ;;
    r) region=$OPTARG ;;
    [?])	print >&2 "Usage: $0 [-r region] [-s stack name]  ..."
        exit 1;;
  esac
done


sam deploy --template-file "$template_path/$stack.yaml" \
 --region "${region}" \
 --no-fail-on-empty-changeset \
 --debug \
 --capabilities CAPABILITY_NAMED_IAM CAPABILITY_IAM \
 --stack-name "$stack"