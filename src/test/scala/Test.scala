package test

import hr.element.doit.s3._
import hr.element.doit.s3.serio.Instances._

object S3Conf {
  val TestBucket = "hr.element.test.prvi"
  val Username   = "AKIAJGBNYBK3WUXUQ74Q"
  val Password   = "2pAmJh+2YZ2MLZVkg9vfmCu/dfo8KPhGrJrCpRFN"
  val Secret     = "Amazon Element D" // Has to be 16 bytes
}

object Test {
  import S3Conf._

  implicit val s3b = S3BucketDescription(
      TestBucket,
      (Username, Password),
      Some(Secret.getBytes("UTF-8")))


  def main(args: Array[String]) {
//    val p = S3.store("kljuc", "podaci velike moci")
    val p = S3.load[String]("kljuc")
    println(p.apply())
  }
}