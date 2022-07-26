//def cicd
//def awxRequestBody
//def awxRequestJson
def invName
def invId
def logicalBroker
def cicdExtraVars
pipeline {
  agent { label 'ansible' }
//  parameters {
//    string(name: 'ANS_INVENTORY',      defaultValue: 'not_specified', description: 'Ansible Inventory File in Source Project')
//    string(name: 'CICDCONFIG_YAML64',  defaultValue: '',              description: 'Base64 encoded YAML CICD config parameter')
//  }
  environment {
    CICDCONFIG_FILE = ".jenkins/cicd-development.yaml"
//    ANS_INVENTORY="${params.ANS_INVENTORY}"
//    CICDCONFIG_YAML64="${params.CICDCONFIG_YAML64}"
  }
  stages {
    stage( 'Read CICD Input' ) {
      steps {
        script {
//          cicdconfig_yaml = base64Decode( env.CICDCONFIG_YAML64 )
          def cicd = readYaml file: "${CICDCONFIG_FILE}"
//          println "Logical Broker: ${cicd.logicalBroker}"
          invName = cicd.env
          logicalBroker = cicd.logicalBroker
          cicdExtraVars = writeJson returnText: true, json: cicd
//          cicdAsJson = writeJSON returnText: true, json: cicd
        }
        script {
            def responseJson = httpRequest Authentication 'awx-credentials', url: "http://35.225.80.104/api/v2/inventories/?name=${invName}"
            def response = readJson text: responseJson
            invId = response.results[0].id
//            awxRequestBody.inventory = invId
//            awxRequestBody.limit = logicalBroker
//            awxRequestBody.extra_vars = cicd
//            awxRequestJson = writeJSON returnText: true, json: awxRequestBody
        }
      }
    }
    stage ('tower') {
//        wrap([$class: 'AnsiColorBuildWrapper', colorMapName: "xterm"]) {
        steps {
            results = ansibleTower(
                towerServer: 'Solace AWX',
                jobTemplate: 'multi-queue',
                inventory: invId.toString(),
                limit: logicalBroker,
                extraVars: cicdExtraVars,
                importTowerLogs: true,
                removeColor: false,
                verbose: true,
                async: false
            )
                
        }
        println(results.JOB_ID)
        println(results.value)
    }
/*    
    stage('create_queues') {
      steps {
        withCredentials([file(credentialsId: 'ansible_vault_password', variable: 'vault_passwd_file')]) {
          sh "ansible-playbook -i inventory/${ANS_INVENTORY} --limit ${cicd.logicalBroker} --vault-password-file ${vault_passwd_file} --extra-vars='${cicdAsJson}' --extra-vars=@config/development_secrets.encrypted playbooks/create-multi-queue-control.yaml"
//          ansiblePlaybook extras: '${cicdAsJson}, @config/development_secrets.encrypted', installation: 'ANSIBLE2EB', inventory: 'inventory/${ANS_INVENTORY}', limit: '${cicd.logicalBroker}', playbook: 'playbooks/create-multi-queue-control.yaml', vaultCredentialsId: 'ansible_vault_password'  
        }
      }
    }
*/
  }
}