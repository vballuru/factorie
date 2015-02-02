package cc.factorie.epistemodb

import cc.factorie.util
import org.junit.Assert._
import org.scalatest.junit.JUnitSuite
import org.junit.Test

/**
 * Created by beroth on 1/30/15.
 */
class TestDBMatrix extends JUnitSuite  with util.FastLogging {

  val eps = 1e-4

  @Test def getSetCellsTest() {
    val m = new DBMatrix()
    m.set(0,0,1.0)
    m.set(4,2,3.0)
    m.set(1,3,1.0)
    m.set(4,2,2.0)
    assertEquals(m.numRows(),5)
    assertEquals(m.numCols(),4)
    assertEquals(m.get(1, 3),1.0, eps)
    assertEquals(m.get(4, 2),2.0, eps)
    assertEquals(m.get(2, 2),0.0, eps)
    assertEquals(m.get(5, 5),0.0, eps)
  }

  @Test def pruneMatrixTest() {
    val m = new DBMatrix()
    m.set(1,1,1.0)
    m.set(2,2,1.0)
    m.set(2,3,1.0)
    m.set(3,3,1.0)

    val (m0, rowMap0, colMap0) = m.prune(0)
    // pruned matrix only contains biggest component, i.e. rows 2 and 3, and columns 2 and 3
    assertEquals(m0.numRows(), 2)
    assertEquals(m0.numCols(), 2)

    assertFalse(rowMap0.contains(0))
    assertFalse(colMap0.contains(0))
    assertFalse(rowMap0.contains(1))
    assertFalse(colMap0.contains(1))
    assertTrue(rowMap0.contains(2))
    assertTrue(colMap0.contains(2))
    assertTrue(rowMap0.contains(3))
    assertTrue(colMap0.contains(3))

    // check that the columns are mapped with the order preserved
    assertEquals(colMap0(2), 0)
    assertEquals(colMap0(3), 1)
    assertEquals(rowMap0(2), 0)
    assertEquals(rowMap0(3), 1)

    val (m1, rowMap1, colMap1) = m.prune(1)
    assertEquals(2, m1.numRows())
    assertEquals(1, m1.numCols())
    assertFalse(colMap1.contains(0))
    assertFalse(colMap1.contains(1))
    assertFalse(colMap1.contains(2))
    assertEquals(0, colMap1(3))
    assertFalse(rowMap1.contains(0))
    assertFalse(rowMap1.contains(1))
    assertEquals(0, rowMap1(2))
    assertEquals(1, rowMap1(3))
  }

}