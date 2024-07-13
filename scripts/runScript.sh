#!/bin/bash
echo "Remove old logs"
rm -rf /home/ec2-user/exportLogs.txt
cd /home/ec2-user/google-drive-to-s3-exporter
export BUCKET_NAME=TBD
export SNS_TOPIC=TBD
export REGION=TBD
export DYNAMODB_TABLE=TBD
export SM_GROUP=TBD
echo "Start the exporter"
mvn spring-boot:run > /home/ec2-user/exportLogs.txt
echo "Script has finished"