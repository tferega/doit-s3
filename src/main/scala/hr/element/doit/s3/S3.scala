package hr.element.doit
package s3

import serio.{ Decode, Encode }
import statistics.Timer

import com.amazonaws.{ AmazonClientException, AmazonServiceException }
import com.amazonaws.services.s3.model.ObjectMetadata

import java.io.ByteArrayInputStream

import org.apache.commons.io.IOUtils.toByteArray

import scalaz.concurrent.Promise
import scalaz.Scalaz._


object S3 {
  private def ErrorHanlder[T]: PartialFunction[Throwable, (Option[Long], Either[Exception, T])] = {
    case e: AmazonServiceException => (None, Left(new Exception("An error occured in Amazon S3 while processing the request.", e)))
    case e: AmazonClientException  => (None, Left(new Exception("An error occured in the client while making the request or handling the response.", e)))
    case e: Exception              => (None, Left(new Exception("An unexpected error occured.", e)))
  }

  // Limits number of concurrent operations to 1000.
  implicit val strat = ThreadStrategy.cachedTo(1000)

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
