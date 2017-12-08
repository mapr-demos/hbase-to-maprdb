# Migration to MapR-DB Binary

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
`java -cp `asynchbase classpath`:$APP_CLASSPATH <ProgramName>`

`asynchbase $APP_CLASSPATH <ProgramName>`
