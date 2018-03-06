node ('nimble-jenkins-slave') {
    stage('Download Latest') {
        git(url: 'https://github.com/nimble-platform/identity-service.git', branch: 'k8s-integration')
        sh 'git submodule init'
        sh 'git submodule update'
    }

    stage ('Build docker image') {
        sh 'mvn clean install -DskipTests'

//        sh 'cat identity-service/pom.xml'
        sh ''' sed -i 's/IMAGE_TAG/'"$BUILD_NUMBER"'/g' identity-service/pom.xml '''
//        sh 'cat identity-service/pom.xml'

        sh 'mvn -f identity-service/pom.xml docker:build'

        sh 'sleep 5' // For the tag to populate
    }

    stage ('Push docker image') {
        withDockerRegistry([credentialsId: 'NimbleDocker']) {
            sh 'docker push nimbleplatform/identity-service:${BUILD_NUMBER}'
        }
    }

    stage ('Deploy') {
        sh ''' sed -i 's/IMAGE_TAG/'"$BUILD_NUMBER"'/g' kubernetes/deploy.yaml '''
        sh 'kubectl apply -f kubernetes/keycloak-svc.yml -n prod --validate=false'
        sh 'kubectl apply -f kubernetes/keycloak-deploy.yml -n prod --validate=false'

        sh 'kubectl apply -f kubernetes/deploy.yml -n prod --validate=false'
        sh 'kubectl apply -f kubernetes/svc.yml -n prod --validate=false'
    }

    stage ('Print-deploy logs') {
        sh 'sleep 60'
        sh 'kubectl  -n prod logs deploy/identity-service -c identity-service'
    }
}





//#!/usr/bin/env groovy
//
//node('nimble-jenkins-slave') {
//
//    stage('Clone and Update') {
//        // slackSend 'Started build no. ${env.BUILD_ID} of ${env.JOB_NAME}'
//        git(url: 'https://github.com/nimble-platform/identity-service.git', branch: env.BRANCH_NAME)
//        sh 'git submodule init'
//        sh 'git submodule update'
//    }
//
//    stage('Build Java') {
//        sh 'mvn clean install -DskipTests'
//    }
//
//    stage('Build Docker') {
//        sh 'mvn -f identity-service/pom.xml docker:build'
//    }
//
//    if (env.BRANCH_NAME == 'master') {
 //       stage('Deploy') {
//            sh 'ssh nimble "cd /data/deployment_setup/prod/ && sudo ./run-prod.sh restart-single identity-service"'
//        }
//    }
//
//    if (env.BRANCH_NAME == 'master') {
//        stage('Push Docker') {
//            withDockerRegistry([credentialsId: 'NimbleDocker']) {
//                sh 'docker push nimbleplatform/identity-service:latest'
//            }
//        }
    
//
//        stage('Apply to Cluster') {
//            sh 'ssh nimble "cd /data/nimble_setup/ && sudo ./run-prod.sh restart-single identity-service"'
////            sh 'kubectl apply -f kubernetes/deploy.yml -n prod --validate=false'
//        }
//    }
//}
