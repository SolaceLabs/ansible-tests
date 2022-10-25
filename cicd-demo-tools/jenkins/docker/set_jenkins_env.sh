# set_jenkins_env.sh -- Extra work for the environment
#
# THIS SCRIPT ADDS A COMMAND TO .bashrc THAT EXECUTES WHEN SSH SHELL IS STARTED
# THE COMMAND CAUSES THE ENV VARS IN /etc/environment TO BE ADDED TO THE SHELL
# FOR uid=0 (root)
#
cat > ~/__TEMP.bashrc << 'EOF'
export $(grep -vE '^#|^JENKINS_AGENT_SSH_PUBKEY' /etc/environment | xargs)
EOF
cat ~/.bashrc >> ~/__TEMP.bashrc
mv ~/.bashrc ~/__BACKUP.bashrc
mv ~/__TEMP.bashrc ~/.bashrc
