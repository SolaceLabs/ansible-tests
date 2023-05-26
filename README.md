# Ansible Plugin
This project contains:
1. Components and instructions to build the CICD Demo Tools environment in Kubernetes
2. Pipeline and Code Objects for Ansible and Jenkins to provision Solace queues
3. Instructions for calling jobs using REST calls and WebHooks.

Please see documentation at [Architecture Overview](https://github.com/SolaceLabs/ansible-tests/tree/main/cicd-demo-tools) for information regarding the environment.

## Ansible and AWX (Open-source Ansible Tower)
The CI/CD tool sets allow for builds to be executed using Ansible from an SSH server or from Ansible Tower. The SSH server and Ansible tower are pre-loaded with the Solace Ansible Collection to facilitate this process.
- Ansible tower is defined at https://awx-tower.demo.solace.dev
- See [awx/readme.md](https://github.com/SolaceLabs/ansible-tests/tree/main/cicd-demo-tools/awx) for installation instructions.

### Inventory
There are three distinct inventory files:
- development
- test
- preprod

These inventory files correspond to a set of brokers. Refer to the pubsubplus documentation at [Solace Brokers](https://github.com/SolaceLabs/ansible-tests/tree/main/cicd-demo-tools/pubsubplus).

The inventory files are located under the [inventory](./inventory) folder. There are corresponding secrets for sensitive data. The secrets are encrypted using ansible-vault.

### Ansible Playbooks
Ansible playbooks are defined at [playbooks](https://github.com/SolaceLabs/ansible-tests/tree/main/playbooks) folder. There are playbooks to set up the environment and to maintain queues and subscriptions.

## Jenkins
Jenkins is used to execute CI/CD piplines. For more details see [Jenkins docuemntation](./cicd-demo-tools/jenkins)

### Pipelines
- The pipeline scripts are located at [pipelines](https://github.com/SolaceLabs/ansible-tests/tree/main/playbooks)
- Instructions for executing the jobs on Jenkins are located [here](https://github.com/SolaceLabs/ansible-tests/tree/main/cicd-demo-tools/jenkins)
