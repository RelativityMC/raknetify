#!/bin/bash

loader_version=0.14.6
installer_version=0.11.0

declare -a vers
if [[ -z "$@" ]]; then
  vers="1.16.5 1.17 1.17.1 1.18.1 1.18.2 1.19 1.19.1-pre4"
else
  vers=$@
fi

echo $vers

./gradlew clean build || exit 1

mkdir run-mappingsGen || true
cd run-mappingsGen
rm -r mods
mkdir mods
cp ../fabric/build/libs/raknetify-*-all.jar mods/
rm channelMappings.json || true
if [[ -z "${MAPPINGS_GEN_FRESH}" ]]; then
  cp ../common/src/main/resources/raknetify-channel-mappings.json channelMappings.json
fi

for version in $vers
do
  wget -O fabric-server-mc$version.jar https://meta.fabricmc.net/v2/versions/loader/$version/$loader_version/$installer_version/server/jar || exit 1

  sleep 1

  java -Draknetify.saveChannelMappings=true -Draknetify.saveChannelMappings.exit=true -jar fabric-server-mc$version.jar || exit 1
done

cp channelMappings.json ../common/src/main/resources/raknetify-channel-mappings.json
