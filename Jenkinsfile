#!/usr/bin/env groovy

node('nimble-jenkins-slave') {
    def app
    stage('Clone and Update') {
        // slackSend 'Started build no. ${env.BUILD_ID} of ${env.JOB_NAME}'
        git(url: 'https://github.com/nimble-platform/identity-service.git', branch: env.BRANCH_NAME)
        sh 'git submodule init'
        sh 'git submodule update'
    }

    stage('Build Docker Image') {
        withDockerRegistry([credentialsId: 'NimbleDocker']) {
            sh '/bin/bash -xe deploy.sh docker-build'
        }
    }

    if (env.BRANCH_NAME == 'master') {
        stage('Push Docker image') {
            withDockerRegistry([credentialsId: 'NimbleDocker']) {
                sh 'docker push nimbleplatform/identity-service'
            }
        }

        stage('Apply to Cluster') {
            sh 'kubectl apply -f kubernetes/deploy-prod.yml -n prod --validate=false'
        }
    }
}
