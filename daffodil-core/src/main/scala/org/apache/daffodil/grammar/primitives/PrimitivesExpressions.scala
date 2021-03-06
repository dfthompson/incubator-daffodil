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

package org.apache.daffodil.grammar.primitives

import org.apache.daffodil.grammar._
import org.apache.daffodil.dsom._
import org.apache.daffodil.dpath._
import org.apache.daffodil.xml.XMLUtils
import org.apache.daffodil.xml.GlobalQName
import org.apache.daffodil.processors.parsers.{ Parser => DaffodilParser }
import org.apache.daffodil.processors.unparsers.{ Unparser => DaffodilUnparser }
import org.apache.daffodil.exceptions.Assert
import org.apache.daffodil.Implicits._
import org.apache.daffodil.processors.parsers.NewVariableInstanceStartParser
import org.apache.daffodil.processors.parsers.AssertExpressionEvaluationParser
import org.apache.daffodil.dsom.ElementBase
import org.apache.daffodil.processors.parsers.AssertPatternParser
import org.apache.daffodil.processors.parsers.DiscriminatorPatternParser
import org.apache.daffodil.processors.parsers.NewVariableInstanceEndParser
import org.apache.daffodil.processors.parsers.SetVariableParser
import org.apache.daffodil.processors.parsers.IVCParser
import org.apache.daffodil.processors.unparsers.SetVariableUnparser
import org.apache.daffodil.processors.unparsers.NewVariableInstanceEndUnparser
import org.apache.daffodil.processors.unparsers.NewVariableInstanceStartUnparser
import org.apache.daffodil.compiler.ForParser
import org.apache.daffodil.processors.unparsers.NadaUnparser
import org.apache.daffodil.schema.annotation.props.PropertyLookupResult
import org.apache.daffodil.schema.annotation.props.Found
import org.apache.daffodil.dsom.ExpressionCompilers
import org.apache.daffodil.dsom.DFDLSetVariable
import org.apache.daffodil.dsom.ExpressionCompilers
import org.apache.daffodil.dsom.DFDLNewVariableInstance

abstract class AssertBase(decl: AnnotatedSchemaComponent,
  exprWithBraces: String,
  namespacesForNamespaceResolution: scala.xml.NamespaceBinding,
  scWherePropertyWasLocated: AnnotatedSchemaComponent,
  msg: String,
  discrim: Boolean, // are we a discriminator or not.
  assertKindName: String)
  extends ExpressionEvaluatorBase(scWherePropertyWasLocated) {

  def this(
    decl: AnnotatedSchemaComponent,
    foundProp: Found,
    msg: String,
    discrim: Boolean, // are we a discriminator or not.
    assertKindName: String) =
    this(decl, foundProp.value, foundProp.location.namespaces, decl, msg, discrim, assertKindName)

  override val baseName = assertKindName
  override lazy val exprText = exprWithBraces
  override lazy val exprNamespaces = namespacesForNamespaceResolution
  override lazy val exprComponent = scWherePropertyWasLocated
  override def nodeKind = NodeInfo.Boolean

  override val forWhat = ForParser

  lazy val parser: DaffodilParser = new AssertExpressionEvaluationParser(msg, discrim, decl.runtimeData, expr)

  override def unparser: DaffodilUnparser = new NadaUnparser(decl.runtimeData) // Assert.invariantFailed("should not request unparser for asserts/discriminators")

}

abstract class AssertBooleanPrimBase(
  decl: AnnotatedSchemaComponent,
  stmt: DFDLAssertionBase,
  discrim: Boolean, // are we a discriminator or not.
  assertKindName: String) extends AssertBase(decl, Found(stmt.testTxt, stmt, "test", false), stmt.message, discrim, assertKindName)

case class AssertBooleanPrim(
  decl: AnnotatedSchemaComponent,
  stmt: DFDLAssertionBase)
  extends AssertBooleanPrimBase(decl, stmt, false, "assert") {
}

