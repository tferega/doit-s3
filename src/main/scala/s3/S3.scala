package hr.element.doit.s3

import scala.collection.{ mutable => mut }

import scalaz._ ; import Scalaz._
import scalaz.concurrent._

import scala.concurrent.Lock

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

object S3 {

  implicit def unboundedStrat = Strategy.Executor (Executors.newCachedThreadPool)

  def store [A] (key: String, dataz: A)
    ( implicit s3b : S3BucketDescription
    ,          ev  : Encode[A]
    ) : Promise[Unit] = promise {

    val wat = Encode.encode[A] (dataz)

    val S3BucketDescription (bucket, creds, secret) = s3b
    val client = S3Gate client ((creds, secret))

    val meta = new ObjectMetadata
    meta setContentLength wat.length

    def putf = client putObject (
      bucket, key, new ByteArrayInputStream (wat), meta
    )

    try putf catch {
      case e: AmazonS3Exception if e.getErrorCode == "NoSuchBucket" =>
        client createBucket bucket ;
        putf
    }

    Unit
  }

  def load [A] (key: String)
    ( implicit s3b : S3BucketDescription
    ,          ev  : Decode[A]
    ) : Promise[A] = promise {

    val S3BucketDescription (bucket, creds, secret) = s3b

    val client = S3Gate client ((creds, secret))
    val resp   = client getObject (bucket, key) ;

    Decode.decode[A] ( toByteArray (resp.getObjectContent) )
  }

}

object S3Gate {

  type ClientKey = ((String, String), Option[Array[Byte]])
  
  def client (key: ClientKey) = {
    mutex.acquire
    try clients (key)
    finally mutex.release
  }

  private val mutex = new Lock

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

