def cicd
def invName
def invId
def logicalBroker
def cicdExtraVars
//def branch
pipeline {
  agent { label 'tower' }
  parameters {
    string( name:           'BUILD_ENV',
            defaultValue:   '##_DEFAULT_##', 
            description:    'Build Environment ID')
    string( name:           'REPO_BRANCH',
            defaultValue:   'main', 
            description:    'Git branch to build')
    string( name:           'REPO_HTTP_URL',  
            defaultValue:   'https://github.com/PATH/TO/REPO',              
            description:    'HTTP URL of the repo with AsyncAPI Info' )
    // string( name:           'WEBHOOK_REPO_SSH_URL',  
    //         defaultValue:   'ssh://git@github.com/PATH/TO/REPO',              
    //         description:    'SSH URL of the repo with AsyncAPI Info' )
    string( name:           'ASYNCAPI_FILE',
            defaultValue:   'asyncapi/asyncapi.yaml',
            description:    'The location of the AsyncAPI file in the repository' )
    string( name:           'BUILD_ENV_FILE',
            defaultValue:   '.jenkins/build-env.yaml',
            description:    'The location of the Build Environment file in the repository' )
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
            // script {
            //    def values = "${WEBHOOK_REF}".split('/')
            //    branch = values[2]
            //    println( "Found Branch: ${branch}" )
            // }
            script {
                dir( "${BUILD_DIR}" ) {
                    git branch: "${REPO_BRANCH}", url: "${REPO_HTTP_URL}"
                }
            }
        }
    }
    stage( 'Extract CICD' ) {
      steps {
        script {
          def buildEnv = "${BUILD_ENV}"
          def defaultBuildEnvValue = "##_DEFAULT_##"
          if (buildEnv == defaultBuildEnvValue) {
            def buildEnvExists = fileExists "${BUILD_DIR}/${BUILD_ENV_FILE}"
            if ( buildEnvExists == true ) {
              def buildEnvParams = readYaml file: "${BUILD_DIR}/${BUILD_ENV_FILE}"
              buildEnv = buildEnvParams.buildEnv
            } else {
              buildEnv = "${REPO_BRANCH}"
            }
          }
          sh "mkdir -p ${TMP_DIR} && rm -f ${CICDCONFIG_FILE}"
          sh "java -jar ${JAR_CICD_EXTRACT} --asyncapi-in=${BUILD_DIR}/${ASYNCAPI_FILE} --output=${CICDCONFIG_FILE} --target-server=${buildEnv}"
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
          // if ( invName != branch ) {
          //   println( "### THE [cicd_spec.environment] != [branch] of the Repo; EXITING ###" )
          //   error('Aborting the build.')
          // }
        }
      }
    }
    stage( 'lookup tower invId' ) {
      steps {
        script {
            def responseJson = httpRequest httpMode: 'GET',
                                url: "http://awx-tower-service.awx.svc.cluster.local:9080/api/v2/inventories/?name=${invName}",
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