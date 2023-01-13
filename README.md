- Served at https://github.com/oracle/graalvm-reachability-metadata/pull/170
  and https://github.com/mockito/mockito/issues/2862
- It can be reproduced in an Ubuntu instance by performing the following steps.

```shell
sudo apt install unzip zip curl sed -y
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 22.3.r17-grl
sdk use java 22.3.r17-grl
gu install native-image
sudo apt-get install build-essential libz-dev zlib1g-dev -y

git clone git@github.com:linghengqian/mockito-junit-platform-test.git
cd ./mockito-junit-platform-test/
./gradlew -Pagent clean test
./gradlew metadataCopy --task test
./gradlew clean nativeTest

```