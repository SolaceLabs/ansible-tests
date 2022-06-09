#!/usr/bin/env bash

scriptDir=$(cd $(dirname "$0") && pwd);
SOLANS_LOG_DIR=~/logs

if [ ! -d "$SOLANS_LOG_DIR" ] 
then
    mkdir -p $SOLANS_LOG_DIR
fi

# set the python interpreter
  export ANSIBLE_PYTHON_INTERPRETER=$(python3 -c "import sys; print(sys.executable)")

# set verbosity
  export ANSIBLE_VERBOSITY=3

# set the working dir
  WORKING_DIR="$scriptDir/tmp"

# create broker service
##  ansible-playbook "$scriptDir/service.playbook.yml" --extra-vars "WORKING_DIR=$WORKING_DIR"
##  code=$?; if [[ $code != 0 ]]; then echo ">>> XT_ERROR - $code"; exit 1; fi

# configure broker service
  # enable logging
  export ANSIBLE_SOLACE_ENABLE_LOGGING=True
  export ANSIBLE_SOLACE_LOG_PATH="$SOLANS_LOG_DIR/ansible-solace.log"
  ansible-playbook -i "$scriptDir/inventory/inv_both_local_and_cloud.yml" "$scriptDir/config/configure.playbook.yml"
  code=$?; if [[ $code != 0 ]]; then echo ">>> XT_ERROR - $code"; exit 1; fi
