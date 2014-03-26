jremotelog
==========

Remote encrypted logs using Java and Loggly

Usage
-----

Build with Maven.

Register with loggly.com and make a note of the access token. 

Generate an AES key in hex format e.g.

```head -c 1024 /dev/urandom | sha256sum | cut -c 1-32```

Place a copy of src/main/resources/example.jremotelog.properties in /etc/jremotelog.properties

Modify jremotelog.properties with the loggly access token, AES key, and name of the file you want to tail.

Run ```java -jar jremotelog.jar```

The log file will be tailed, encrypted, and periodically uploaded to loggly.

Check you can retrieve the logs in the next section. Don't wait till you need to look at the logs to do this.

Retrieving logs
---------------

Edit a copy of src/main/resources/example.retrieve.properties and add the AES key.

Run ```java -cp jremotelog.jar eu.ocathain.jremotelog.viewer.ExistingLogViewer [location of jremotelog.retrieve.properties]```
