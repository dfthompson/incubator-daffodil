/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.daffodil.processors.parsers

import org.apache.daffodil.processors.ElementRuntimeData
import org.apache.daffodil.processors.LengthInBitsEv

sealed abstract class HexBinaryLengthParser(override val context: ElementRuntimeData)
  extends PrimParser {

  protected def getLengthInBits(pstate: PState): Long

  private val zeroLengthArray = new Array[Byte](0)

  override final def parse(start: PState): Unit = {
    val dis = start.dataInputStream
    val currentElement = start.simpleElement
    val nBits = getLengthInBits(start).toInt
    if (nBits == 0) {
      currentElement.setDataValue(zeroLengthArray)
    } else if (!dis.isDefinedForLength(nBits)) {
      PE(start, "Insufficient bits in data. Needed %d bit(s) but found only %d available.", nBits, dis.remainingBits.get)
    } else {
      val array = start.dataInputStream.getByteArray(nBits, start)
      currentElement.setDataValue(array)
    }
  }
}

final class HexBinarySpecifiedLengthParser(erd: ElementRuntimeData, lengthEv: LengthInBitsEv)
  extends HexBinaryLengthParser(erd) {

  override val runtimeDependencies = Seq(lengthEv)

  override def getLengthInBits(pstate: PState): Long = {
    lengthEv.evaluate(pstate).get
  }

}

final class HexBinaryEndOfBitLimitParser(erd: ElementRuntimeData)
  extends HexBinaryLengthParser(erd) {

  override val runtimeDependencies = Nil

  override def getLengthInBits(pstate: PState): Long = {
    pstate.bitLimit0b.get - pstate.bitPos0b
  }
}
