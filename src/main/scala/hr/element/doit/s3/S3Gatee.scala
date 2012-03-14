package hr.element.doit
package s3

import scala.collection.{ mutable => mut }

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.model.EncryptionMaterials
import com.amazonaws.services.s3.{ AmazonS3, AmazonS3Client, AmazonS3EncryptionClient }

import javax.crypto.spec.SecretKeySpec

import scalaz.Scalaz._


object S3Gate {
  type ClientKey = (S3Credentials, Option[Array[Byte]])

  // One S3 client per access config, because they parallelize well (until up to 1000).
  private[s3] val clients = cache[ClientKey, AmazonS3] {
    case k @ (S3Credentials(accessKey, secretKey), crypto) =>
      val creds = new BasicAWSCredentials(accessKey, secretKey)
      crypto fold (
        key => new AmazonS3EncryptionClient(creds, new EncryptionMaterials(new SecretKeySpec(key, "AES"))),
        new AmazonS3Client(creds)
      )
  }

  def cache[A, B](create: A => B): A => B = {
    lazy val map : mut.Map [A, B] =
      mut.Map () withDefault { key =>
        synchronized {
          map get key getOrElse {
            val x = create (key)
            map += (key -> x)
            x
          }
        }
      }
      map
  }
}
