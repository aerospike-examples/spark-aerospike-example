package com.aerospike.spark

import org.apache.spark.SparkConf
import org.apache.spark.sql.types.{IntegerType, StringType, StructField, StructType}
import org.apache.spark.sql.{Row, SaveMode, SparkSession}

/**
 * This example will load some data into a "Business" namespace in an aerospike database.
 * It will then use aeroJoin to take a sequence of ids and load the appropriate customer data, filter it, and print out the result.
 *
 * Prereqs: A working Aerospike Connect for Spark and an Aerospike server running on default port on localhost with at least 1 namespace named "Business"
 */
object aeroJoinExample extends Serializable {
  val conf: SparkConf = new SparkConf()
    .setAppName("AeroJoin")
    .set("aerospike.seedhost", "localhost")
    .set("aerospike.port", "3000")
    .set("aerospike.namespace", "test")

  val session: SparkSession = SparkSession.builder()
    .config(conf)
    .master("local[*]")
    .appName("Aerospike Example(II)")
    .config("spark.ui.enabled", "false")
    .getOrCreate()

  def main(args: Array[String]) {
    import session.implicits._

    loadCustomerData()

    val ids = Seq("customer1", "customer2",
      "customer3", "customer4", "IDontExist")

    val customerIdsDF = ids.toDF("customer_id").as[CustomerID]

    val customerDS = customerIdsDF.aeroJoin[CustomerKV]("customer_id", "Customers")
    customerDS.foreach(b => println(b))

    val bestCustomers = customerDS.filter(customer => customer.stars > 4)
    bestCustomers.foreach(b => println(b))

    bestCustomers.map(c => new Customer(c.key, c.customer_id, c.first, c.last, c.stars)).toDF("key", "customer_id", "last", "first", "stars").
      write.mode(SaveMode.Overwrite).
      format("com.aerospike.spark.sql").
      option("aerospike.updateByKey", "customer_id").
      option("aerospike.set", "BestCustomers").
      save()
    session.stop()
  }

  def loadCustomerData(): Unit = {

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
      format("com.aerospike.spark.sql").
      option("aerospike.updateByKey", "customer_id").
      option("aerospike.set", "Customers").
      save()
  }
}

case class CustomerID(customer_id: String)

case class Customer(key: String, customer_id: String, first: String, last: String, stars: Long)

case class CustomerKV(__key: Any, key: String, customer_id: String, first: String, last: String, stars: Long) extends AeroKV