case class DiscriminatorBooleanPrim(
  decl: AnnotatedSchemaComponent,
  stmt: DFDLAssertionBase)
  extends AssertBooleanPrimBase(decl, stmt, true, "discriminator")

// TODO: performance wise, initiated content is supposed to be faster
// than evaluating an expression. There should be a better way to say
// "resolve this point of uncertainty" without having to introduce
// an XPath evaluator that runs fn:true() expression.
case class InitiatedContent(
  decl: AnnotatedSchemaComponent)
  extends AssertBase(decl,
    "{ fn:true() }", <xml xmlns:fn={ XMLUtils.XPATH_FUNCTION_NAMESPACE }/>.scope, decl,
    // always true. We're just an assertion that says an initiator was found.
    "initiatedContent. This message should not be used.",
    true,
    "initiatedContent") {
}

case class SetVariable(decl: AnnotatedSchemaComponent, stmt: DFDLSetVariable)
  extends ExpressionEvaluatorBase(decl) {

  val baseName = "SetVariable[" + stmt.varQName.local + "]"

  override lazy val exprText = stmt.value
  override lazy val exprNamespaces = stmt.xml.scope
  override lazy val exprComponent = stmt

  override lazy val nodeKind = stmt.defv.primType

  lazy val parser: DaffodilParser = new SetVariableParser(expr, stmt.defv.runtimeData)
  override lazy val unparser: DaffodilUnparser = new SetVariableUnparser(expr, stmt.defv.runtimeData, stmt.nonTermRuntimeData)
}

abstract class NewVariableInstanceBase(decl: AnnotatedSchemaComponent, stmt: DFDLNewVariableInstance)
  extends Terminal(decl, true) {
}

case class NewVariableInstanceStart(decl: AnnotatedSchemaComponent, stmt: DFDLNewVariableInstance)
  extends NewVariableInstanceBase(decl, stmt) {

  lazy val parser: DaffodilParser = new NewVariableInstanceStartParser(decl.runtimeData)
  override lazy val unparser: DaffodilUnparser = new NewVariableInstanceStartUnparser(decl.runtimeData)
}

case class NewVariableInstanceEnd(decl: AnnotatedSchemaComponent, stmt: DFDLNewVariableInstance)
  extends NewVariableInstanceBase(decl, stmt) {

  lazy val parser: DaffodilParser = new NewVariableInstanceEndParser(decl.runtimeData)
  override lazy val unparser: DaffodilUnparser = new NewVariableInstanceEndUnparser(decl.runtimeData)
}

/**
 * Refactored primitives that use expressions to put expression evaluation in one place.
 * On this base (for the primitive), and a corresponding parser base class for the
 * actual evaluation.
 *
 * That fixed a bug where a SDE wasn't being reported until the parser was run that
 * could have been reported at compilation time.
 *
 * Anything being computed that involves the dsom or grammar objects or attributes of them,
 * should be done in the grammar primitives, and NOT in the parser.
 * This is important to insure errors are captured at compilation time and
 * reported on relevant objects.
 */
abstract class ExpressionEvaluatorBase(e: AnnotatedSchemaComponent) extends Terminal(e, true) {
  override def toString = baseName + "(" + exprText + ")"

  def baseName: String
  def exprNamespaces: scala.xml.NamespaceBinding
  def exprComponent: SchemaComponent
  def exprText: String

  def nodeKind: NodeInfo.Kind

  private def qn = GlobalQName(Some("daf"), baseName, XMLUtils.dafintURI)

  lazy val expr = LV('expr) {
    ExpressionCompilers.AnyRef.compileExpression(qn,
      nodeKind, exprText, exprNamespaces, exprComponent.dpathCompileInfo, false, this)
  }.value
}

abstract class ValueCalcBase(e: ElementBase,
  property: PropertyLookupResult)
  extends ExpressionEvaluatorBase(e) {

  override lazy val exprText = exprProp.value
  override lazy val exprNamespaces = exprProp.location.namespaces
  override lazy val exprComponent = exprProp.location.asInstanceOf[SchemaComponent]

  lazy val pt = e.primType //.typeRuntimeData
  override lazy val nodeKind = pt
  lazy val ptn = pt.name

  lazy val exprProp = property.asInstanceOf[Found]

}

