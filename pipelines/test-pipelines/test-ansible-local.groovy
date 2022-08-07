pipeline {
  agent { label 'ansible' }
  environment {
    DUMMYVAR='Stuff'
  }
  stages {
    stage('Create_queues') {
      steps {
        sh 'ansible-playbook -i inventory/inv_local.yml config/configure.playbook.yml'
      }
    }
  }
}