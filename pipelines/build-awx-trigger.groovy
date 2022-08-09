def cicd
def invName
def invId
def logicalBroker
def cicdExtraVars
def branch
pipeline {
  agent { label 'tower' }
  parameters {
    string( name:           'WEBHOOK_REF',
            defaultValue:   'refs/heads/main', 
            description:    'refs from GitHook POST body')
    string( name:           'WEBHOOK_REPO_HTTP_URL',  
            defaultValue:   'https://github.com/PATH/TO/REPO',              
            description:    'HTTP URL of the repo with AsyncAPI Info' )
    string( name:           'WEBHOOK_REPO_SSH_URL',  
            defaultValue:   'ssh://git@github.com/PATH/TO/REPO',              
            description:    'SSH URL of the repo with AsyncAPI Info' )
    string( name:           'CICDCONFIG_FILE',
            defaultValue:   '.jenkins/cicd-extract.yaml',
            description:    'The location of the CICD config file in the repository' )
    string( name:           'REPO_CREDS_ID',
            defaultValue:   'my-jenkins-credentials-id',
            description:    'The credentials used to checkout from the repo' )    
  }
  environment {
    BUILD_DIR = "__BUILD_DIR__/"
    CICDCONFIG_FILE = "${BUILD_DIR}${params.CICDCONFIG_FILE}"
  }
  stages {
    stage( 'Checkout' ) {
        steps {
            script {
               def values = "${WEBHOOK_REF}".split('/')
               branch = values[2]
               println( "Found Branch: ${branch}" )
            }
            script {
                dir( "${BUILD_DIR}" ) {
                    git branch: "${branch}", url: "${WEBHOOK_REPO_HTTP_URL}"
                }
            }
        }
    }
    stage( 'Read CICD Input' ) {
      steps {
        script {
          cicd = readYaml file: "${CICDCONFIG_FILE}"
          invName = cicd.environment
          logicalBroker = cicd.logicalBroker
          cicdExtraVars = writeJSON returnText: true, json: cicd
          if ( invName != branch ) {
            println( "### THE [cicd_spec.environment] != [branch] of the Repo; EXITING ###" )
            error('Aborting the build.')
          }
        }
      }
    }
    stage( 'lookup tower invId' ) {
      steps {
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
                    removeColor: true,
                    verbose: true,
                    async: false
                )
                println(results.JOB_ID)
                println(results.value)
            }
        }
    }
    stage( 'update EP' ) {
        steps {
          script {
            def responseJson
            def epAppVersionUrl = "https://api.solace.cloud/api/v2/architecture/applications/${cicd.applicationId}/versions/${cicd.applicationVersionId}"
            println("call to get event mesh list - START")
            withCredentials([string(credentialsId: 'solace-cloud-authorization-header', variable: 'cloudAuth')]) {
                def authHeader = [ name: 'Authorization', value: "${cloudAuth}", maskValue: true ]
                def custHeaders = [ authHeader ]
                responseJson = httpRequest httpMode: 'GET',
                                url: "${epAppVersionUrl}",
                                customHeaders: custHeaders,
                                validResponseCodes: "200,201"
            }
            // ADD ERROR HANDLING
            def response = readJSON text: responseJson.getContent()
            def eventMeshes = response.data.eventMeshIds

            def foundMesh = false
                response.data.eventMeshIds.each { val -> 
                  if( val == cicd.modelledEventMeshId ) {
                    foundMesh = true
                  }
            }

            if ( foundMesh == false ) {
              def eventMeshIds = response.data.eventMeshIds
              eventMeshIds.add( cicd.modelledEventMeshId )
              def patchRequest = [ data : [ eventMeshIds: eventMeshIds ] ]
              patchRequest.data.applicationId = cicd.applicationId
              patchRequest.data.id = cicd.applicationVersionId
              patchRequestJson = writeJSON returnText: true, json: patchRequest
              println( "${patchRequestJson}" )
              withCredentials([string(credentialsId: 'solace-cloud-authorization-header', variable: 'cloudAuth')]) {
                  def authHeader = [ name: 'Authorization', value: "${cloudAuth}", maskValue: true ]
                  def custHeaders = [ authHeader ]
                  def patchResponse = httpRequest httpMode: 'PATCH',
                                        url: "${epAppVersionUrl}",
                                        customHeaders: custHeaders,
                                        contentType: 'APPLICATION_JSON',
                                        validResponseCodes: "200,201",
                                        requestBody: "${patchRequestJson}"
              }
            }
          }
        }
    }
  }
}