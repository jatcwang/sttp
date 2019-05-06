package com.softwaremill.sttp

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files

import org.scalatest.{FlatSpec, Matchers}

class ToCurlConverterTest extends FlatSpec with Matchers {

  private val localhost = uri"http://localhost"

  it should "convert base request" in {
    sttp
      .get(localhost)
      .toCurl shouldBe """curl -L --max-redirs=32 -X GET -H "Accept-Encoding: gzip, deflate" http://localhost"""
  }

  it should "convert request with method to curl" in {
    sttp.get(localhost).toCurl should include("-X GET")
    sttp.post(localhost).toCurl should include("-X POST")
    sttp.put(localhost).toCurl should include("-X PUT")
    sttp.delete(localhost).toCurl should include("-X DELETE")
    sttp.patch(localhost).toCurl should include("-X PATCH")
    sttp.head(localhost).toCurl should include("-X HEAD")
    sttp.options(localhost).toCurl should include("-X OPTIONS")
  }

  it should "convert request with header" in {
    sttp.header("User-Agent", "myapp").get(localhost).toCurl should include(
      """-H "User-Agent: myapp""""
    )
  }

  it should "convert request with body" in {
    sttp.body(Map("name" -> "john", "org" -> "sml")).post(localhost).toCurl should include(
      """-H "Content-Type: application/x-www-form-urlencoded" -H "Content-Length: 17" -F 'name=john&org=sml'"""
    )
    sttp.body("name=john").post(localhost).toCurl should include(
      """-H "Content-Type: text/plain; charset=utf-8" -H "Content-Length: 9" --data 'name=john'"""
    )
    sttp.body("name=john", StandardCharsets.ISO_8859_1.name()).post(localhost).toCurl should include(
      """ -H "Content-Type: text/plain; charset=ISO-8859-1" -H "Content-Length: 9" --data 'name=john'"""
    )
  }

  it should "convert request with options" in {
    sttp.followRedirects(false).get(localhost).toCurl should not include "-L"
    sttp.maxRedirects(11).get(localhost).toCurl should include("--max-redirs=11")
  }

  it should "put placeholder when sending binary data" in {
    val testBodyBytes = "this is the body".getBytes("UTF-8")

    val curl = sttp
      .post(localhost)
      .body(new ByteArrayInputStream(testBodyBytes))
      .toCurl
    curl should include("--data-binary <PLACEHOLDER>")
  }

  it should "use filename when sending file" in {
    val file = Files.createTempFile("sttp", "sttp").toFile
    file.deleteOnExit()

    val curl = sttp
      .post(localhost)
      .body(file)
      .toCurl
    curl should include(s"--data-binary @${file.getName}")
  }
}
