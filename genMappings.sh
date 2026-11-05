#!/bin/bash

loader_version=0.20.1
installer_version=1.1.1

#declare -a vers
if [[ -z "$@" ]]; then
  vers="1.18.1 1.18.2 1.19 1.19.2 1.19.3 1.19.4 23w13a_or_b 1.20.1 1.20.2 1.20.4 1.20.6 1.21.1 1.21.3 1.21.4 1.21.5 1.21.6 1.21.8 1.21.10 1.21.11 26.1 26.1.1 26.1.2 26.2 26.3 26.4-snapshot-7"
else
  vers=$@
fi

echo $vers

./gradlew clean build || exit 1

mkdir run-mappingsGen || true
cd run-mappingsGen  || exit 1
rm -r mods
mkdir mods
rm channelMappings.json || true
if [[ -z "${MAPPINGS_GEN_FRESH}" ]]; then
  cp ../common/src/main/resources/raknetify-channel-mappings.json channelMappings.json
fi

for version in $vers
do
  rm -f mods/raknetify-*-all.jar
  if [[ "$version" == 26.* ]]; then
    cp ../fabric-26/build/libs/raknetify-*-all.jar mods/ || exit 1
  else
    cp ../fabric/build/libs/raknetify-*-all.jar mods/ || exit 1
  fi

  launcher="fabric-server-mc${version}-loader${loader_version}-installer${installer_version}.jar"
  if [ -e "$launcher" ]; then
    echo "Using existing $launcher"
  else
    wget -O "$launcher" https://meta.fabricmc.net/v2/versions/loader/"$version"/$loader_version/$installer_version/server/jar || exit 1
  fi

  sleep 1

  touch .mapping-generation-start
  java -Draknetify.saveChannelMappings=true -Draknetify.saveChannelMappings.exit=true -jar "$launcher" || exit 1
  if [ ! channelMappings.json -nt .mapping-generation-start ]; then
    echo "Mapping generation did not write output for $version"
    exit 1
  fi
done

cp channelMappings.json ../common/src/main/resources/raknetify-channel-mappings.json
