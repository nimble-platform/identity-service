node ('nimble-jenkins-slave') {
    def app
    stage('Clone and Update') {
        // slackSend 'Started build no. ${env.BUILD_ID} of ${env.JOB_NAME}'
        git(url: 'https://github.com/nimble-platform/identity-service.git', branch: 'master')
        sh 'git submodule init'
        sh 'git submodule update'
    }

    stage ('Build Docker Image') {
        sh '/bin/bash -xe deploy.sh docker-build'
    }

    stage ('Push Docker image') {
        withDockerRegistry([credentialsId: 'NimbleDocker']) {
            sh 'docker push nimbleplatform/identity-service'
        }
    }

    stage ('Apply to Cluster') {
        sh 'kubectl apply -f kubernetes/deploy-prod.yaml -n prod --validate=false'
    }
}
