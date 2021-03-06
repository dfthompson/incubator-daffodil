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

package org.apache.daffodil.processors.unparsers

import org.apache.daffodil.processors.parsers.HasKnownLengthInBits
import org.apache.daffodil.processors.ElementRuntimeData
import org.apache.daffodil.processors.ParseOrUnparseState
import org.apache.daffodil.util.DecimalUtils
import java.lang.{ Long => JLong }
import java.math.{ BigInteger => JBigInteger }
import org.apache.daffodil.processors.parsers.HasRuntimeExplicitLength
import org.apache.daffodil.processors.Evaluatable
import org.apache.daffodil.schema.annotation.props.gen.LengthUnits

abstract class BCDIntegerBaseUnparser(
  e: ElementRuntimeData)
  extends PackedBinaryIntegerBaseUnparser(e) {

  override def fromBigInteger(bigInt: JBigInteger, nBits: Int): Array[Byte] = DecimalUtils.bcdFromBigInteger(bigInt, nBits)
}

class BCDIntegerKnownLengthUnparser(
  e: ElementRuntimeData,
  override val lengthInBits: Int)
  extends BCDIntegerBaseUnparser(e)
  with HasKnownLengthInBits {
}

class BCDIntegerRuntimeLengthUnparser(
  val e: ElementRuntimeData,
  val lengthEv: Evaluatable[JLong],
  val lUnits: LengthUnits)
  extends BCDIntegerBaseUnparser(e)
  with HasRuntimeExplicitLength {

  override lazy val runtimeDependencies = List(lengthEv)
}

final class BCDIntegerDelimitedUnparser(
  e: ElementRuntimeData)
  extends BCDIntegerBaseUnparser(e) {

  override def getBitLength(state: ParseOrUnparseState): Int = { 0 }
}

abstract class BCDDecimalBaseUnparser(
  e: ElementRuntimeData,
  binaryDecimalVirtualPoint: Int)
  extends PackedBinaryDecimalBaseUnparser(e, binaryDecimalVirtualPoint) {

  override def fromBigInteger(bigInt: JBigInteger, nBits: Int): Array[Byte] = DecimalUtils.bcdFromBigInteger(bigInt, nBits)
}

class BCDDecimalKnownLengthUnparser(
  e: ElementRuntimeData,
  binaryDecimalVirtualPoint: Int,
  override val lengthInBits: Int)
  extends BCDDecimalBaseUnparser(e, binaryDecimalVirtualPoint)
  with HasKnownLengthInBits {
}

class BCDDecimalRuntimeLengthUnparser(
  val e: ElementRuntimeData,
  binaryDecimalVirtualPoint: Int,
  val lengthEv: Evaluatable[JLong],
  val lUnits: LengthUnits)
  extends BCDDecimalBaseUnparser(e, binaryDecimalVirtualPoint)
  with HasRuntimeExplicitLength {

  override lazy val runtimeDependencies = List(lengthEv)
}

final class BCDDecimalDelimitedUnparser(
  e: ElementRuntimeData,
  binaryDecimalVirtualPoint: Int)
  extends BCDDecimalBaseUnparser(e, binaryDecimalVirtualPoint) {

  override def getBitLength(state: ParseOrUnparseState): Int = { 0 }
}
