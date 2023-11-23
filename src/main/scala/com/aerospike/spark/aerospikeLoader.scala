package com.aerospike.spark
import org.apache.spark.SparkConf
import org.apache.spark.internal.Logging
import org.apache.spark.sql.catalyst.expressions.MonotonicallyIncreasingID
import org.apache.spark.sql.functions.{monotonicallyIncreasingId, monotonically_increasing_id}
import org.apache.spark.sql.types.{IntegerType, StringType, StructField, StructType}
import org.apache.spark.sql.{Row, SaveMode, SparkSession}
import com.aerospike.client.AerospikeClient

import scala.Console.println
import scala.annotation.tailrec
import scala.sys.exit



/**
 * This example will load some data into specified namespace(by default test) in an specified aerospike database (by default running localhost:3000).
 * It will then use aeroJoin to take a sequence of ids and load the appropriate customer data, filter it, and print out the result.
 *
 * Prereqs: A working Aerospike Connect for Spark and an Aerospike server running on default port on localhost with at least 1 namespace named "Business"
 */
object aerospikeLoader extends Logging{

  def main(args: Array[String]): Unit = {
    @tailrec
    def nextArg(map: Map[String, String], list: List[String]): Map[String, String] = {
      list match {
        case Nil => map
        case "--seedhost" :: value :: tail =>
          nextArg(map ++ Map("seedhost" -> value), tail)
        case "--namespace" :: value :: tail =>
          nextArg(map ++ Map("namespace" -> value), tail)
        case "--path" :: value :: tail =>
          nextArg(map ++ Map("path" -> value), tail)
        case "--key-column" :: value :: tail =>
          nextArg(map ++ Map("keyColumn" -> value), tail)
        case unknown :: _ =>
          println("Unknown option " + unknown)
          exit(1)
      }
    }
    val options = nextArg(Map(), args.toList)
    logInfo(options.toString)
    val conf: SparkConf = new SparkConf()
      .setAppName("AerospikeLoader")
      .set("aerospike.seedhost", options.getOrElse("seedhost", "localhost:3000"))
      .set("aerospike.namespace", options.getOrElse("namespace", "test"))

    val session: SparkSession = SparkSession.builder()
      .config(conf)
//      .master("local[*]") //change it, if your spark cluster is not local.
      .config("spark.ui.enabled", "true")
      .config("spark.driver.maxResultSize", 0)
      .getOrCreate()
    loadJsonData(session, path = options.getOrElse("path", throw new Exception("Missing path argument")),
      keyColumn = options.get("keyColumn"), set = options.get("set") )
    session.stop()
  }

  private def loadJsonData(session: SparkSession, path:String, keyColumn: Option[String] = Some("__key"),
                           set:Option[String] = Some("__default")): Unit = {
    val resolvedKeyColumn = keyColumn.getOrElse("__key")
    val resolvedSet = set.getOrElse("__default")


    val jsonDF = session.read.json(path)
    jsonDF.printSchema()

    val jsonIdDF = if (jsonDF.columns.contains(resolvedKeyColumn)) {
      jsonDF
    } else {
      jsonDF.withColumn(resolvedKeyColumn,monotonically_increasing_id())
    }

    logWarning(s"Schema is")
    logWarning(jsonIdDF.schema.json)

    jsonIdDF.write.
      mode(SaveMode.Overwrite).
      format("aerospike").
      option("aerospike.updateByKey", resolvedKeyColumn).
      option("aerospike.set", resolvedSet).
      option("aerospike.sendKey", value = true).
      save()
  }
}