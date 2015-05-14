package edu.illinois.ncsa.daffodil.tdml

import edu.illinois.ncsa.daffodil.util.Misc

/**
 * Creates the DFDLTestSuite object lazily, so the file isn't read into memory
 * and parsed unless you actually try to run a test using it.
 *
 * Creates the DFDLTestSuite only once.
 *
 * Provides a reset method to be called from @AfterClass to drop
 * the test suite object (and avoid memory leak).
 */
object Runner {
  def apply(dir: String, file: String): Runner = new Runner(dir, file)
}

class Runner private (dir: String, file: String) {

  private def getTS = {
    if (ts == null) {
      // This is ok to be a hard-wired "/" because these are resource identifiers, which
      // are not file-system paths that have to be made platform-specific. 
      // In other words, we don't need to use "\\" for windows here. "/" works there as well.
      val d = if (dir.endsWith("/")) dir else dir + "/"
      ts = new DFDLTestSuite(Misc.getRequiredResource(d + file))
    }
    ts
  }

  private var ts: DFDLTestSuite = null

  def runOneTest(testName: String, schema: Option[scala.xml.Node] = None, leakCheck: Boolean = false) =
    getTS.runOneTest(testName, schema, leakCheck)

  /**
   *  Call this from an @AfterClass method
   *  to drop any state (like the test suite object) so we don't leak
   */
  def reset {
    ts = null
  }

  def trace = {
    getTS.trace
  }
}