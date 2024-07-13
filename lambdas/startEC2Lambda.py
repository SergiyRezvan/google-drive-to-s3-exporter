import boto3
import os

region = os.environ['AWS_REGION']
instances = [os.environ['INSTANCE_ID']]
ec2 = boto3.client('ec2', region_name=region)

def lambda_handler(event, context):
  ec2.start_instances(InstanceIds=instances)
  print('start your instances: ' + str(instances))