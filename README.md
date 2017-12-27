# Hbase To MapR-DB Migration
This repository contains sample applications, and documentation explaining how to move your Hbase application to MapR-DB Bninary
Sample MapR-DB applications based on Twitabse apps for both sync and async APIs.

## Migration of the data from HBase to MapR-DB Binary

If you were previously using HBase tables on your cluster, prepare your cluster to use MapR-DB binary tables.

MapR-DB tables can be parsed by the Apache CopyTable tool. You can use the CopyTable tool to migrate data from an Apache 
HBase table to a MapR-DB binary table.

### Prerequisites

When migrating to MapR-DB binary tables, change your Apache HBase client to the MapR client by installing the version of 
the `mapr-hbase` package that matches the version of Apache HBase on your source cluster.

See [Installing MapR Software](https://maprdocs.mapr.com/60/AdvancedInstallation/InstallingMapRSoftware.html) for 
information about MapR installation procedures, including setting up the proper repositories.

### Example

For the sake of example, we will setup standalone HBase 1.18 on single-node cluster. Note, that `mapr-hbase` package is 
installed already.

```

$ wget http://archive.apache.org/dist/hbase/1.1.8/hbase-1.1.8-bin.tar.gz

$ tar -xzvf hbase-1.1.8-bin.tar.gz

$ cd hbase-1.1.8/

```

Paste the following configuration intro `conf/hbase-site.xml` to run HBase in Standalone mode:
```

<configuration>
  <property>
    <name>hbase.rootdir</name>
    <value>file:///home/mapr/hbase-1.1.8</value>
  </property>
  <property>
    <name>hbase.zookeeper.property.dataDir</name>
    <value>/home/mapr/zookeeper</value>
  </property>
</configuration>

```

Start HBase:
```
$ ./bin/start-hbase.sh
```

Init test HBase table using `init-hbase-test-table.script`(assuming HBase is in home directory):
```

$ git clone https://github.com/mapr-demos/hbase-to-maprdb.git

$ ~/hbase-1.1.8/bin/hbase shell hbase-to-maprdb/bin/init-hbase-test-table.script

```

Ensure that `testtable` initialized using HBase shell:
```
$ ~/hbase-1.1.8/bin/hbase shell

hbase(main):004:0> scan 'testtable'
ROW                             COLUMN+CELL                                                                               
 user-0                         column=cf:email, timestamp=1514370176187, value=email-0                                   
 user-0                         column=cf:name, timestamp=1514370176198, value=name-0                                     
 user-0                         column=cf:password, timestamp=1514370176211, value=password-0                             
 user-0                         column=cf:user, timestamp=1514370176165, value=user-0  
 
 ...                            ...

 user-9                         column=cf:name, timestamp=1514370176502, value=name-9                                     
 user-9                         column=cf:password, timestamp=1514370176505, value=password-9                             
 user-9                         column=cf:user, timestamp=1514370176496, value=user-9                                     
21 row(s) in 0.0810 seconds

```

### Copying the data

You can use the CopyTable tool to migrate data from an Apache HBase table to a MapR-DB binary table. The Apache 
CopyTable tool launches a MapReduce application. The nodes on your cluster must have the correct version of the 
`mapr-hbase` package installed. To ensure that your existing HBase applications and workflow work properly, install 
the `mapr-hbase` package that provides the same version number of HBase as your existing Apache HBase.

1. Create the destination table. This example uses the HBase shell. The maprcli and MapR Control System (MCS) are also viable methods.


```
$ hbase shell
HBase Shell; enter 'help<RETURN>' for list of supported commands.
Type "exit<RETURN>" to leave the HBase Shell

hbase(main):001:0> create '/testtable', 'cf'
0 row(s) in 0.2040 seconds

```

Note: here we use MapR HBase client `hbase` and not the one, installed in [Example](#example) section.

2. Exit the HBase shell:

```
hbase(main):002:0> exit

```

3. From the HBase command line, use the CopyTable tool to migrate data.

On the node in the MapR cluster where you will launch the CopyTable tool, modify the value of the 
`hbase.zookeeper.quorum` property in the `hbase-site.xml` file to point at a ZooKeeper node in the source cluster. 
Alternately, you can specify the value for the `hbase.zookeeper.quorum` property from the command line. 
This example specifies the value in the command line.

```
$ hbase org.apache.hadoop.hbase.mapreduce.CopyTable -Dhbase.zookeeper.quorum=localhost -Dhbase.zookeeper.property.clientPort=2181 --new.name=/testtable testtable

```

### Verifying Migration

After copying data to the new tables, verify that the migration is complete and successful.  
Check the copied data with `scan` command:

```
hbase(main):008:0> scan '/testtable'
ROW                             COLUMN+CELL                                                                               
 user-0                         column=cf:email, timestamp=1514370176187, value=email-0                                   
 user-0                         column=cf:name, timestamp=1514370176198, value=name-0                                     
 user-0                         column=cf:password, timestamp=1514370176211, value=password-0                             
 user-0                         column=cf:user, timestamp=1514370176165, value=user-0  
 
 ...                            ...

 user-9                         column=cf:name, timestamp=1514370176502, value=name-9                                     
 user-9                         column=cf:password, timestamp=1514370176505, value=password-9                             
 user-9                         column=cf:user, timestamp=1514370176496, value=user-9                                     
21 row(s) in 0.0810 seconds
```


For more information about migration of the data from HBase to MapR-DB Binary, refer 
[Migrating between Apache HBase and MapR-DB Binary Tables](https://maprdocs.mapr.com/60/MapR-DB/MigratingBetweenApacheHBaseandMapRDB.html).

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

