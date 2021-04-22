package com.aerospike.spark

import org.apache.spark.SparkConf
import org.apache.spark.sql.types.{IntegerType, StringType, StructField, StructType}
import org.apache.spark.sql.{Row, SaveMode, SparkSession}
import scala.collection.mutable
/**
 * This example will load some data into specified namespace(by default test) in an specified aerospike database (by default running localhost:3000).
 * It will then use aeroJoin to take a sequence of ids and load the appropriate customer data, filter it, and print out the result.
 *
 * Prereqs: A working Aerospike Connect for Spark and an Aerospike server running on default port on localhost with at least 1 namespace named "Business"
 */
object aeroJoinExample {

  def main(args: Array[String]) {

    val commandLineParams = mutable.Map[String, String]()
    for (item <- args.grouped(2))
      commandLineParams(item(0).trim()) = item(1).trim
    val allParams=commandLineParams.toMap

    val conf: SparkConf = new SparkConf()
      .setAppName("AeroJoin")
      .set("aerospike.seedhost", allParams.getOrElse("aerospike.seedhost","localhost:3000") )
      .set("aerospike.namespace", allParams.getOrElse("aerospike.namespace","test"))

    val session: SparkSession = SparkSession.builder()
      .config(conf)
      .master("local[*]")  //change it, if your spark cluster is not local.
      .config("spark.ui.enabled", "false")
      .getOrCreate()

    session.sparkContext.setLogLevel("error")
    import session.implicits._
    loadCustomerData(session)

    val ids = Seq("customer1", "customer2", "customer3", "customer4", "IDontExist")
    //convert Dataframe to Dataset. This step is needed since aeroJoin is defined over Dataset.
    val customerIdsDF = ids.toDF("customer_id").as[CustomerID]

    val customerDS = customerIdsDF.aeroJoin[CustomerKV]("customer_id", "Customers")
    customerDS.foreach(b => println(b))
    val bestCustomers = customerDS.filter(customer => customer.stars > 4)
    bestCustomers.foreach(b => println(b))
    bestCustomers.map(c => Customer(c.key, c.customer_id, c.first, c.last, c.stars)).toDF("key", "customer_id", "last", "first", "stars").
      write.
      mode(SaveMode.Overwrite).
      format("aerospike").
      option("aerospike.updateByKey", "customer_id").
      option("aerospike.set", "BestCustomers").
      save()
    session.stop()
  }

  /**
   * Save some sample data in Customers set for experimentation.
   */
  def loadCustomerData(session: SparkSession): Unit = {

    import session.implicits._
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
      option("aerospike.set", "Customers").//insert into this set
      save()
  }
}

case class CustomerID(customer_id: String)

case class Customer(key: String, customer_id: String, first: String, last: String, stars: Long)

case class CustomerKV(__key: Any, key: String, customer_id: String, first: String, last: String, stars: Long) extends AeroKV
