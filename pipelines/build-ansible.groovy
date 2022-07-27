def cicd
def invName
def logicalBroker
def cicdExtraVars
pipeline {
  agent { label 'ansible' }
  parameters {
    string( name:           'PROJECT_REPO',
            defaultValue:   'https://github.com/dennis-brinley/asyncapi-samples.git', 
            description:    'Project containing CICD build file')
    string( name:           'BRANCH',  
            defaultValue:   'development',              
            description:    'Branch in code repo to build' )
    string( name:           'CICDCONFIG_FILE',
            defaultValue:   '.jenkins/cicd-development.yaml',
            description:    'The location of the CICD config file in the repository' )
  }
  environment {
    BUILD_DIR = "__BUILD_DIR__/"
    CICDCONFIG_FILE = "${BUILD_DIR}${params.CICDCONFIG_FILE}"
    ENV_SECRETS_FILE = "config/${BRANCH}_secrets.encrypted"
  }
  stages {
    stage( 'Checkout' ) {
        steps {
            script {
                dir( "${BUILD_DIR}" ) {
                    git branch: "${params.BRANCH}", url: "${params.PROJECT_REPO}"
                }
            }
        }
    }
    stage( 'Read CICD Input' ) {
      steps {
        script {
          cicd = readYaml file: "${CICDCONFIG_FILE}"
          invName = cicd.env
          logicalBroker = cicd.logicalBroker
          cicdExtraVars = writeJSON returnText: true, json: cicd
          println("${cicdExtraVars}")
        }
      }
    }
    stage ('ansible build') {
      steps {
        withCredentials([file(credentialsId: 'ansible_vault_password', variable: 'vault_passwd_file')]) {
//          sh "ansible-playbook -i inventory/${invName} --limit ${logicalBroker} --vault-password-file ${vault_passwd_file} --extra-vars='${cicdExtraVars}' --extra-vars=@${ENV_SECRETS_FILE} playbooks/create-multi-queue-control.yaml"

//          ansiblePlaybook extraVars: [ queueDefinitions: "${queueDefinitions}", "@${ENV_SECRETS_FILE}" ], 
          ansiblePlaybook extras: "-e '${cicdExtraVars}' -e @${ENV_SECRETS_FILE}", 
                          installation: 'ANSIBLE_SOLACE_COLLECTION', 
                          inventory: "inventory/${invName}", 
                          limit: "${logicalBroker}", 
                          playbook: 'playbooks/create-multi-queue-control.yaml', 
                          vaultCredentialsId: 'ansible_vault_password'  

        }
      }
    }
//    stage( 'Update EP MEM' ) {
//        steps {
//
//        }
//    }
  }
}