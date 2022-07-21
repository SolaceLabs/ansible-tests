// Groovy Function to encode input String to Base64 String
def base64Encode(inputString){
    encoded = inputString.bytes.encodeBase64().toString()
    return encoded
}

// Groovy Function to decode Base64 input to String 
def base64Decode(encodedString){
    byte[] decoded = encodedString.decodeBase64()
    String decode = new String(decoded)
    return decode
}

def cicd
def cicdAsJson
pipeline {
  agent { label 'ansible' }
  parameters {
    string(name: 'ANS_INVENTORY',      defaultValue: 'not_specified', description: 'Ansible Inventory File in Source Project')
    string(name: 'CICDCONFIG_YAML64',  defaultValue: '',              description: 'Base64 encoded YAML CICD config parameter')
  }
  environment {
    ANS_INVENTORY="${params.ANS_INVENTORY}"
    CICDCONFIG_YAML64="${params.CICDCONFIG_YAML64}"
  }
  stages {
    stage( 'Init' ) {
      steps {
        script {
          cicdconfig_yaml = base64Decode( env.CICDCONFIG_YAML64 )
          cicd = readYaml text: "${cicdconfig_yaml}"
//          println "Logical Broker: ${cicd.logicalBroker}"
          cicdAsJson = writeJSON returnText: true, json: cicd
        }
      }
    }
    stage( 'extract_cicdconfig' ) {
      
    }
    stage('create_queues') {
      steps {
        withCredentials([file(credentialsId: 'ansible_vault_password', variable: 'vault_passwd_file')]) {
          sh "ansible-playbook -i inventory/${ANS_INVENTORY} --limit ${cicd.logicalBroker} --vault-password-file ${vault_passwd_file} --extra-vars='${cicdAsJson}' --extra-vars=@config/development_secrets.encrypted playbooks/create-multi-queue-control.yaml"
//          ansiblePlaybook extras: '${cicdAsJson}, @config/development_secrets.encrypted', installation: 'ANSIBLE2EB', inventory: 'inventory/${ANS_INVENTORY}', limit: '${cicd.logicalBroker}', playbook: 'playbooks/create-multi-queue-control.yaml', vaultCredentialsId: 'ansible_vault_password'  
        }
      }
    }
  }
}
