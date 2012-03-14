package hr.element.doit
package s3
package statistics


object Timer {
  def t = System.currentTimeMillis()

  def apply[T](f: => T): Timer[T] = {
    val start = t
    val result = f
    val end = t

    Timer((end - start), result)
  }
}


case class Timer[T](time: Long, result: T)