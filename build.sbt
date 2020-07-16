import scala.util.Properties

name := "spark-aerojoin-example"

version := "1.0"

organization := "com.aerospike"
mainClass in (Compile, run) := Some("com.aerospike.spark.aeroJoinExample")
mainClass in assembly := Some("com.aerospike.spark.aeroJoinExample")

crossScalaVersions := Seq("2.10.6", "2.11.8", "2.12.0")

scalaVersion := "2.11.8"

val sparkHome = Properties.envOrElse("SPARK_HOME", "/opt/spark")

javacOptions ++= Seq("-source", "1.7", "-target", "1.7")

parallelExecution in test := false

fork in test := true

libraryDependencies ++= Seq(
	"org.apache.spark"            %% "spark-core"             % "2.2.0",
	"org.apache.spark"            %% "spark-sql"              % "2.2.0",
	"org.apache.spark"            %% "spark-mllib"            % "2.2.0",
	"org.apache.spark"            %% "spark-streaming"        % "2.2.0",
	"com.ning"                    % "async-http-client"       % "1.9.10",
	
	"com.databricks"              %% "spark-csv"              % "1.5.0",
	"com.aerospike"               %% "aerospike-spark"        % "provided" from s"file://${sparkHome}/jars/aerospike-spark-assembly-1.0.0.jar",
	"com.aerospike"               %  "aerospike-helper-java"  % "1.2.2",
	"com.aerospike"               %  "aerospike-client"       % "4.0.8",
	"com.typesafe.scala-logging"  %% "scala-logging-slf4j"    % "2.1.2",
	"org.scalatest"               %% "scalatest"              % "2.2.1" % Test,
	"joda-time"                   % "joda-time"               % "2.9.9" % Test
)

resolvers ++= Seq("Local Maven" at Path.userHome.asFile.toURI.toURL + ".m2/repository")
resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"
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
