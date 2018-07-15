# How to release Armeria to the Sonatype?

### Initial setup

- [Create your JIRA account in Sonatype](https://issues.sonatype.org/secure/Signup!default.jspa)
- Create a wiki page that demands the deployable role (It takes a day or two days to get the permission.) 
- Working with the PGP signature
  - Install GnuPG using "$ apt-get install GnuPG"
  - Create your own PGP Signature
    - "$ gpg --gen-key" (Note that generating key without passphrase is not a problem.)
    - "$ gpg --list-secret-keys" will show your pgp key ID
      - If the key ID is longer than 8 digits, the last 8 digits is the key ID
  - Test your key using
    - "$ gpg -ab temp.java"  will create temp.java.asc which is the signature of temp.java
    - Verify it using your public key
    - "$ gpg --verify temp.java.asc"
  - Send your key to the key server
    - "$ gpg --keyserver hkp://pool.sks-keyservers.net --send-keys youKeyId"
    - Check whether it is uploaded or not [key server](https://pgp.mit.edu/).
  - Store your information in the ~/.gradle/gradle.properties
      ```
      signing.keyId=
      signing.password=
      signing.secretKeyRingFile=/Users/Yourname/.gnupg/secring.gpg
      ossrhUsername=
      ossrhPassword=
      ```
  - If you don't have the secring.gpg file, then create it using:
    - "gpg --export-secret-key yourKeyId  > ~/.gnupg/secring.gpg"
  - [More detailed instruction](https://central.sonatype.org/pages/working-with-pgp-signatures.html) about PGP signature

### Uploading to the staging repository

- Clone the repository into your local
  - "$ git clone git@github.com:line/armeria.git upstream-armeria"
  - "$ git clone git@github.com:line/armeria.git site-armeria"
- Install with sign
  - In ./upstream-armeia "$ ./gradlew -Psign install --no-daemon"
- Verify it
  - "$ gpg --verify ./core/build/libs/armeria-0.XY.0-SNAPSHOT-javadoc.jar.asc"
- Release
  - "$ ./gradlew release -PreleaseVersion=0.XY.0 -PnextVersion=0.(XY+1).0"
  - "$ git checkout armeria-0.XY.0"
  - "$./gradlew clean publish site --no-daemon"
- Close it
  - Find in https://oss.sonatype.org/#stagingRepositories
  - Close by clicking close button.

### Releasing the jars

- Release
  - Push the release button.
  - check the checkbox about "Release automatically"
- Follow the instruction from the gradle log
- Check out at http://line.github.io/armeria/
- Write the release note
