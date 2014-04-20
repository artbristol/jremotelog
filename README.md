jremotelog
==========

Remote encrypted logs using Java and Loggly.

Prequisites
-----------

Java 1.7

Native `tail` command (due to a bug in Commons IO https://issues.apache.org/jira/browse/IO-279)

Usage
-----

Build with Maven.

Copy ```target/jremotelog-[version]-jar-with-dependencies.jar``` to ```jremotelog.jar```.

Register with loggly.com and make a note of the access token. 

Generate an AES key in hex format e.g.

```head -c 1024 /dev/urandom | sha256sum | cut -c 1-32```

Back up this key somewhere safe.

Place a copy of ```src/main/resources/example.jremotelog.properties``` in ```/etc/jremotelog.properties```.

Modify ```jremotelog.properties``` with the loggly access token, AES key, and name of the file you want to tail.

Run ```java -jar jremotelog.jar```

The log file will be tailed, encrypted, and periodically uploaded to loggly.

Check you can retrieve the logs in the next section. Don't wait till you need to look at the logs to do this.

Retrieving logs
---------------

Do this on a different computer, because that's what you'll be doing when you really need the logs.

Edit a copy of ```src/main/resources/example.retrieve.properties``` and add the AES key and your credentials.

Run ```java -cp jremotelog.jar eu.ocathain.jremotelog.viewer.ExistingLogViewer [location of jremotelog.retrieve.properties] [number of hours]```

Recent log entries will be echoed to stdout.

TODO
----

The IV is generated for each message using a counter. The first counter is generated randomly, but this carries a small risk of collisions when the process restarts. The counter should be persisted between restarts.
