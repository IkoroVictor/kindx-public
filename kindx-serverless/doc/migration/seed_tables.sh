

aws --profile="${AWS_PROFILE:-default}" dynamodb batch-write-item --cli-input-json file://./data/kitchens.json
aws --profile="${AWS_PROFILE:-default}" dynamodb batch-write-item --cli-input-json file://./data/users.json
aws --profile="${AWS_PROFILE:-default}" dynamodb batch-write-item --cli-input-json file://./data/kitchen_configurations.json
aws --profile="${AWS_PROFILE:-default}" dynamodb batch-write-item --cli-input-json file://./data/users_kitchens.json