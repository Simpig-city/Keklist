#!/bin/bash

mvn package -f pom.xml
mkdir -p ./testserver/plugins
cp ./target/keklist-1.0-SNAPSHOT.jar ./testserver/plugins

cd ./testserver || exit

build=$(curl -s -L "https://api.purpurmc.org/v2/purpur/1.20.4/latest/" | jq -r '.build')
currentVersion=$(jq -r '.currentVersion' version_history.json)

echo "Latest build: git-Purpur-$build (MC: 1.20.4)"
echo "Current version: $currentVersion"
# Compare the two values and output the result
if [[ "git-Purpur-$build (MC: 1.20.4)" == "$currentVersion" ]]; then
  echo "The server build is up-to-date."
else
  echo "The build is out-of-date. Downloading the latest one..."
  curl -L https://api.purpurmc.org/v2/purpur/1.20.4/latest/download -o server.jar
fi
java -Xms4096M -Xmx4096M --add-modules=jdk.incubator.vector -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch -XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=4 -XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90 -XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem -XX:MaxTenuringThreshold=1 -Dusing.aikars.flags=https://mcflags.emc.gs -Daikars.new.flags=true -XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M -XX:G1ReservePercent=20 -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 -jar server.jar