case class InputValueCalc(e: ElementBase,
  property: PropertyLookupResult)
  extends ValueCalcBase(e, property) {

  override def baseName = "inputValueCalc"

  override lazy val parser: DaffodilParser = {
    new IVCParser(expr, e.elementRuntimeData)
  }

  override lazy val unparser = Assert.usageError("Not to be called on InputValueCalc class.")
}

//case class OutputValueCalcStaticLength(e: ElementBase,
//  property: PropertyLookupResult,
//  ovcRepUnparserGram: Gram,
//  knownLengthInBits: Long)
//  extends ValueCalcBase(e, property) {
//
//  override def baseName = "outputValueCalc"
//
//  override lazy val parser = Assert.usageError("Not to be called on OutputValueCalc class.")
//
//  override lazy val unparser = {
//    val ovcRepUnparser = ovcRepUnparserGram.unparser
//    val unp = new ElementOutputValueCalcStaticLengthUnparser(e.elementRuntimeData, ovcRepUnparser, MaybeULong(knownLengthInBits))
//    unp
//  }
//}
//
//case class OutputValueCalcRuntimeLength(e: ElementBase,
//  property: PropertyLookupResult,
//  ovcRepUnparserGram: Gram,
//  lengthEv: LengthEv,
//  lengthUnits: LengthUnits)
//  extends ValueCalcBase(e, property) {
//
//  override def baseName = "outputValueCalc"
//
//  override lazy val parser = Assert.usageError("Not to be called on OutputValueCalc class.")
//
//  override lazy val unparser = {
//    val ovcRepUnparser = ovcRepUnparserGram.unparser
//    val unp = new ElementOutputValueCalcRuntimeLengthUnparser(e.elementRuntimeData, ovcRepUnparser, lengthEv, lengthUnits)
//    unp
//  }
//}
//
//case class OutputValueCalcVariableLength(e: ElementBase,
//  property: PropertyLookupResult,
//  ovcRepUnparserGram: Gram)
//  extends ValueCalcBase(e, property) {
//
//  override def baseName = "outputValueCalc"
//
//  override lazy val parser = Assert.usageError("Not to be called on OutputValueCalc class.")
//
//  override lazy val unparser = {
//    val ovcRepUnparser = ovcRepUnparserGram.unparser
//    //
//    // same "static length" unparser, but the length is optional, so in this case we don't provide it.
//    //
//    val unp = new ElementOutputValueCalcStaticLengthUnparser(e.elementRuntimeData, ovcRepUnparser, MaybeULong.Nope)
//    unp
//  }
//}

abstract class AssertPatternPrimBase(decl: Term, stmt: DFDLAssertionBase)
  extends Terminal(decl, true) {

  lazy val eName = decl.diagnosticDebugName
  lazy val testPattern = {
    PatternChecker.checkPattern(stmt.testTxt, decl)
    stmt.testTxt
  }

  override val forWhat = ForParser

  def parser: DaffodilParser

  override def unparser: DaffodilUnparser = Assert.invariantFailed("should not request unparser for asserts/discriminators")
}

case class AssertPatternPrim(term: Term, stmt: DFDLAssert)
  extends AssertPatternPrimBase(term, stmt) {

  val kindString = "AssertPatternPrim"

  lazy val parser: DaffodilParser = {
    new AssertPatternParser(eName, kindString, term.termRuntimeData, testPattern, stmt.message)
  }

}

case class DiscriminatorPatternPrim(term: Term, stmt: DFDLAssertionBase)
  extends AssertPatternPrimBase(term, stmt) {

  val kindString = "DiscriminatorPatternPrim"

  lazy val parser: DaffodilParser = new DiscriminatorPatternParser(testPattern, eName, kindString, term.termRuntimeData, stmt.message)
}

