# google-drive-to-s3-exporter
Java command line application to export files from GoogleDrive to Amazon S3

This is the Spring Boot command line application that always starts only one command - exporting of google drive data onto S3

*/lambdas/* folder contains Python lambda functions to start and stop AWS Lambdas.

*/scripts/* folder contains shell scripts that are needed to be executed on EC2 instance.
installScripts.sh - here you can find bare minimum of commands that are needed to install Java and Maven, pull git project and build it. And additional scripts after exporter.service
exporter.service - file that has to be placed in /etc/systemd/system/ in order to run exporter service after startup.
runScript.sh - file that will be executed on startup, where environment variables are set according to required resources.

List of env variables:
BUCKET_NAME - s3 bucket name where Google Drive data will be located
SNS_TOPIC - SNS topic arn that is used for notification
REGION - AWS region where all AWS resources are created
DYNAMODB_TABLE - DynamoDB table name to track exporter runner executions.
SM_GROUP - AWS Secrets Manager group name with Google Service Account credentials.
IGNORED_TYPES - Optional. List of media types that should be ignored by exporter.

