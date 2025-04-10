# spark-aerospike-example

***NOTE: This is only an example application that performs some basic operations. For a Tutorial on using the spark connector please see https://aerospike.com/docs/connectors/spark/tutorials/notebook/***

## Aerojoin spark application
An example app that makes use of aeroJoin. You must have a working spark installation (3.0.0) with the [Aerospike Connect for Spark installed](https://www.aerospike.com/enterprise/download/connectors/aerospike-spark/notes.html).

## Building and running

### Build using sbt
  - `cd` to the project root directory  `spark-aerospike-example`
  - run following bash commands appropriately
     ``` bash
      export AerospikeSparkJarPath="absolute-path-to-aerospike-spark-assembly-3.0.1.jar"
      sbt package
    ``` 
 
### Submit spark job

Then submit a job to your spark installation. If not provided `aerospike.seedhost` and  `aerospike.namespace` are assumed to be 
`localhost:3000` and `test` respectively. The script assumes that provided ports are TCP enabled.  

```bash
spark-submit --jars $AerospikeSparkJarPath --class com.aerospike.spark.aeroJoinExample  target/scala-2.12/spark-aerojoin-example_2.12-1.0.jar   aerospike.seedhost IP:PORT aerospike.namespace test 
```

## What does this example do?

This spark job will do a couple of things showing how aeroJoin can be used. It will take a list of ids and load the 
Aerospike records with those keys, filter those records, and write the filtered list back into Aerospike.

### Setting environment

First we must add some configuration:

```scala

 val conf: SparkConf = new SparkConf()
       .setAppName("AeroJoin")
       .set("aerospike.seedhost", allParams.getOrElse("aerospike.seedhost","localhost:3000") )
       .set("aerospike.namespace", allParams.getOrElse("aerospike.namespace","test"))
```

This is for an Aerospike server running on the default port on this machine as well as defining which namespace we are using (
if you have defined a different namespace in your setup you can modify this to point there instead.
).

Next we are using a spark session so we will define that:

```scala
  val session: SparkSession = SparkSession.builder()
    .config(conf)
    .master("local[*]")
    .appName("Aerospike Example(II)")
    .config("spark.ui.enabled", "false")
    .getOrCreate()
```
### Loading test data into Aerospike
As a first step we must load some test data into the database.

```scala
  def loadCustomerData(session: SparkSession): Unit = {

    val schema: StructType = new StructType(Array(
      StructField("key", StringType, nullable = true),
      StructField("customer_id", StringType, nullable = false),
      StructField("last", StringType, nullable = true),
      StructField("first", StringType, nullable = true),
      StructField("stars", IntegerType, nullable = true)
    ))

    val rows = Seq(
      Row("Fraser_Malcolm", "customer1", "Fraser", "Malcolm", 5),
      Row("Hawke_Bob", "customer2", "Hawke", "Bob", 4),
      Row("Keating_Paul", "customer3", "Keating", "Paul", 1),
      Row("Im_Nothere", "secretcustomer", "Nothere", "Im", 5),
      Row("Howard_John", "customer4", "Howard", "John", 1)
    )

    val customerRDD = session.sparkContext.parallelize(rows)
    val customerDF = session.createDataFrame(customerRDD, schema)

    customerDF.write.
      mode(SaveMode.Overwrite).
      format("aerospike").
      option("aerospike.updateByKey", "customer_id").
      option("aerospike.set", "Customers").
      save()
  }
}
```

Here we are creating 5 customers with a 5 star rating system and saving them to the Customers set of the configured 
namespace. The keys for these records will be the value in the `customer_id` column.

### Generate DataFrame of customer ids

```scala
 val ids = Seq("customer1", "customer2",
      "customer3", "customer4", "IDontExist")
 val customerIdsDF = ids.toDF("customer_id").as[CustomerID]
```
This creates a DataFrame holding just customer ids. This id list has 4 of the 5 customers we loaded into the DB and
an additional one not in the DB.

### Using aeroJoin

```scala
val customerDS = customerIdsDF.aeroJoin[CustomerKV]("customer_id", "Customers")
``` 

Here aeroJoin used to load records with keys matching the "customer_id" column of the DataFrame 
we just created.
The result will have 4 rows since there is no record in Aerospike with the key "IDontExist" and there is no customer_id in 
the DataFrame with the value "secretcustomer".

Next we filter the new DataSet to get the customers with 5 stars and save to a new set, `BestCustomers`, in our namespace.
There should be only 1 even though there are two in the raw data from the database (because the other one was not in the id list)

```scala
  val bestCustomers = customerDS.filter(customer => customer.stars > 4)

  bestCustomers.map(c => new Customer(c.key, c.customer_id, c.first, c.last, c.stars)).toDF("key", "customer_id", "last", "first", "stars").
      write.mode(SaveMode.Overwrite).
      format("aerospike").
      option("aerospike.updateByKey", "customer_id").
      option("aerospike.set", "BestCustomers").
      save()
```

### Verifying results

Once the spark job is completed you can check the Aerospike database to verify what it did in the `aql` tool.

```
aql> select * from Business.Customers
+------------------+-----------+------------------+-----------+-------+
| customer_id      | first     | key              | last      | stars |
+------------------+-----------+------------------+-----------+-------+
| "secretcustomer" | "Im"      | "Im_Nothere"     | "Nothere" | 5     |
| "customer4"      | "John"    | "Howard_John"    | "Howard"  | 1     |
| "customer2"      | "Bob"     | "Hawke_Bob"      | "Hawke"   | 4     |
| "customer3"      | "Paul"    | "Keating_Paul"   | "Keating" | 1     |
| "customer1"      | "Malcolm" | "Fraser_Malcolm" | "Fraser"  | 5     |
+------------------+-----------+------------------+-----------+-------+
5 rows in set (0.035 secs)

OK
```

This was the test data we loaded in the first step

```
aql> select * from Business.BestCustomers
+-------------+----------+------------------+-----------+-------+
| customer_id | first    | key              | last      | stars |
+-------------+----------+------------------+-----------+-------+
| "customer1" | "Fraser" | "Fraser_Malcolm" | "Malcolm" | 5     |
+-------------+----------+------------------+-----------+-------+
1 row in set (0.052 secs)

```

This is the filtered data of the one 5 star customer that was in the list of provided ids.
