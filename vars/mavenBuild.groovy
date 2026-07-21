def call() {
  stage('Build') {
        steps {
                sh "mvn -B release:clean install"
        }
  }
}
