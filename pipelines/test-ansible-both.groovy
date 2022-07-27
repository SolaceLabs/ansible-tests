pipeline {
  agent { label 'ansible' }
  parameters {
    string(name: 'ANS_LOGICAL_BROKER', defaultValue: 'not_specified', description: 'Logical Broker Identifier in Ansible Inventory')
    string(name: 'ANS_INVENTORY',      defaultValue: 'not_specified', description: 'Ansible Inventory File in Source Project')
  }
  // comment 4
  environment {
    ANS_LOGICAL_BROKER="${params.ANS_LOGICAL_BROKER}"
    ANS_INVENTORY="${params.ANS_INVENTORY}"
  }
  stages {
    stage( 'Init' ) {
      steps {
        echo "Logical Broker:   ${ANS_LOGICAL_BROKER}"
        echo "Inventory:        ${ANS_INVENTORY}"
      }
    }
    stage('create_queues') {
      steps {
        withCredentials([file(credentialsId: 'ansible_vault_password', variable: 'vault_passwd_file')]) {
          sh 'ansible-playbook -i inventory/${ANS_INVENTORY} --limit ${ANS_LOGICAL_BROKER} --vault-password-file ${vault_passwd_file} --extra-vars="@examples/jenkins/multi-queue-sample.yaml" --extra-vars="@config/development_secrets.encrypted" playbooks/create-multi-queue-control.yaml'
        }
      }
    }
  }
}