// See LICENSE for license details.

package chiselTests

import chisel3._
import chisel3.experimental.chiselName
import org.scalatest._
import org.scalatest.prop._
import chisel3.testers.BasicTester

import scala.collection.mutable.ListBuffer

trait NamedModuleTester extends Module {
  val io = IO(new Bundle())  // Named module testers don't need IO

  val expectedNameMap = ListBuffer[(Data, String)]()

  /** Expects some name for a node that is propagated to FIRRTL.
    * The node is returned allowing this to be called inline.
    */
  def expectName[T <: Data](node: T, fullName: String): T = {
    expectedNameMap += ((node, fullName))
    node
  }

  /** After this module has been elaborated, returns a list of (node, expected name, actual name)
    * that did not match expectations.
    * Returns an empty list if everything was fine.
    */
  def getNameFailures(): List[(Data, String, String)] = {
    val failures = ListBuffer[(Data, String, String)]()
    for ((ref, expectedName) <- expectedNameMap) {
      if (ref.instanceName != expectedName) {
        failures += ((ref, expectedName, ref.instanceName))
      }
    }
    failures.toList
  }
}

@chiselName
class NamedModule extends NamedModuleTester {
  @chiselName
  def FunctionMockup2(): UInt = {
    val my2A = 1.U
    val my2B = expectName(my2A +& 2.U, "test_myNested_my2B")
    val my2C = my2B +& 3.U  // should get named at enclosing scope
    my2C
  }

  @chiselName
  def FunctionMockup(): UInt = {
    val myNested = expectName(FunctionMockup2(), "test_myNested")
    val myA = expectName(1.U + myNested, "test_myA")
    val myB = expectName(myA +& 2.U, "test_myB")
    val myC = expectName(myB +& 3.U, "test_myC")
    myC +& 4.U  // obviously named at enclosing scope
  }

  val test = expectName(FunctionMockup(), "test")
  val test2 = expectName(test +& 2.U, "test2")
}

/** Ensure no crash happens if a named function is enclosed in a non-named module
  */
class NonNamedModule extends NamedModuleTester {
  @chiselName
  def NamedFunction(): UInt = {
    val myVal = 1.U + 2.U
    myVal
  }

  val test = NamedFunction()
}

/** Ensure no crash happens if a named function is enclosed in a non-named function in a named
  * module.
  */
@chiselName
class NonNamedFunction extends NamedModuleTester {
  @chiselName
  def NamedFunction(): UInt = {
    val myVal = 1.U + 2.U
    myVal
  }

  def NonNamedFunction() : UInt = {
    val myVal = NamedFunction()
    myVal
  }

  val test = NamedFunction()
}

/** A simple test that checks the recursive function val naming annotation both compiles and
  * generates the expected names.
  */
class NamingAnnotationSpec extends ChiselPropSpec {
  property("NamedModule should have function hierarchical names") {
    // TODO: clean up test style
    var module: NamedModule = null
    elaborate { module = new NamedModule; module }
    assert(module.getNameFailures() == Nil)
  }

  property("NonNamedModule should elaborate") {
    elaborate { new NonNamedModule }
  }

  property("NonNamedFunction should elaborate") {
    elaborate { new NonNamedFunction }
  }
}
