#!/bin/bash

docker run --rm -it -v "`pwd`/data:/workspace/data" ldap2nextcloud:0.0.1-SNAPSHOT 
