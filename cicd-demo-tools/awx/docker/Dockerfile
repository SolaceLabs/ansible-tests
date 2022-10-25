##  Dockerfile for Ansible Tower with solace ansible collecdtion
##  Image based on AWS-EE
##  - quay.io/ansible/awx-ee:latest
##  Install:
##  - Solace PubSub+ Ansible Collection
##
##  Base Image: PRETTY_NAME="CentOS Stream 8"
##
##  BUILD:
##  docker build -t ghcr.io/solacelabs/awx-solace:1.0 .
##  docker push ghcr.io/solacelabs/awx-solace:1.0
  
## DECLARE BASE IMAGE
FROM quay.io/ansible/awx-ee:latest
## USER runner
## K8S fails to create pod if user != root
USER root

##  INSTALL SOLACE ANSIBLE COLLECTION
##
COPY requirements.txt .
RUN yum -y upgrade python3
RUN python3 -m pip install --upgrade pip
RUN pip install -r requirements.txt
RUN ansible-galaxy collection install solace.pubsub_plus
