#!/bin/bash
user=simon-scholz
token=secret
path=simon-scholz/eclipse-apps/eclipse4simon/0.3.0

curl -T ./releng/update-site/target/update-site-1.0.0-SNAPSHOT.zip -u $user:$token -H "X-Bintray-Explode: 1" -H "X-Bintray-Package:eclipse4simon" -H "X-Bintray-Version:0.3.0" https://api.bintray.com/content/$path
