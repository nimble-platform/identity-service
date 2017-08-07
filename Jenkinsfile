node ('nimble-jenkins-slave') {
    def app
    stage('Clone & Build') {
            // slackSend 'Started build no. ${env.BUILD_ID} of ${env.JOB_NAME}'
            git(url: 'https://github.com/nimble-platform/identity-service.git', branch: 'master')
            sh 'git submodule init'
            sh 'git submodule update'
            sh 'deploy.sh'
    }
    stage ('Docker Build') {
        app = docker.build("nimbleplatform/identity-service")
    }
    stage ('Docker Push')  {
      docker.withRegistry('https://registry.hub.docker.com', 'NimbelPlatformDocker') {
            app.push("${env.BUILD_NUMBER}")
            app.push("latest")
        }
    }
}

