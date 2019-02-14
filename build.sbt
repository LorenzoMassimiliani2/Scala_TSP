name := "Scala TSP"

version := "0.1"

scalaVersion := "2.11.12"

libraryDependencies ++= Seq (
  "org.apache.spark" %% "spark-core" % "2.2.0",
  "org.apache.spark"  %% "spark-mllib" % "2.2.0"
)

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}