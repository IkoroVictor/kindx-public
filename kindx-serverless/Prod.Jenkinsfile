pipeline {
    agent { label 'slave-prod-1' }
    environment {
        AWS_DEPLOY_REGION   = 'eu-north-1'
        ELASTIC_SEARCH_CREDENTIALS   = credentials('es-cred-prod')
        TWILIO_AUTH_TOKEN = credentials('twilio-auth-token-prod')
        FACEBOOK_TOKEN = credentials('fb-token-prod')
    }
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        stage('Test') {
            steps {
                sh 'gradle clean test'
            }
        }
        stage('Package') {
            steps {
                sh 'gradle build'
            }
        }
        stage('Deploy') {
            steps {
                script {
                  echo "Building production binaries and templates"
                  sh """
                        export ELASTIC_SEARCH_CREDENTIALS=${ELASTIC_SEARCH_CREDENTIALS}
                        export TWILIO_AUTH_TOKEN=${TWILIO_AUTH_TOKEN}
                        export FACEBOOK_TOKEN=${FACEBOOK_TOKEN}
                        export VERSION=release

                        ./prepare.sh -e ./doc/serverless/env/prod.env
                     """
                }

            }
        }
    }
   post {
       success {
           script { slackSend color: 'good', message: " *PRODUCTION*: :grinning: Successful build. [Job: *${JOB_NAME}*] - [Build: *${BUILD_NUMBER}*]" }
       }
       unstable {
           script { slackSend color: '#ffa700', message: "*PRODUCTION*: :worried: Build Unstable. [Job: *${JOB_NAME}*] - [Build: *${BUILD_NUMBER}*] "}
       }
       failure {
           script { slackSend color: '#ff0000', message: "*PRODUCTION*: :skull: Build failed. [Job: *${JOB_NAME}*] - [Build: *${BUILD_NUMBER}*] " }
       }
   }
}