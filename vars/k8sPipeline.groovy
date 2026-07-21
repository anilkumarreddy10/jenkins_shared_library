pipeline {
    agent any

    tools {
        maven 'mvn'
    }
    environment {
        REGISTRY = "docker.io/gangalakunta"
        IMAGE_TAG  = "latest"
        IMAGE_NAME = "spring-boot-app" 
        PROJECT_ID = 'project-820f02b7-4f02-4540-a71'
        CLUSTER_NAME = 'my-cluster'
        REGION = 'us-central1'
    }
    stages {
        stage('Clean Workspace') {
            steps {
                deleteDir()
            }
        }
        stage('Checkout') {
            steps {
                git branch: 'main',
                    credentialsId: 'git',
                    url: 'git@github.com:anilkumarreddy10/Jenkins-Zero-To-Hero.git'
            }
        }

        stage('Build') {
            steps {
                configFileProvider([
                    configFile(fileId: 'maven-settings', variable: 'SETTINGS')]) {

                    sh """ cd java-maven-sonar-argocd-helm-k8s/spring-boot-app &&
                    mvn -B release:clean release:prepare release:perform \
                    -Dmaven.test.failure.ignore=true \
                    -s ${SETTINGS}
                    """
                }
            }
        }
        stage('sonarqube') {
            steps {
                withSonarQubeEnv('sonarqube') {
                    sh 'cd java-maven-sonar-argocd-helm-k8s/spring-boot-app && mvn sonar:sonar'
                }
            }
        }
        stage('docker') {
            steps {
                sh "cd java-maven-sonar-argocd-helm-k8s/spring-boot-app && docker build -t ${REGISTRY}/${IMAGE_NAME}:${BUILD_NUMBER} ."
                sh " docker tag ${REGISTRY}/${IMAGE_NAME}:${BUILD_NUMBER} ${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}"
            }
        }
        stage('push image') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'docker-cred', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                   sh "echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin"
                   sh "docker push ${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}"
                }
            }
        }
        stage('connect to gke') {
            steps {
                sh "gcloud container clusters get-credentials $CLUSTER_NAME --region $REGION"
            }
        }
        stage('deploy to gke') {
            steps {
                sh "kubectl apply -f java-maven-sonar-argocd-helm-k8s/spring-boot-app-manifests/ "
            }
        }
    }
}
