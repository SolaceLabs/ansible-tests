
# Readme

## CI/CD Demo Tools components
- AWX (Ansible Tower open-source version)
- Jenkins
- Solace PubSub+ Brokers
- Kubernetes Ingress

## CI/CD Demo Tools Architecture Overview

### Source Diagrams
[Conceptual Architecture Diagram Source](https://app.diagrams.net/#G1qsAfy3DZwqaD71AB2jaAQ87Ssd9CBvVj)

![CI/CD Demo Tools Architecture Overview](images/EP-Ansible-Jenkins-arch-overview.jpg)

## Pub-Sub+ Brokers

![PubSub+ Broker Inventory](images/pubsubplus-inventory.jpg)

Note the logical and physical broker details.

## Logical Brokers
Logical brokers are labels given to physical brokers for deployment. The purpose is to provide a consistent name for brokers regardless of the environment. These labels exist in all environments. A logical broker lable may be assigned to multiple physical brokers.

![Logical Broker Map](images/logical-broker-map.jpg)
