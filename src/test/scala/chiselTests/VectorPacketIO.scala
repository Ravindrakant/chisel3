// See LICENSE for license details.

package chiselTests

import Chisel._
import Chisel.testers.BasicTester

/**
  * This test illustrates the creation of a firrtl file
  * with missing declarations, the problem is exposed by
  * the creation of the val outs in VectorPacketIO
  * NOTE: The problem does not exists if the initialization
  * code is removed from DeqIO and EnqIO
  * see: Decoupled.scala lines 29 and 38
  * valid := Bool(false) and ready := Bool(false)
  * statements inside a bundle
  */
class Packet extends Bundle {
  val header = UInt(width = 1)
}

/**
  * The problem occurs with just the ins or the outs
  * lines also.
  * The problem does not occur if the Vec is taken out
  */
class VectorPacketIO(n: Int) extends Bundle {
  val ins  = Vec(n, new DeqIO(new Packet()))
  val outs = Vec(n, new EnqIO(new Packet()))
}

/**
  * a module uses the vector based IO bundle
  * the value of n does not affect the error
  */
class BrokenVectorPacketModule extends Module {
  val n  = 4
  val io = new VectorPacketIO(n)
}

class VectorPacketIOUnitTester extends BasicTester {
  val device_under_test = Module(new BrokenVectorPacketModule)
}

class VectorPacketIOUnitTesterSpec extends ChiselFlatSpec {
  "a circuit using an io containing a vector of EnqIO wrapped packets" should
    "compile and run" in {
    assertTesterPasses {
      new VectorPacketIOUnitTester
    }
  }
}