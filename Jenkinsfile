#!/usr/bin/env groovy

node('nimble-jenkins-slave') {

    stage('Test') {
        sh 'env'
    }

    if (env.BRANCH_NAME == 'staging') {

        stage('Clone and Update') {
            git(url: 'https://github.com/nimble-platform/identity-service.git', branch: env.BRANCH_NAME)
            sh 'git submodule init'
            sh 'git submodule update'
        }

        stage('Run Tests') {
            sh 'mvn clean test'
        }

        stage('Build Java') {
            sh 'mvn clean install -DskipTests'
        }

        stage('Build Docker') {
            sh 'mvn -f identity-service/pom.xml docker:build -DdockerImageTag=staging'
        }

        stage('Push Docker') {
            sh 'docker push nimbleplatform/identity-service:staging'
        }

        stage('Deploy') {
            sh 'ssh staging "cd /srv/nimble-staging/ && ./run-staging.sh restart-single identity-service"'
        }
    }

    if (env.BRANCH_NAME == 'master') {

        stage('Clone and Update') {
            git(url: 'https://github.com/nimble-platform/identity-service.git', branch: env.BRANCH_NAME)
            sh 'git submodule init'
            sh 'git submodule update'
        }

        stage('Run Tests') {
            sh 'mvn clean test'
        }

        stage('Build Java') {
            sh 'mvn clean install -DskipTests'
        }
    }

    // check for release (e.g. tagged with 0.0.1)
    if( env.BRANCH_NAME ==~ /^\d+.\d+.\d+$/) {

        stage('Clone and Update') {
            git(url: 'https://github.com/nimble-platform/identity-service.git', branch: 'master')
            sh 'git submodule init'
            sh 'git submodule update'
        }

        stage('Run Tests') {
            sh 'mvn clean test'
        }

        stage('Build Java') {
            sh 'mvn clean install -DskipTests'
        }

        stage('Build Docker') {
            sh 'mvn -f identity-service/pom.xml docker:build'
        }

//        stage('Push Docker') {
//            sh 'mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version' // fetch dependencies
//            sh 'docker push nimbleplatform/identity-service:$(mvn -f identity-service/pom.xml org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version | grep -v \'\\[\')'
//            sh 'docker push nimbleplatform/identity-service:latest'
//        }
//
//        stage('Deploy MVP') {
//            sh 'ssh nimble "cd /data/deployment_setup/prod/ && sudo ./run-prod.sh restart-single identity-service"'
//        }
//
//        stage('Deploy FMP') {
//            sh 'ssh fmp-prod "cd /srv/nimble-fmp/ && ./run-fmp-prod.sh restart-single identity-service"'
//        }
    }
}
