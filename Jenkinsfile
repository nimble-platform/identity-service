#!/usr/bin/env groovy

node('nimble-jenkins-slave') {

    // -----------------------------------------------
    // --------------- Staging Branch ----------------
    // -----------------------------------------------
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

    // -----------------------------------------------
    // --------------- Staging V2 Branch ----------------
    // -----------------------------------------------
    if (env.BRANCH_NAME == 'staging-v2') {

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
            sh 'mvn -f identity-service/pom.xml docker:build -DdockerImageTag=staging-v2'
        }

        stage('Push Docker') {
            sh 'docker push nimbleplatform/identity-service:staging-v2'
        }

        stage('Deploy') {
            sh 'ssh staging "cd /srv/nimble-staging/ && ./run-staging.sh restart-single identity-service-v2"'
        }
    }

    // -----------------------------------------------
    // ---------------- Master Branch ----------------
    // -----------------------------------------------
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

    // -----------------------------------------------
    // --------------- K8s Branch ----------------
    // -----------------------------------------------
    if (env.BRANCH_NAME == 'efactory') {

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
            sh 'mvn -f identity-service/pom.xml docker:build -DdockerImageTag=efactory'
        }

        stage('Push Docker') {
            sh 'docker push nimbleplatform/identity-service:efactory'
        }

        stage('Deploy') {
            sh 'ssh efac-prod "kubectl delete pod -l  io.kompose.service=identity-service"'
        }
    }

    // -----------------------------------------------
    // ---------------- Release Tags -----------------
    // -----------------------------------------------
    if( env.TAG_NAME ==~ /^\d+.\d+.\d+$/) {

        stage('Clone and Update') {
            git(url: 'https://github.com/nimble-platform/identity-service.git', branch: 'master')
            sh 'git submodule init'
            sh 'git submodule update'
        }
        stage('Set version') {
            sh 'mvn org.codehaus.mojo:versions-maven-plugin:2.1:set -DnewVersion=' + env.TAG_NAME
            sh 'mvn -f identity-service/pom.xml org.codehaus.mojo:versions-maven-plugin:2.1:set -DnewVersion=' + env.TAG_NAME
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

        stage('Push Docker') {
            sh 'docker push nimbleplatform/identity-service:' + env.TAG_NAME
            sh 'docker push nimbleplatform/identity-service:latest'
        }

        stage('Deploy MVP') {
            sh 'ssh nimble "cd /data/deployment_setup/prod/ && sudo ./run-prod.sh restart-single identity-service"'
        }

        stage('Deploy FMP') {
            sh 'ssh fmp-prod "cd /srv/nimble-fmp/ && ./run-fmp-prod.sh restart-single identity-service"'
        }
    }
}
