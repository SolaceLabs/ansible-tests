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
    string(name: 'CICDCONFIG_YAML64',  defaultValue: 'LS0tCnRpdGxlOiBteUV2ZW50QXBpCmRlc2NyaXB0aW9uOiBteUV2ZW50QXBpCmFwaVZlcnNpb246IDAuMS4wCmFwcGxpY2F0aW9uRG9tYWluSWQ6IGh0a2VrcmgzN3MwCmFwcGxpY2F0aW9uRG9tYWluTmFtZTogbXlBcHBEb21haW4KZXZlbnRBcGlTdGF0ZTogMQpldmVudEFwaVN0YXRlTmFtZTogRFJBRlQKbW9kZWxlZEV2ZW50TWVzaDogZGVtby1tZXNoCmxvZ2ljYWxCcm9rZXI6IGxvZ2ljYWxfYnJva2VyX2IKcXVldWVEZWZpbml0aW9uczoKLSBxdWV1ZU5hbWU6IENyZWF0ZWRIUkV2ZW50cwogIHF1ZXVlU2V0dGluZ3M6CiAgICBhY2Nlc3NUeXBlOiBleGNsdXNpdmUKICB0b3BpY1N1YnNjcmlwdGlvbnM6CiAgLSBwZXJzb24vKi9jcmVhdGVkCiAgLSBwZXJzb24vb3RoZXIvdG9waWMKLSBxdWV1ZU5hbWU6IFVwZGF0ZWRIUkV2ZW50cwogIHF1ZXVlU2V0dGluZ3M6CiAgICBhY2Nlc3NUeXBlOiBub24tZXhjbHVzaXZlCiAgdG9waWNTdWJzY3JpcHRpb25zOgogIC0gcGVyc29uLyovdXBkYXRlZAogIC0gcGVyc29uL290aGVyL3RvcGljCiAgLSBwZXJzb24vdGhpcmQvdG9waWMKLSBxdWV1ZU5hbWU6IFdIT0xFX09USEVSX1FVRVVFCiAgcXVldWVTZXR0aW5nczoKICAgIGFjY2Vzc1R5cGU6IG5vbi1leGNsdXNpdmUKICB0b3BpY1N1YnNjcmlwdGlvbnM6CiAgLSBzdGFyLyovdXBkYXRlZAogIC0gc3Rhci9vdGhlci90b3BpYwogIC0gc3Rhci90aGlyZC90b3BpYw==',
                  description: 'Base64 encoded YAML CICD config parameter')
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
      steps{ 
        sh "echo extract_cicdconfig"
      }
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
