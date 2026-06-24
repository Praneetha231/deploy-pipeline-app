pipeline {
    agent any

    environment {
        DOCKERHUB_CREDENTIALS = credentials('dockerhub-creds')
        IMAGE_NAME = "praneetha2305/deploy-pipeline-app"
        IMAGE_TAG = "${env.BUILD_NUMBER}"
        KUBECONFIG_CRED = credentials('kubeconfig')
    }

    stages {

        stage('Checkout Code') {
            steps {
                git branch: 'main', url: 'https://github.com/Praneetha231/deploy-pipeline-app.git'
            }
        }

        stage('Build Artifact (Maven)') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Run Unit Tests') {
            steps {
                sh 'mvn test'
            }
        }

        stage('Build Docker Image') {
            steps {
                sh "docker build -t ${IMAGE_NAME}:${IMAGE_TAG} -t ${IMAGE_NAME}:latest ."
            }
        }

        stage('Push Docker Image') {
            steps {
                sh "echo $DOCKERHUB_CREDENTIALS_PSW | docker login -u $DOCKERHUB_CREDENTIALS_USR --password-stdin"
                sh "docker push ${IMAGE_NAME}:${IMAGE_TAG}"
                sh "docker push ${IMAGE_NAME}:latest"
            }
        }

        stage('Deploy to Kubernetes') {
    steps {
        sh "sed -i 's|IMAGE_PLACEHOLDER|${IMAGE_NAME}:${IMAGE_TAG}|g' k8s/deployment.yaml"
        sh '''
            echo "$KUBECONFIG_CRED" > /tmp/kubeconfig_jenkins.yaml
            kubectl apply -f k8s/deployment.yaml --kubeconfig=/tmp/kubeconfig_jenkins.yaml
            kubectl apply -f k8s/service.yaml --kubeconfig=/tmp/kubeconfig_jenkins.yaml
            kubectl rollout status deployment/deploy-pipeline-app --kubeconfig=/tmp/kubeconfig_jenkins.yaml
        '''
    }
}

        stage('Verify Deployment') {
    steps {
        sh '''
            kubectl get pods -l app=deploy-pipeline-app --kubeconfig=/tmp/kubeconfig_jenkins.yaml
        '''
    }
}
    }

    post {
        success {
            echo 'Pipeline completed successfully. Application deployed.'
        }
        failure {
            echo 'Pipeline failed. Check logs above.'
        }
        always {
            sh 'docker logout'
        }
    }
}
