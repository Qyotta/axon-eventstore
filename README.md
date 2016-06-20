# axon-eventstore
An implementation of the axon EventStore using Greg Young's Event Store (https://geteventstore.com/) to use with the Axon Framework (http://www.axonframework.org)

## What you get
* A simple java client for Greg Young's Event Store.
* An implementation of the Axons EventStore to use Greg Young's Event Store within Axon.

## Limitations
* The java client only supports forward iteration over all events in a single stream.
* The Axon event store does not implement UpcasterAware.

# Quickstart
* Make sure you have Greg Young's Event Store installed (This library was impemented and tested using version 3.6.3)
* Clone the repository
* install with mvn clean install
* add the maven dependency to your project

```xml
<dependency>
  <groupId>de.qyotta.eventstore</groupId>
  <artifactId>axon-eventstore</artifactId>
  <version>0.1-SNAPSHOT</version>
</dependency>
```

