#!/bin/sh

TOKEN=$(curl -s 'https://discovery.etcd.io/new?size=3' 2> /dev/null)

MACHINE_TYPE=f1-micro
#MACHINE_TYPE=n1-standard-1
#MACHINE_TYPE=n1-highcpu-2


sed "s|{DISCOVERY_TOKEN}|$TOKEN|g" < cloud-config-template.yaml > cloud-config.yaml

CMD="gcloud compute instances create pan-node1 pan-node2 pan-node3 \
	--image https://www.googleapis.com/compute/v1/projects/coreos-cloud/global/images/coreos-stable-835-9-0-v20151208 \
	--machine-type $MACHINE_TYPE \
	--tags http-server,bot-server,pan-node \
	--scopes 'https://www.googleapis.com/auth/cloud-platform' \
	--metadata-from-file user-data=cloud-config.yaml"
echo $CMD
eval $CMD

/bin/sh fleet_init.sh

