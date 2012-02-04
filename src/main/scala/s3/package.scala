package hr.element.doit

import java.util.Properties
import scala.collection.JavaConverters._

private [doit] object Props {

  def mk (f: Properties => Any) = {
    val props = new Properties
    f (props) ; props.asScala
  }

  def fromResource (res: String) =
    mk (_ load (getClass.getResourceAsStream (res)))

}

package object s3 extends serio.EncDecInstances {

  object buckets {

    private val propsfile = "/aws-s3-access.properties"

    private val creds = {

      val props = try Props fromResource propsfile catch {
          case _ => sys error ("Cannot load `%s'" format propsfile)
        }

      try (( props ("id"), props ("key") )) catch {
        case _ => sys error ("`%s' must contain `id' and `key' props.")
      }
    }

    import s3.S3BucketDescription
    /** Default bucket access descriptor. */
    implicit def storeator: S3BucketDescription = S3BucketDescription ( "xxx.desu.666", creds , None )

    // Variate default creds.
    def defaultAccess ( bucketName : String, secret : Option[Array[Byte]] ) =
      S3BucketDescription ( bucketName, creds, secret )
  }
}

