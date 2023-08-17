#/bin/bash

TIMESTAMP=`date +%Y%m%d-%H%M`

echo $TIMESTAMP > data/timestamp

java -cp "./lib/*" nl.esciencecenter.rsd.HarvestRSD

