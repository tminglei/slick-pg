package com.github.tminglei

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

package object slickpg {
  implicit val testExecContext = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(4))
}
