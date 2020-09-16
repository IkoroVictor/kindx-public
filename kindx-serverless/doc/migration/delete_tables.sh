

aws --profile="${AWS_PROFILE:-default}" dynamodb delete-table --table-name kitchens
aws --profile="${AWS_PROFILE:-default}" dynamodb delete-table --table-name users
aws --profile="${AWS_PROFILE:-default}" dynamodb delete-table --table-name menus
aws --profile="${AWS_PROFILE:-default}" dynamodb delete-table --table-name kitchen_configurations
aws --profile="${AWS_PROFILE:-default}" dynamodb delete-table --table-name users_kitchens
aws --profile="${AWS_PROFILE:-default}" dynamodb delete-table --table-name users_notifications
aws --profile="${AWS_PROFILE:-default}" dynamodb delete-table --table-name menu_food_items
aws --profile="${AWS_PROFILE:-default}" dynamodb delete-table --table-name food_items_aggregations
aws --profile="${AWS_PROFILE:-default}" dynamodb delete-table --table-name menu_geo_hashes