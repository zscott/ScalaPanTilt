package io.synaptix

import java.util.concurrent.atomic.AtomicInteger

import scala.concurrent.{Promise, ExecutionContext, Future}
import scala.util.{Try, Failure, Success}

object FutureHelpers {

  def firstSuccessOrLastFailure[T](futures: TraversableOnce[Future[T]])(implicit executor: ExecutionContext): Future[T] = {
    val p = Promise[T]()
    val size = futures.size
    val failureCount = new AtomicInteger(0)

    futures foreach {
      _.onComplete {
        case Success(v) => p.trySuccess(v)
        case Failure(e) =>
          val count = failureCount.incrementAndGet
          if (count == size) p.tryFailure(e)
      }
    }
    p.future
  }

  def tryToFuture[T](t: Try[T])(implicit executor: ExecutionContext): Future[T] = Future {
    t match {
      case Success(v) => v
      case Failure(e) => throw e
    }
  }
}
