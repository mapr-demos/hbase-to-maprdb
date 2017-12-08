# Hbase To MapR-DB Migration
This repository contains sample applications, and documentation explaining how to move your Hbase application to MapR-DB Bninary
Sample MapR-DB applications based on Twitabse apps for both sync and async APIs.

## Sync API

MapR-DB supports most of the Apache HBase 1.1 Java APIs that allows you to easily port existing HBase applications to 
use MapR-DB binary tables. Note, that MapR-DB binary tables do not support low-level HBase API calls that are used to 
manipulate the state of an Apache HBase cluster. HBase API calls that are not supported by MapR-DB tables report 
successful completion to allow legacy code written for Apache HBase to continue executing, but do not perform any 
actual operations.

### Maven dependencies

For applications that use the MapR-DB Java API for binary tables, use Maven to compile and determine the application's 
dependencies. 

Add MapR's maven repository to the list of repositories in your application's pom.xml, if it is not there already:
```xml
<repository>
      <id>mapr-releases</id>
      <url>http://repository.mapr.com/maven/</url>
      <snapshots><enabled>true</enabled></snapshots>
      <releases><enabled>true</enabled></releases>
</repository>
```

Add a dependency to the HBase client project:
```xml
<dependency>
  <groupId>org.apache.hbase</groupId>
  <artifactId>hbase-client</artifactId>
  <version>${hbase.version}</version>
</dependency>
```

Original Twitbase application uses older Hbase API version, so we have to include `hbase-server` dependency to run 
Map Reduce jobs:
```xml
<dependency>
    <groupId>org.apache.hbase</groupId>
    <artifactId>hbase-server</artifactId>
    <version>${hbase.version}</version>
</dependency>
```

### Changing API Version

We will use `1.1.8-mapr-1710` Hbase API version insted of `0.92.1`, which is used by original Twitbase application. Some 
of classes and methods from API are deprecated at `1.1.*`. To avoid using such classes and methods, they should be 
replaced by analogues from newer API. 

Below listed some of such analogues along wth short description:

| Old API                     | New API                     | Description                                             |
| --------------------------- | --------------------------- | ------------------------------------------------------- |
| new HBaseAdmin()            | connection.getAdmin()       | Instance of Admin can be get through Connection         |
| HTablePool                  | Connection                  | Use Connection interface instead of HTablePool          |
| KeyValue                    | Cell                        | KeyValue contains a lot of deprecated methods           |
| byte[] table                | TableName table             | Use TableName class, which represents a table name      |
| HTableInterface             | Table                       | Obtain an instance from a Connection                    |


### Connection configuration

Use snippet, listed below to obtain instance of `Connection`:
```
    Configuration conf = HBaseConfiguration.create();
    conf.set("hbase.zookeeper.property.clientPort", "5181");

    Connection connection = ConnectionFactory.createConnection(conf);
```

Note, that MapR ZooKeeper's default port is `5181` instead of `2181`.

### Table name mismatch

MapR's implementations of the HBase Java API differentiate between Apache HBase tables and MapR-DB tables according to 
table names. In general, if a table name includes a slash (`/`), the name is assumed to be a path to a MapR-DB table, because slash 
is not a valid character for Apache HBase table names. 

The easiest way is to change Twitbase's table names to start with `/`, although it's possible to adjust table names 
mappings using `hbase.table.namespace.mappings` property of `/opt/mapr/hadoop/hadoop-<version>/etc/hadoop/core-site.xml` 
configuration file.

### Coprocessors support

Currently, HBase co-processors are not supported.

### Custom filters support

Twitbase app uses `PasswordStrengthFilter` to demonstrate custom HBase filters. Currently, such custom filters are not 
supported. We can replace such custom filter with supported `SingleColumnValueFilter`, which is based on 
`RegexStringComparator`. Thus, we can reproduce `PasswordStrengthFilter` behaviour to filter passwords, which are longer 
than `3` using regular expression.

### Changing launcher script

When you run the application, you must include its dependencies. If the cluster is secure, the node must also have a 
mapr ticket configured for the user that runs the application.

While you can use Maven to initially determine the application's dependencies, it is recommended that you specify 
dependencies with a classpath when you submit the application to the cluster. Bundling dependencies JAR files with the 
application may cause the cluster to shut down unexpectedly. 

Thus, launcher script must be changed to append `habse client` output to the application's classpath:
```
...

java -cp `hbase classpath`:target/twitbase-1.0.0.jar -Djava.library.path=/opt/mapr/lib $CLASS "$@"
```

## Async API

### Installing AsyncHBase Libraries

Based on your operating system, run one of the following commands to install the package:

* On Red Hat /Centos:

`yum install mapr-asynchbase`

* On SUSE:

`zypper install mapr-asynchbase`

* On Ubuntu:

`apt-get install mapr-asynchbase`

Run the `configure.sh` script with the following command to configure the AsyncHBase role for the node:
`/opt/mapr/server/configure.sh -R`

### Maven dependencies

For applications that use the MapR-DB Java API for binary tables, use Maven to compile and determine the application's 
dependencies. 

Add MapR's maven repository to the list of repositories in your application's pom.xml, if it is not there already:
```xml
<repository>
      <id>mapr-releases</id>
      <url>http://repository.mapr.com/maven/</url>
      <snapshots><enabled>true</enabled></snapshots>
      <releases><enabled>true</enabled></releases>
</repository>
```

Add a dependency to the HBase Async client project:
```xml
<dependency>
    <groupId>org.hbase</groupId>
    <artifactId>asynchbase</artifactId>
    <version>1.7.0-mapr-1607</version>
</dependency>
```

### Connection configuration

Note, that MapR ZooKeeper's default port is `5181` instead of `2181`. Thus, we must specify ZooKeepr's port while 
creating HBase Client:
```
    final HBaseClient client = new HBaseClient("localhost:5181");
```

### Table name mismatch

MapR's implementations of the HBase Java API differentiate between Apache HBase tables and MapR-DB tables according to 
table names. In general, if a table name includes a slash (`/`), the name is assumed to be a path to a MapR-DB table, because slash 
is not a valid character for Apache HBase table names. 

The easiest way is to change Twitbase's table names to start with `/`, although it's possible to adjust table names 
mappings using `hbase.table.namespace.mappings` property of `/opt/mapr/hadoop/hadoop-<version>/etc/hadoop/core-site.xml` 
configuration file.

### Running AsyncHBase Application

To run the application, use one of the following commands:
```
$ java -cp `asynchbase classpath`:$APP_CLASSPATH <ProgramName>
```

Or:
```
$ asynchbase $APP_CLASSPATH <ProgramName>
```

