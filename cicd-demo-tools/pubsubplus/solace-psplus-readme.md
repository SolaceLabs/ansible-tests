# Solace PubSub+ Brokers

## Install using Helm

The instructions below will create solace pubsub+ brokers with ClusterIP services. If you wish messaging services to be exposed externally, then they should be created with service type = LoadBalancer.

```bash
kubectl create namespace solace
helm install dev1 solacecharts/pubsubplus \
--namespace solace \
--set solace.usernameAdminPassword=[your admin password] \
--set solace.size=dev,service.type=ClusterIP \
--set storage.size=10Gi,serviceAccount.create=true

helm install test1 solacecharts/pubsubplus \
--namespace solace \
--set solace.usernameAdminPassword=[your admin password] \
--set solace.size=dev,service.type=ClusterIP \
--set storage.size=10Gi \
--set serviceAccount.create=false,serviceAccount.name=dev1-pubsubplus-sa

helm install preprod1 solacecharts/pubsubplus \
--namespace solace \
--set solace.usernameAdminPassword=[your admin password] \
--set solace.size=dev,service.type=ClusterIP \
--set storage.size=10Gi \
--set serviceAccount.create=false,serviceAccount.name=dev1-pubsubplus-sa

## Etc.
```

## Pub-Sub+ Brokers

![PubSub+ Broker Inventory](../images/pubsubplus-inventory.jpg)

Note the logical and physical broker details.

## Logical Brokers
Logical brokers are labels given to physical brokers for deployment. The purpose is to provide a consistent name for brokers regardless of the environment. These labels exist in all environments. A logical broker lable may be assigned to multiple physical brokers.

![Logical Broker Map](../images/logical-broker-map.jpg)
