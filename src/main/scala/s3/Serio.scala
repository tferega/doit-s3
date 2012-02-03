package hr.element.doit.s3.serio

import java.nio.charset.Charset


trait Encode[-A] { def encode (a: A): Array[Byte] }
trait Decode[+A] { def decode (bs: Array[Byte]): A }

object Encode {
  def encode [A: Encode] (a: A) = implicitly [Encode[A]] encode a
}
object Decode {
  def decode [A: Decode] (bs: Array[Byte]) = implicitly [Decode[A]] decode bs
}

protected [serio] trait EncDecLow {

  def enc [A] (f: A => Array[Byte]) =
    new Encode [A] { def encode (a: A) = f (a) }
  def dec [A] (f: Array[Byte] => A) =
    new Decode [A] { def decode (bs: Array[Byte]) = f (bs) }

}

trait EncDecInstances extends EncDecLow {

  val UTF8 = Charset forName "UTF-8"

  implicit val encodeStr = enc [String] (_ getBytes UTF8)
  implicit val decodeStr = dec [String] (new String (_, UTF8))

  implicit val encodeAry = enc [Array[Byte]] (identity)
  implicit val decodeAry = dec [Array[Byte]] (identity)
}

object Instances extends EncDecInstances


