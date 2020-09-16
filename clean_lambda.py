from __future__ import absolute_import, print_function, unicode_literals
import boto3


def clean_old_lambda_versions():
    client = boto3.client('lambda')
    functions = client.list_functions()['Functions']
    for function in functions:
        versions = client.list_versions_by_function(FunctionName=function['FunctionArn'])['Versions']
        for version in versions:
            if version['Version'] != function['Version']:
                arn = version['FunctionArn']
                print('delete_function(FunctionName={})'.format(arn))
                try:
                  client.delete_function(FunctionName=arn)  # uncomment me once you've checked
                except:
                  print ("Could not delete_function")

def clean_old_lambda_versions():
    client = boto3.client('lambda')
    functions = client.list_functions()['Functions']
    for function in functions:
        while True:
          versions = client.list_versions_by_function(FunctionName=function['FunctionArn'])['Versions']
          if len(versions) == 2:
              print('{}: done'.format(function['FunctionName']))
              break
          for version in versions:
              if version['Version'] != function['Version']:
                  arn = version['FunctionArn']
                  print('delete_function(FunctionName={})'.format(arn))
                  try:
                    client.delete_function(FunctionName=arn)  # uncomment me once you've checked
                  except:
                    print ("Could not delete_function")



if __name__ == '__main__':
    clean_old_lambda_versions()