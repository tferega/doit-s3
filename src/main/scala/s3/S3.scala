package hr.element.doit.s3

import scala.collection.{ mutable => mut }

import scalaz._ ; import Scalaz._

import scala.actors._
import scala.concurrent.ops._
import scala.concurrent.JavaConversions

import java.util.concurrent.Executors

import com.amazonaws.services.{ s3 => as3 }
import as3.{ AmazonS3, AmazonS3Client, AmazonS3EncryptionClient }
import as3.model.{ EncryptionMaterials, ObjectMetadata, AmazonS3Exception }
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.importexport.model.NoSuchBucketException

import javax.crypto.spec.SecretKeySpec
import java.io.ByteArrayInputStream

import org.apache.commons.io.IOUtils.toByteArray

import serio.Encode ; import serio.Decode

/**
 * Some goodies and a standalone S3 entry point.
 */
object S3 {

  lazy val instance = new S3 start

  def store [A] (key: String, dataz: A)
    ( implicit bucket  : S3BucketDescription
    ,          ev      : Encode[A]
    ): Future[Any]
    = (instance !! Store (bucket, key, dataz)) 

  def load [A] (key: String)
    ( implicit bucket : S3BucketDescription
    ,          ev     : Decode[A]
    ): Future[Any]
    = (instance !! Load (bucket, key))

}

/**
 * A single request for a single store operation.
 * @param bucket access info
 * @param key key name / file name
 * @param dataz meat
 */
class Store (
    val bucket : S3BucketDescription
  , val key    : String
  , val dataz  : Array[Byte]
)

object Store {
  def apply [A: Encode] (bucket: S3BucketDescription, key: String, dataz: A)
    = new Store (bucket, key, Encode.encode [A] (dataz))
  def unapply (s: Store) = Some ((s.bucket, s.key, s.dataz))
}

/**
 * A single request for a single load operation.
 * @param bucket access info
 * @param key key name / file name
 */
case class Load (
    val bucket : S3BucketDescription
  , val key    : String
)

/**
 * Bucket and its associated security context.
 * @param bucketName Name of the bucket.
 * @param creds Amazon ID + Access key.
 * @param secret [It might be an] encryption key.
 */
case class S3BucketDescription (
    bucketName : String
  , creds      : (String, String)
  , secret     : Option[Array[Byte]] = None
)

class S3 extends DaemonActor {

  def act = loop { react {

    case Store (S3BucketDescription (bucket, creds, secret), key, what) =>

      val client = clients ((creds, secret))

      spawn_ {

        val meta = new ObjectMetadata
        meta setContentLength what.length

        def putf = client putObject (
          bucket, key, new ByteArrayInputStream (what), meta
        )

        try putf catch {
          case e: AmazonS3Exception if e.getErrorCode == "NoSuchBucket" =>
            client createBucket bucket ;
            putf
        }
      }

    case Load (S3BucketDescription (bucket, creds, secret), key) =>

      val client = clients ((creds, secret))
      spawn_ { toByteArray ( client getObject (bucket, key) getObjectContent ) }

  } }

  // Hence, `spawn`s are queued to an unbounded, self-shrinking thread pool.
  implicit val runner = JavaConversions asTaskRunner Executors.newCachedThreadPool

  def spawn_ (wat: => Any) = {
    val sender0 = sender
    spawn { 
      sender0 ! ( try wat catch { case e: Exception => e } )
    }
  }

  type ClientKey = ((String, String), Option[Array[Byte]])

  // One s3 client per access config, because they parallelize well.
  private val clients: mut.Map[ClientKey, AmazonS3] = mut.Map () withDefault {

    case k@((accessKey, secretKey), crypto) =>

      val creds  = new BasicAWSCredentials (accessKey, secretKey)

      val client = crypto fold (
        key => new AmazonS3EncryptionClient (creds,
          new EncryptionMaterials (new SecretKeySpec (key, "AES"))) ,
        new AmazonS3Client (creds)
      )

      clients += (k -> client)
      client
  }

}


// // Migrate me to common utils.
// private [s3] trait Logs {

//   private def l (f: (AnyRef, => String) => Unit) : ((String, AnyRef*) => Unit)
//     = (msg, params) => f (this, msg.format (params: _*))

//   import akka.event.{EventHandler => eh}
//   protected object log {
//     val info  = l (eh.info)
//     val error = l (eh.error)
//   }
// }
