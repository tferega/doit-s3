package hr.element.doit
package s3

import serio.{ Decode, Encode }
import statistics.Timer

import com.amazonaws.{ AmazonClientException, AmazonServiceException }
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.importexport.model.NoSuchBucketException
import com.amazonaws.services.s3.{ AmazonS3, AmazonS3Client, AmazonS3EncryptionClient }
import com.amazonaws.services.s3.model.{ EncryptionMaterials, ObjectMetadata, AmazonS3Exception }

import java.io.ByteArrayInputStream
import java.util.concurrent.Executors

import javax.crypto.spec.SecretKeySpec

import org.apache.commons.io.IOUtils.toByteArray

import scala.collection.{ mutable => mut }
import scala.concurrent.Lock

import scalaz.concurrent._
import scalaz.Scalaz._


case class S3Credentials(accessKey: String, secretKey: String)
case class S3Bucket(
    name:        String,
    credentials: S3Credentials,
    secret:      Option[Array[Byte]] = None)



case class S3Result[T](totalTime: Long, actionTime: Option[Long], result: Either[Exception, T])
object S3 {
  def ErrorHanlder[T]: PartialFunction[Throwable, (Option[Long], Either[Exception, T])] = {
    case e: AmazonServiceException => (None, Left(new Exception("An error occured in Amazon S3 while processing the request.", e)))
    case e: AmazonClientException  => (None, Left(new Exception("An error occured in the client while making the request or handling the response.", e)))
    case e: Exception              => (None, Left(new Exception("An unexpected error occured.", e)))
  }


  // Limits number of concurrent operations to 1000.
  implicit val strat = Strat.cachedTo (1000)

  def store[A]
      (key: String, data: A)
      (implicit bucket: S3Bucket, ev: Encode[A]):
      Promise[S3Result[Unit]] =
    promise {
      val timedResult = Timer {
        try {
          val encodedData = Encode.encode[A](data)
          val client = S3Gate.clients((bucket.credentials, bucket.secret))

          val meta = new ObjectMetadata
          meta setContentLength encodedData.length

          val dataStream = new ByteArrayInputStream(encodedData)
          val timedPut = Timer {
            client.putObject(bucket.name, key, dataStream, meta)
          }

          (Some(timedPut.time), Right())
        } catch
          ErrorHanlder
      }

      val Timer(totalTime, (actionTime, result)) = timedResult
      S3Result(totalTime, actionTime, result)
    }


  def load[A](key: String)
      (implicit bucket: S3Bucket, ev: Decode[A]):
      Promise[S3Result[A]] =
    promise {
      val timedResult = Timer {
        try {
          val client   = S3Gate.clients((bucket.credentials, bucket.secret))
          val response = client.getObject(bucket.name, key)
          val timedGet = Timer {
            Decode.decode[A](toByteArray(response.getObjectContent))
          }

          (Some(timedGet.time), Right(timedGet.result))
        } catch
          ErrorHanlder
      }

      val Timer(totalTime, (actionTime, result)) = timedResult
      S3Result(totalTime, actionTime, result)
    }
}



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



object Strat {
  import java.util.concurrent._

  def daemonicTF = new ThreadFactory {
    val deftf = Executors.defaultThreadFactory
    def newThread(r: Runnable) = {
      val t = deftf newThread r
      t setDaemon true
      t
    }
  }

  def cachedTo (n: Int) =
    Strategy.Executor(
      new ThreadPoolExecutor (
        0, n, 60L, TimeUnit.SECONDS,
        new SynchronousQueue[Runnable],
        daemonicTF
    )
  )
}
