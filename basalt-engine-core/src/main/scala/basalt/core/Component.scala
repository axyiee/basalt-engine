package basalt.core

import cats.kernel.Monoid
import cats.Id

trait Component[I, D]:
  def identity: Id[I]
  def data: D

trait ComponentTag[C <: Component[_, _]]:
  
