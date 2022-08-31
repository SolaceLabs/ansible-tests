def cicd
def invName
def logicalBroker
def cicdExtraVars
def branch
def secretsFile
pipeline {
  agent { label 'ansible' }
  parameters {
    string( name:           'WEBHOOK_REF',
            defaultValue:   'refs/heads/main', 
            description:    'refs from GitHook POST body')
    string( name:           'WEBHOOK_REPO_HTTP_URL',  
            defaultValue:   'https://github.com/PATH/TO/REPO',              
            description:    'HTTP URL of the repo with AsyncAPI Info' )
    // string( name:           'WEBHOOK_REPO_SSH_URL',  
    //         defaultValue:   'ssh://git@github.com/PATH/TO/REPO',              
    //         description:    'SSH URL of the repo with AsyncAPI Info' )
    string( name:           'ASYNCAPI_FILE',
            defaultValue:   'asyncapi/asyncapi.yaml',
            description:    'The location of the AsyncAPI file in the repository' )
    // string( name:           'REPO_CREDS_ID',
    //         defaultValue:   'my-jenkins-credentials-id',
    //         description:    'The credentials used to checkout from the repo' )    
  }
  environment {
    BUILD_DIR = "__BUILD_DIR__"
    TMP_DIR = "tmp"
    CICDCONFIG_FILE = "${TMP_DIR}/generated-cicd-config.yaml"
    JAR_CICD_EXTRACT = "/home/jenkins/exec-jars/asyncapi-cicd-parser-0.1.1.jar"
  }
  stages {
    stage( 'Checkout' ) {
        steps {
            script {
               def values = "${WEBHOOK_REF}".split('/')
               branch = values[2]
               println( "Found Branch: ${branch}" )
//               secretsFile = "secrets/${branch}_secrets.encrypted"
            }
            script {
                dir( "${BUILD_DIR}" ) {
                    git branch: "${branch}", url: "${WEBHOOK_REPO_HTTP_URL}"
                }
            }
        }
    }
    stage( 'Extract CICD' ) {
      steps {
        script {
          sh "mkdir -p ${TMP_DIR} && rm -f ${CICDCONFIG_FILE}"
          sh "java -jar ${JAR_CICD_EXTRACT} --asyncapi-in=${BUILD_DIR}/${ASYNCAPI_FILE} --output=${CICDCONFIG_FILE} --target-server=${branch}"
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
          secretsFile = "secrets/${invName}_secrets.encrypted"
        }
      }
    }
    stage ('ansible build') {
      steps {
        withCredentials([file(credentialsId: 'ansible-vault-password', variable: 'vault_passwd_file')]) {
          ansiblePlaybook extras: "-e '${cicdExtraVars}' -e @${secretsFile}", 
                          installation: 'solace-ansible-install', 
                          inventory: "inventory/${invName}", 
                          limit: "${logicalBroker}", 
                          playbook: 'playbooks/create-multi-queue-control.yaml', 
                          vaultCredentialsId: 'ansible-vault-password'  

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