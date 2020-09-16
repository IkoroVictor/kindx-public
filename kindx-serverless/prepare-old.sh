set -e


template_path='./doc/serverless/template'
api_definition='api-swagger-definition.yaml'
admin_api_definition='admin-api-swagger-definition.yaml'
env_file='./doc/serverless/env/default.env'
region=${AWS_REGION:-'us-east-1'}

current_commit=$(git rev-parse --verify --short HEAD)
version=${VERSION:-'snapshot'}

while getopts e:t:r: opt; do
  case $opt in
    t) template_path=$OPTARG ;;
    e) env_file=$OPTARG ;;
    r) region=$OPTARG ;;
    [?])	print >&2 "Usage: $0 [-e envfile] [-t template path] [-r region]  ..."
        exit 1;;
  esac
done



set -o allexport # or ( set -a)
# shellcheck source=/dev/null
. $env_file
set +o allexport  # or ( set +a)

S3_LAMBDA_BUCKET=kitchen-lambdas-snapshot-bucket


api_definition_s3="${version}_${current_commit}_${api_definition}"
admin_api_definition_s3="${version}_${current_commit}_${admin_api_definition}"


#Copy API definitions to s3
aws s3 cp "./doc/serverless/swagger/$api_definition" "s3://${S3_LAMBDA_BUCKET}/api/$api_definition_s3" --region "$region"
aws s3 cp "./doc/serverless/swagger/$admin_api_definition" "s3://${S3_LAMBDA_BUCKET}/api/$admin_api_definition_s3" --region "$region"

#Copy java build to S3
cd ./build/distributions/
build_file_name=$(find *.zip)
hash=$(md5sum "$build_file_name" | cut -f 1 -d ' ')
s3_file_name="${version}_${hash}_${build_file_name}"

aws s3 cp "./$build_file_name" "s3://${S3_LAMBDA_BUCKET}/$s3_file_name" --region "$region"

#Copy node layers to s3
cd ../../node
aws s3 cp --recursive ./layers/ "s3://${S3_LAMBDA_BUCKET}/layers" --region "$region"


#Copy node app build to s3
cd ./app
node_zip_name="bundle.zip"
zip -r "$node_zip_name" .
node_hash=$(md5sum "$node_zip_name" | cut -f 1 -d ' ')
node_s3_file_name="node_${version}_${node_hash}_${build_file_name}"
aws s3 cp "./$node_zip_name" "s3://${S3_LAMBDA_BUCKET}/$node_s3_file_name" --region "$region"
rm -rf "$node_zip_name"

#Generate serveless application template
cd ../../
export BUILD_FILE_NAME="$s3_file_name"
export NODE_BUILD_FILE_NAME="$node_s3_file_name"
export COMMIT_HASH="$current_commit"
export API_DEFINITION="$api_definition_s3"
export ADMIN_API_DEFINITION="$admin_api_definition_s3"

generated_template_path='./generated/templates'

rm -rf $generated_template_path
mkdir -p $generated_template_path

# shellcheck disable=SC2044
for f in $(find "$template_path" -regex '.*\.ya*ml');
  do envsubst  "$(env | sed -e 's/=.*//' -e 's/^/\$/g')"  < $f > "./generated/templates/$(basename $f)";
done

s3_template_path_name="${version}_${current_commit}"

aws s3 cp "$generated_template_path" "s3://${S3_LAMBDA_BUCKET}/deployments/$s3_template_path_name/" \
 --recursive \
 --region "$region"
