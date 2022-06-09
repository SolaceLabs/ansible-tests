# ansible-tests
Test Ansible Playbooks using Solace Ansible Galaxy
## Inventory
Refer to inventory file: ```inventory/solace-dev```. Ignore the rest. Note that addition of an extention such as **.yaml** will make an inventory file unrecognizable to AWX. It can be a yaml file, but the extension can't be used. Not sure if this quirk applies to Ansible Tower/Automation server paid product.
## Playbooks
In playbooks/ folder:
- ```create-or-replace-subscriptions.yaml``` - AWX/Tower playbook to create or replace subscriptions on queue
    - extra_vars.subscriptionMode: **create** | replace
    - If **create** (default), then add subscriptions in list if not present on the queue.
    - If **replace**, then replace the current list with the list provided.
    - extra_vars.subscriptions: [ "subscription/one", "subscription/two", "subscription/N" ]
- ```create-or-replace-subscriptions.yaml``` - AWX/Tower playbook to delete subscriptions on queue
    - extra_vars.subscriptionMode: **explicit** | all
    - If **explicit** (default), then delete subscriptions in provided list:
        - extra_vars.subscriptions: [ "subscription/one", "subscription/two", "subscription/N" ]
