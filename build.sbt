name := "slicksample"

version := "0.1"

scalaVersion := "2.13.3"

val slickVersion ="3.3.3"

val testcontainersScalaVersion = "0.38.4"

resolvers += "Artima Maven Repository" at "https://repo.artima.com/releases"

libraryDependencies ++= Seq(
  "com.typesafe.slick" %% "slick" % slickVersion,
  "org.slf4j" % "slf4j-api" % "1.6.4",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.slick" %% "slick-hikaricp" % slickVersion,
  "mysql" % "mysql-connector-java" % "8.0.15",
  // Scala test
  "org.scalactic" %% "scalactic" % "3.2.0",
  "org.scalatest" %% "scalatest" % "3.2.0" % "test",
  // Test containers
  "org.testcontainers" % "jdbc" % "1.15.0-rc2",
  "org.testcontainers" % "mysql" % "1.15.0-rc2",
  "org.testcontainers" % "testcontainers" % "1.15.0-rc2",
  "org.testcontainers" % "database-commons" % "1.15.0-rc2",
  "com.dimafeng" %% "testcontainers-scala-scalatest" % testcontainersScalaVersion % "test",
  "com.dimafeng" %% "testcontainers-scala-mysql" % testcontainersScalaVersion % "test"
)

Test / fork := true

