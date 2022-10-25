# Install AWX / Ansible Tower Open Source

## AWX Operator
AWX uses a Kubernetes operator to manage installation. The Kubernetes operator is installed using a helm chart. An AWX is resource is then declared, which creates all of the associated resources.

The Operator source code and configuration instructions can be found on GitHub at:
[AWX Operator](https://github.com/ansible/awx-operator)

## Pre-requisites
- Kubernetes environment with sufficient resources must be in place. See the ```k8s``` folder to examine the CRDs and their resource requests.
- As currently defined, the AWX CRD will create the AWX instance with a ClusterIP service. This method assumes that the server will either be
    - exposed internally only or
    - use a kubernetes ingress to expose the service externally.
- You can update the service be created as a load balancer, but TLS must then be configured.
- If you are planning to use a load balancer with TLS or require other special configurations, then instructions can be found on GitHub here: [AWX Operator Helm Install](https://github.com/ansible/awx-operator#helm-install-on-existing-cluster)
- There is an awx-admin-password.yaml CRD that defines a Kubernetes secret in the ```awx``` namespace. This CRD should be updated with a strong password prior to installing the AWX resource. You can also create a password secret directly from the command line.


## Install AWX Operator using Helm

1. Get the helm chart for AWX Operator
   ```bash
   helm repo add awx-operator https://ansible.github.io/awx-operator/
   helm repo update
   ```
1. Create namespace ```awx```
   ```bash
   kubectl create namespace awx
   ```
1. Install the Kubernetes Operator
   ```bash
   helm install -n awx awx-operator awx-operator/awx-operator
   ```
1. Create the admin password secret. You can use the resource (with updated password) or follow kubernetes instructions to create a secret from the command line.
   ```bash
   kubectl apply -f "https://raw.githubusercontent.com/SolaceLabs/ansible-tests/cicd-demo-tools/awx/k8s/awx-admin-password.yaml"
   ```
1. Declare AWX resource to install AWX Tower
   ```bash
   kubectl apply -f "https://raw.githubusercontent.com/SolaceLabs/ansible-tests/cicd-demo-tools/awx/k8s/awx-tower.yaml"
   ```

## Configure AWX Tower
When connectivity has been established, you will need to configure the server.
The following image should be used to execute builds using Solace Ansible Collection:
```ghcr.io/solacelabs/awx-solace:1.0```

