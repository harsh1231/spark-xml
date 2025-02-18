/*
 * Copyright 2014 Databricks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.databricks.spark.xml.parsers

import java.nio.file.Files
import java.sql.{Date, Timestamp}
import java.time.{ZoneId, ZonedDateTime}

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types._

import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite

case class KnownData(
    booleanDatum: Boolean,
    dateDatum: Date,
    decimalDatum: Decimal,
    doubleDatum: Double,
    integerDatum: Integer,
    longDatum: Long,
    stringDatum: String,
    timeDatum: String,
    timestampDatum: Timestamp,
    nullDatum: Null
)

final class StaxXmlGeneratorSuite extends AnyFunSuite with BeforeAndAfterAll {

  private lazy val spark: SparkSession = {
    // It is intentionally a val to allow import implicits.
    SparkSession.builder().
      master("local[2]").
      appName("XmlSuite").
      config("spark.ui.enabled", false).
      getOrCreate()
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    spark.sparkContext.setLogLevel("WARN")
  }

  override def afterAll(): Unit = {
    try {
      spark.stop()
    } finally {
      super.afterAll()
    }
  }

  test("write/read roundtrip") {
    import spark.implicits._

    val dataset = Seq(
      KnownData(
        booleanDatum = true,
        dateDatum = Date.valueOf("2016-12-18"),
        decimalDatum = Decimal(54.321, 10, 3),
        doubleDatum = 42.4242,
        integerDatum = 17,
        longDatum = 1520828868,
        stringDatum = "test,breakdelimiter",
        timeDatum = "12:34:56",
        timestampDatum = Timestamp.from(ZonedDateTime.of(2017, 12, 20, 21, 46, 54, 0,
          ZoneId.of("UTC")).toInstant),
        nullDatum = null),
      KnownData(booleanDatum = false,
        dateDatum = Date.valueOf("2016-12-19"),
        decimalDatum = Decimal(12.345, 10, 3),
        doubleDatum = 21.2121,
        integerDatum = 34,
        longDatum = 1520828123,
        stringDatum = "breakdelimiter,test",
        timeDatum = "23:45:16",
        timestampDatum = Timestamp.from(ZonedDateTime.of(2017, 12, 29, 17, 21, 49, 0,
          ZoneId.of("America/New_York")).toInstant),
        nullDatum = null)
    )
    
    val df = dataset.toDF().orderBy("booleanDatum")
    val targetFile =
      Files.createTempDirectory("StaxXmlGeneratorSuite").resolve("roundtrip.xml").toString
    df.write.format("xml").save(targetFile)
    val newDf =
      spark.read.schema(df.schema).format("xml").load(targetFile).orderBy("booleanDatum")
    assert(df.collect().toSeq === newDf.collect().toSeq)
  }

}
