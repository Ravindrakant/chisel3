// See LICENSE for license details.

package chiselTests

import chisel3._
import firrtl.FirrtlExecutionSuccess
import firrtl.transforms.DedupModules
import org.scalatest.{FreeSpec, Matchers}

trait NoDedupAnnotator {
  self: Module =>

  def doNotDedup(module: Module): Unit = {
    annotate(ChiselAnnotation(module, classOf[DedupModules], "nodedup!"))
  }
}

class MuchUsedModule extends Module {
  val io = IO(new Bundle {
    val in = Input(UInt(16.W))
    val out = Output(UInt(16.W))
  })
  io.out := io.in +% 1.U
}

class UsesMuchUsedModule(addAnnos: Boolean) extends Module with NoDedupAnnotator{
  val io = IO(new Bundle {
    val in = Input(UInt(16.W))
    val out = Output(UInt(16.W))
  })

  val mod0 = Module(new MuchUsedModule)
  val mod1 = Module(new MuchUsedModule)
  val mod2 = Module(new MuchUsedModule)
  val mod3 = Module(new MuchUsedModule)

  mod0.io.in := io.in
  mod1.io.in := mod0.io.out
  mod2.io.in := mod1.io.out
  mod3.io.in := mod2.io.out
  io.out := mod3.io.out

  if(addAnnos) {
    doNotDedup(mod1)
    doNotDedup(mod3)
  }
}

class AnnotationNoDedup extends FreeSpec with Matchers {
  "Firrtl provides transform that reduces identical modules to a single instance" - {
    "Annotations can be added which will defeat this deduplication for specific modules instances" in {
      Driver.execute(Array("-X", "low"), () => new UsesMuchUsedModule(addAnnos = true)) match {
        case ChiselExecutionSucccess(_, _, Some(firrtlResult: FirrtlExecutionSuccess)) =>
          val lowFirrtl = firrtlResult.emitted

          lowFirrtl should include ("module MuchUsedModule :")
          lowFirrtl should include ("module MuchUsedModule_1 :")
          lowFirrtl should include ("module MuchUsedModule_3 :")
          lowFirrtl should not include "module MuchUsedModule_2 :"
          lowFirrtl should not include "module MuchUsedModule_4 :"
        case _ =>
      }
    }
    "Turning off these nnotations dedup all the occurrences" in {
      Driver.execute(Array("-X", "low"), () => new UsesMuchUsedModule(addAnnos = false)) match {
        case ChiselExecutionSucccess(_, _, Some(firrtlResult: FirrtlExecutionSuccess)) =>
          val lowFirrtl = firrtlResult.emitted

          lowFirrtl should include ("module MuchUsedModule :")
          lowFirrtl should not include "module MuchUsedModule_1 :"
          lowFirrtl should not include "module MuchUsedModule_3 :"
          lowFirrtl should not include "module MuchUsedModule_2 :"
          lowFirrtl should not include "module MuchUsedModule_4 :"
        case _ =>
      }
    }
  }
}
