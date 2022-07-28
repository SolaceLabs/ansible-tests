def cicd
def invName
def invId
def logicalBroker
def cicdExtraVars

def branch
pipeline {
  agent { label 'ansible' }
/*
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
*/
  environment {
    CICDCONFIG_FILE = ".jenkins/cicd-development.yaml"
    BUILD_DIR = "__BUILD_DIR__/"
    CICDCONFIG_FILE_PATH = "${BUILD_DIR}${CICDCONFIG_FILE}"
    PROJECT_REPO = "https://github.com/dennis-brinley/asyncapi-samples.git"
    BRANCH = "main"
  }
  stages {
    stage( 'Checkout' ) {
        steps {
            script {
               def values = "${ref}".split('/')
               branch = values[2]
               println( "Found Branch: ${branch}" )
            }
            script {
                dir( "${BUILD_DIR}" ) {
                    git branch: "${branch}", url: "${PROJECT_REPO}"
                }
            }
        }
    }
    stage( 'Read CICD Input' ) {
      steps {
        script {
          cicd = readYaml file: "${CICDCONFIG_FILE_PATH}"
          invName = cicd.env
          logicalBroker = cicd.logicalBroker
          cicdExtraVars = writeJSON returnText: true, json: cicd
        }
        script {
            def responseJson = httpRequest httpMode: 'GET',
                                url: "http://awx-tower-service.awx.svc.cluster.local/api/v2/inventories/?name=${invName}",
                                authentication: 'awx-credentials',
                                validResponseCodes: "200,201"

            // ADD ERROR HANDLING
            def response = readJSON text: responseJson.getContent()

            invId = response.results[0].id

            println( "Found Inventory Name=${invName}, ID=${invId}" )
        }
      }
    }
    stage ('tower') {
        steps {
            script {
                def results = ansibleTower(
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
                println(results.JOB_ID)
                println(results.value)
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