
# AWX / Ansible Tower

- Ansible Tower is server used to execute ansible jobs
- AWX is the open source version of Ansible Tower

# Brokers in Inventory
## Software
http://35.232.8.227:8080

## Cloud
https://mr6p7iajy93k7.messaging.solace.cloud:943

# Project
Inventory, playbooks, and examples are stored in a project on GitHub. AWX references the GitHub project directly to import inventory and playbooks.

## Ansible Project on GitHub
https://github.com/dennis-brinley/ansible-tests

**Contents:**
- playbooks/ - Contains the playbooks used in the jobs
- inventory/ - 
- examples/

Inventory file contains broker definitions:
inventory/solace-dev-brokers --> this is a yaml file, but AWX won't recognize it as inventory with a yaml extension

# AWX Ansible Tower Server
http://35.225.80.104

## REST API

http://35.225.80.104/api/

## Launching a job

**Endpoint:**
http://35.225.80.104:80/api/v2/job_templates/{job-name}/launch/

Jobs

create-or-update-queue
Allows for a PubSub+ broker queue to be created or updated. A subscription list may also be provided with the play.

delete-queue
Self-Explanatory

create-or-replace-subscriptions
delete-subscriptions


Key elements of the POST request payload:

- inventory - This can be used to over-ride which inventory of brokers are subject to configuration in a call. AWX does not accept the inventory name. It expects the primary key (integer). The solace-dev-brokers inventory is referenced by inventory: 9
    - solace-dev-brokers is the default inventory value for the configured jobs and will be used if the value is missing.
- limit - This value is used to limit which brokers will be configured in a call. In general, this value should be populated. If it is not, then all brokers in the inventory will be included in the play. See Inventory Limit below for more explanation.
- extra_vars.queueName - The name of the target queue (always required)
- extra_vars.queueSettings - Queue settings to provide for the play. These settings mirror the JSON SEMP config for queues ***exactly***. This setting applies to create-or-update-queue job only. If settings are not provided, then the broker defaults will be used. 
    - If queueName is provided, it must match the queueName specified outside of the settings.
    - If msgVpnName is provided, it must match the message VPN specified in the inventory file.
    - It is better to exclude queueName and msgVpnName from the queueSettings as they can only cause problems.
    - See below for example queue settings
- extra_vars.subscriptionMode - Subscription mode controls how subscription lists should be applied to a particular queue. This setting applies for any operation where a subscription list is specified. It has different values depending upon context.
ghcr.io/dennis-brinley/awx-solace:latest

Inventory Limit

The ansible inventories maintained use a concept of "logical brokers" to specify a specific physical broker in an environment. This mechanism allows for a consistent logical broker identifier to be presented even the provisioned queues migrate environments with distinct topologies. In the example inventory solace-dev-brokers, there are 2 physical brokers and 4 logical brokers. 

The current mapping in solace-dev-brokers is:
- logical_broker_a --> physical_broker_1 --> Kubernetes Software Broker
- logical_broker_b --> physical_broker_2 --> Cloud service broker
- logical_broker_c --> physical_broker_1 --> Kubernetes Software Broker
- logical_broker_d --> physical_broker_2 --> Cloud service broker

In this mapping, the 4 logical brokers map to 2 physical brokers.

In a higher environment such as integration test, the mapping may be:
- logical_broker_a --> physical_broker_1 --> INT Kubernetes Software Broker
- logical_broker_b --> physical_broker_2 --> Cloud service broker
- logical_broker_c --> physical_broker_3 --> Kubernetes Software Broker
- logical_broker_d --> physical_broker_4 --> Cloud service broker

Here, each logical broker maps to a unique physical broker. When the queues are defined in a tool such as Event Portal, they can be assigned to logical brokers IDs that will be consistent as deployment migrates through environments.

Assigning in ansible plays
To limit the play to a specific broker, specify the logical broker in the call:
```json
"limit": "logical_broker_a"
```

Multiple logical brokers can be specified as a list
```json
"limit": "logical_broker_a logical_broker_b"
```
