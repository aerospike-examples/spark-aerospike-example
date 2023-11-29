import sbtassembly.MergeStrategy._
import scala.util.Properties

name := "spark-aerojoin-example"

version := "1.0"

organization := "com.aerospike"
mainClass in (Compile, run) := Some("com.aerospike.spark.aeroJoinExample")
mainClass in assembly := Some("com.aerospike.spark.aeroJoinExample")

scalaVersion := "2.12.11"

val aerospikeJarPath = Properties.envOrElse("AerospikeSparkJarPath", "/home/joem/src/aerospike-spark/target/scala-2.12/aerospike-spark-4.3.1-spark3.0-scala2.12-allshaded.jar")
javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint")
scalaVersion := "2.12.11"
crossScalaVersions := Seq(scalaVersion.value)
val sparkVer = "3.0.0"
val hadoopVer = "3.3.0"
val connectorVersion = "4.3.1-spark3.2-scala2.12-allshaded"

libraryDependencies ++= Seq(
	"org.apache.spark"            %% "spark-core"   % sparkVer % Provided,
	"org.apache.spark"            %% "spark-sql"   % sparkVer % Provided,
  "org.apache.hadoop" % "hadoop-common" % hadoopVer % Provided,
  "org.apache.hadoop" % "hadoop-mapreduce-client-core" % hadoopVer % Provided,

//	"com.aerospike"               %% "aerospike-spark"        % "provided" from s"file://${aerospikeJarPath}",
  "com.aerospike" %% "aerospike-spark" % connectorVersion,
)

resolvers ++= Seq("Local Maven" at Path.userHome.asFile.toURI.toURL + ".m2/repository")
resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"
resolvers += "Artifactory Realm" at "https://aerospike.jfrog.io/artifactory/spark-connector"
publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))

cancelable in Global := true

assemblyMergeStrategy in assembly := {
  case x if Assembly.isConfigFile(x) =>
    MergeStrategy.concat
  case PathList(ps @ _*) if Assembly.isReadme(ps.last) || Assembly.isLicenseFile(ps.last) =>
    MergeStrategy.rename
  case PathList("META-INF", "maven","com.aerospike","aerospike-client", "pom.properties") =>
    MergeStrategy.discard
  case PathList("META-INF", "maven","com.aerospike","aerospike-client", "pom.xml") =>
    MergeStrategy.discard
  case PathList("META-INF", "maven","org.slf4j","slf4j-api", "pom.xml") =>
    MergeStrategy.discard
  case PathList("META-INF", "maven","com.fasterxml.jackson.core","jackson-annotations", "pom.xml") =>
    MergeStrategy.discard
  case PathList("META-INF", "maven","com.fasterxml.jackson.core","jackson-core", "pom.xml") =>
    MergeStrategy.discard
  case PathList("META-INF", "maven","com.fasterxml.jackson.core","jackson-databind", "pom.xml") =>
    MergeStrategy.discard
  case PathList("META-INF", "maven","commons-logging","commons-logging", "pom.xml") =>
    MergeStrategy.discard
  case PathList("META-INF", "maven","com.google.guava","guava", "pom.xml") =>
    MergeStrategy.discard
  case PathList("META-INF", "maven","jline","jline", "pom.xml") =>
    MergeStrategy.discard
  case PathList(ps @ _*) if ps.last endsWith "pom.properties" =>
    MergeStrategy.discard  
  case PathList("META-INF", xs @ _*) =>
    xs.map(_.toLowerCase) match {
      case ("manifest.mf" :: Nil) | ("index.list" :: Nil) | ("dependencies" :: Nil) =>
        MergeStrategy.discard
      case ps @ (x :: _) if ps.last.endsWith(".sf") || ps.last.endsWith(".dsa") =>
        MergeStrategy.discard
      case "plexus" :: _ =>
        MergeStrategy.discard
      case "services" :: _ =>
        MergeStrategy.filterDistinctLines
      case ("spring.schemas" :: Nil) | ("spring.handlers" :: Nil) =>
        MergeStrategy.filterDistinctLines
      case _ => MergeStrategy.deduplicate
    }
  case _ => MergeStrategy.first
}
