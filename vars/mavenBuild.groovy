def call() {
  stage('Build') {
            steps {
                configFileProvider([
                    configFile(fileId: 'maven-settings', variable: 'SETTINGS')]) {

                    sh """ mvn -B release:clean install \
                    -Dmaven.test.failure.ignore=true \
                    -s ${SETTINGS}
                    """
                }
            }
        }
}
