package com.github.tminglei

/**
  * Created by minglei on 10/23/16.
  */
package object slickpg {
  @deprecated("Use type `ExPostgresProfile` instead of `ExPostgresDriver`", "0.15.0")
  type ExPostgresDriver = ExPostgresProfile
  @deprecated("Use type `ExPostgresProfile` instead of `ExPostgresDriver`", "0.15.0")
  val ExPostgresDriver = ExPostgresProfile
}
