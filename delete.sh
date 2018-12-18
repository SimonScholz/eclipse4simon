#!/bin/bash
files=(
artifacts.jar

content.jar
)

user=simon-scholz
token=secret
deletePath=simon-scholz/eclipse-apps/eclipse4simon/0.3.0

for item in ${files[*]}; do
	curl -u $user:$token -X "DELETE" https://api.bintray.com/content/$deletePath/$item
done



