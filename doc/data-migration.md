# Migration of the data from HBase to MapR-DB Binary

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