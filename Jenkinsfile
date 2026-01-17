pipeline {
  agent any
  options {
    timestamps()
  }
  parameters {
    string(
      name: 'MAVEN_POMS',
      defaultValue: '',
      description: 'Comma-separated pom.xml paths. Leave blank to build all modules.'
    )
  }
  stages {
    stage('Checkout') {
      steps {
        checkout scm
      }
    }
    stage('Build and Test') {
      steps {
        script {
          def pomList = []
          def rawParam = params.MAVEN_POMS?.trim()
          if (rawParam) {
            pomList = rawParam.split(',')
              .collect { it.trim() }
              .findAll { it }
          } else if (fileExists('pom.xml')) {
            pomList = ['pom.xml']
          } else {
            def raw = sh(
              script: "find . -name pom.xml -not -path './.git/*' -print",
              returnStdout: true
            ).trim()
            pomList = raw ? raw.split('\n') : []
          }
          if (pomList.isEmpty()) {
            error 'No pom.xml found. Set MAVEN_POMS to your project pom.xml paths.'
          }
          env.EFFECTIVE_POMS = pomList.sort().join(',')
        }
        script {
          def poms = env.EFFECTIVE_POMS.split(',')
          for (p in poms) {
            echo "Building ${p}"
            sh "mvn -B -f '${p}' clean verify"
          }
        }
      }
    }
  }
  post {
    always {
      junit testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true
    }
  }
}
