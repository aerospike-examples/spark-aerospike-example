import scala.util.Properties

name := "joexample"

version := "0.1"

scalaVersion := "2.12.11"
def sysPropOrDefault(propName:String,default:String):String = Option(System.getProperty(propName)).getOrElse(default)

val sparkVer = sysPropOrDefault("version","2.4.4")
val hadoopVer = "2.7.3"
val asClientVer = "4.4.13"
val asSparkConnectorVer = "2.3.0"
val sparkHome = Properties.envOrElse("SPARK_HOME", "/opt/spark")

//libraryDependencies += "org.apache.spark" %% "spark-sql" % "2.4.4" % "provided"
//libraryDependencies += "org.apache.spark" %% "spark-mllib" % "2.4.4" % "provided"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % sparkVer % "provided",
  "org.apache.spark" %% "spark-sql" % sparkVer ,
  "org.apache.hadoop" % "hadoop-common" % hadoopVer ,
  "org.apache.hadoop" % "hadoop-mapreduce-client-core" % hadoopVer % Provided,
  "com.aerospike"  %% "aerospike-spark" % "provided" from s"file://${sparkHome}/jars/aerospike-spark-assembly-${asSparkConnectorVer}.jar",
  "com.aerospike"  %  "aerospike-client"   % asClientVer,

)
//
//resolvers ++= Seq("Local Maven" at Path.userHome.asFile.toURI.toURL + ".m2/repository")
//resolvers += "Local Maven Repository" at "file://" + Path.userHome.absolutePath + "/.m2/repository"
//resolvers += Resolver.url("bintray-sbt-plugins", url("http://dl.bintray.com/sbt/sbt-plugin-releases"))(Resolver.ivyStylePatterns)
////resolvers += "Party" at "file://" + Path.userHome.absolutePath + "/jars"
//resolvers += Resolver.url("my-test-repo", url("file://" + Path.userHome.absolutePath + "/jars"))( Patterns("/[artifact].[ext]") )
resolvers ++= Seq("Local Maven" at Path.userHome.asFile.toURI.toURL + ".m2/repository")
resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"
publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))
