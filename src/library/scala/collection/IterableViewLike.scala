/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2003-2011, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

package scala.collection

import generic._
import TraversableView.NoBuilder

/** A template trait for non-strict views of iterable collections.
 *  $iterableViewInfo
 *
 *  @define iterableViewInfo
 *  $viewInfo
 *  All views for iterable collections are defined by re-interpreting the `iterator` method.
 * 
 *  @author Martin Odersky
 *  @version 2.8
 *  @since   2.8
 *  @tparam A    the element type of the view
 *  @tparam Coll the type of the underlying collection containing the elements.
 *  @tparam This the type of the view itself
 */
trait IterableViewLike[+A, 
                       +Coll,
                       +This <: IterableView[A, Coll] with IterableViewLike[A, Coll, This]] 
     extends Iterable[A]
        with IterableLike[A, This]
        with TraversableView[A, Coll]
        with TraversableViewLike[A, Coll, This]
        with GenIterableViewLike[A, Coll, This]
{ self =>
  
  trait Transformed[+B] extends IterableView[B, Coll] with super[TraversableViewLike].Transformed[B] with super[GenIterableViewLike].Transformed[B] {
    def iterator: Iterator[B]
    override def foreach[U](f: B => U): Unit = iterator foreach f
    override def toString = viewToString
  }

  trait EmptyView extends Transformed[Nothing] with super[TraversableViewLike].EmptyView with super[GenIterableViewLike].EmptyView

  trait Forced[B] extends super[TraversableViewLike].Forced[B] with super[GenIterableViewLike].Forced[B] with Transformed[B]

  trait Sliced extends super[TraversableViewLike].Sliced with super[GenIterableViewLike].Sliced with Transformed[A]

  trait Mapped[B] extends super[TraversableViewLike].Mapped[B] with super[GenIterableViewLike].Mapped[B] with Transformed[B]

  trait FlatMapped[B] extends super[TraversableViewLike].FlatMapped[B] with super[GenIterableViewLike].FlatMapped[B] with Transformed[B]
    
  trait Appended[B >: A] extends super[TraversableViewLike].Appended[B] with super[GenIterableViewLike].Appended[B] with Transformed[B]

  trait Filtered extends super[TraversableViewLike].Filtered with super[GenIterableViewLike].Filtered with Transformed[A]
    
  trait TakenWhile extends super[TraversableViewLike].TakenWhile with super[GenIterableViewLike].TakenWhile with Transformed[A]

  trait DroppedWhile extends super[TraversableViewLike].DroppedWhile with super[GenIterableViewLike].DroppedWhile with Transformed[A]

  trait Zipped[B] extends Transformed[(A, B)] with super[GenIterableViewLike].Zipped[B]
  
  trait ZippedAll[A1 >: A, B] extends Transformed[(A1, B)] with super[GenIterableViewLike].ZippedAll[A1, B]
 
  /** Boilerplate method, to override in each subclass
   *  This method could be eliminated if Scala had virtual classes
   */
  protected def newZipped[B](that: GenIterable[B]): Transformed[(A, B)] = new { val other = that } with Zipped[B]
  protected def newZippedAll[A1 >: A, B](that: GenIterable[B], _thisElem: A1, _thatElem: B): Transformed[(A1, B)] = new {
    val other: GenIterable[B] = that
    val thisElem = _thisElem
    val thatElem = _thatElem
  } with ZippedAll[A1, B]
  protected override def newForced[B](xs: => GenSeq[B]): Transformed[B] = new { val forced = xs } with Forced[B]
  protected override def newAppended[B >: A](that: GenTraversable[B]): Transformed[B] = new { val rest = that } with Appended[B]
  protected override def newMapped[B](f: A => B): Transformed[B] = new { val mapping = f } with Mapped[B]
  protected override def newFlatMapped[B](f: A => Flattenable[B]): Transformed[B] = new { val mapping = f } with FlatMapped[B]
  protected override def newFiltered(p: A => Boolean): Transformed[A] = new { val pred = p } with Filtered
  protected override def newSliced(_endpoints: SliceInterval): Transformed[A] = new { val endpoints = _endpoints } with Sliced
  protected override def newDroppedWhile(p: A => Boolean): Transformed[A] = new { val pred = p } with DroppedWhile
  protected override def newTakenWhile(p: A => Boolean): Transformed[A] = new { val pred = p } with TakenWhile
  
  override def zip[A1 >: A, B, That](that: GenIterable[B])(implicit bf: CanBuildFrom[This, (A1, B), That]): That = {
    newZipped(that).asInstanceOf[That]
// was:    val b = bf(repr)
//    if (b.isInstanceOf[NoBuilder[_]]) newZipped(that).asInstanceOf[That]
//    else super.zip[A1, B, That](that)(bf)    
  }

  override def zipWithIndex[A1 >: A, That](implicit bf: CanBuildFrom[This, (A1, Int), That]): That =
    zip[A1, Int, That](Stream from 0)(bf)

  override def zipAll[B, A1 >: A, That](that: GenIterable[B], thisElem: A1, thatElem: B)(implicit bf: CanBuildFrom[This, (A1, B), That]): That =
    newZippedAll(that, thisElem, thatElem).asInstanceOf[That]

  override def grouped(size: Int): Iterator[This] =
    self.iterator grouped size map (x => newForced(x).asInstanceOf[This])

  override def sliding[B >: A](size: Int, step: Int): Iterator[This] =
    self.iterator.sliding(size, step) map (x => newForced(x).asInstanceOf[This])

  override def stringPrefix = "IterableView"
}
