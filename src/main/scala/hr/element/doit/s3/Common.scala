package hr.element.doit.s3


case class S3Credentials(accessKey: String, secretKey: String)

case class S3Bucket(
    name:        String,
    credentials: S3Credentials,
    secret:      Option[Array[Byte]] = None)


case class S3Result[T](totalTime: Long, actionTime: Option[Long], result: Either[Exception, T])
