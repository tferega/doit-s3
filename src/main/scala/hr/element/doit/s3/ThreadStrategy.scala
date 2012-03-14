package hr.element.doit
package s3

import scalaz.concurrent.Strategy


object ThreadStrategy {
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
