# Jenkins Component

## Install Jenkins using helm chart

1. Get the Jenkins chart:
   ```bash
   helm repo add jenkins https://charts.jenkins.io
   helm repo update
   ```
1. Follow instructions on GitHub to configure the installation: [Jenkins Community Helm Charts](https://github.com/jenkinsci/helm-charts)
1. Create Deployment for SSH agent with Solace Ansible Collection installed
    - IF you need to build the agent image, then you will first need to build the asyncapi-parser project and copy the jar file into docker build directory.
       The image should be available at ```ghcr.io/solacelabs/solace-jenkins-ssh:1.0```
    - Create the deployment:
       ```bash
       kubectl apply -f https://raw.githubusercontent.com/SolaceLabs/ansible-tests/cicd-demo-tools/jenkins/k8s/solace-jenkins-agent-ssh.yaml
       ```
    - Using the Jenkins UI, add the Solace Ansible SSH agent

## Configure and Execute Jobs on Jenkins
Job scripts are are defined in [Ansible Tests](https://github.com/SolaceLabs/ansible-tests)

## REST Calls

### Ansible – REST – from AsyncAPI File
Jenkins job to build using Ansible (without Tower) from AsyncApi input. Initiated from REST call.

- Jenkins Job: build-ansible-asyncapi-rest
- REST URL: ```https://jenkins.demo.solace.dev/job/build-ansible-asyncapi-rest/buildWithParameters```
- Basic Auth: jenkins-sa / REST API Token as Password
- Body: FORM-URLENCODED PARAMS

|Item|Required|Description|Req Details|
|---|---|---|---|
|**REPO_HTTP_URL**|Yes|HTTP URL to containing AsyncApi to build. Must be public||
|**REPO_BRANCH**|No|Repo branch to checkout and build. Assumes “main” if not present|Not required but should be provided to specify branch without assumptions|
|**ASYNCAPI_FILE**|No|Location of the AsyncAPI file in the project.|If not present, defaults to asyncapi/asyncapi.yaml|
|**BUILD_ENV**|No|The name of the environment (inventory) to provision|If not present, will look for BUILD_ENV_FILE in build project|
|BUILD_ENV_FILE|No|Optional method of passing the environment (Inventory) to provision. Use relative path.|Used if BUILD_ENV not set|

### AWX Tower – REST – from AsyncAPI File
Jenkins job to build using AWX / Ansible Tower from AsyncApi input. Initiated from Webhook.

- Jenkins Job: build-awx-asyncapi-rest
- REST URL: ```https://jenkins.demo.solace.dev/job/build-ansible-asyncapi-rest/buildWithParameters```
- Basic Auth: jenkins-sa / REST API Token as Password
- Body: FORM-URLENCODED PARAMS

Item|Required|||
|---|---|---|---|
|**REPO_HTTP_URL**|Yes|HTTP URL to containing AsyncApi to build. Must be public||
|**REPO_BRANCH**|No|Repo branch to checkout and build. Assumes “main” if not present|Not required but should be provided to specify branch without assumptions|
|**ASYNCAPI_FILE**|No|Location of the AsyncAPI file in the project.|If not present, defaults to asyncapi/asyncapi.yaml|
|**BUILD_ENV**|No|The name of the environment (inventory) to provision|If not present, will look for BUILD_ENV_FILE in build project|
|BUILD_ENV_FILE|No|Optional method of passing the environment (Inventory) to provision. Use relative path.|Used if BUILD_ENV not set|

## Webhooks

### Ansible – Webhook – from AsyncApi File
Jenkins job to build using Ansible (without Tower) from AsyncApi input. Initiated from Webhook.

- Jenkins Job: build-ansible-asyncapi-webhook
- Webhook URL: ```https://jenkins.demo.solace.dev/generic-webhook-trigger/invoke?token=buildFromAsyncApiWithAnsibleTrigger```
 
|Item|Location|Required|Description|
|---|---|---|---|
|AsyncAPI File|asyncapi/asyncapi.yaml|Yes|The AsyncAPI Spec
|Build Env File|.jenkins/build-env.yaml|No|Set the environment name (inventory) to provision. If not present, will use the inventory name == Git branch

### AWX Tower – Webhook – from AsyncAPI File
Jenkins job to build using AWX / Ansible Tower from AsyncApi input. Initiated from Webhook.

- Jenkins Job: build-awx-asyncapi-webhook
- Webhook URL: ```https://jenkins.demo.solace.dev/generic-webhook-trigger/invoke?token=buildFromAsyncApiWithAwxTrigger```
 
|Item|Location|Required|Description|
|---|---|---|---|
|AsyncAPI File|asyncapi/asyncapi.yaml|Yes|The AsyncAPI Spec|
|Build Env File|.jenkins/build-env.yaml|No|Set the environment name (inventory) to provision. If not present, will use the inventory name == Git branch|
