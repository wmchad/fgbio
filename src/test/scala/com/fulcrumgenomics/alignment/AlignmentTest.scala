/*
 * The MIT License
 *
 * Copyright (c) 2017 Fulcrum Genomics LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.fulcrumgenomics.alignment

import com.fulcrumgenomics.FgBioDef._
import com.fulcrumgenomics.testing.UnitSpec
import htsjdk.samtools.{CigarOperator => Op}

class AlignmentTest extends UnitSpec {
  "Cigar.apply(String)" should "parse valid cigars" in {
    Cigar("75M")   shouldBe Cigar(IndexedSeq(CigarElem(Op.M, 75)))
    Cigar("5S70M") shouldBe Cigar(IndexedSeq(CigarElem(Op.S, 5),CigarElem(Op.M, 70)))
    Cigar("5=2X30=3I35=") shouldBe Cigar(IndexedSeq(CigarElem(Op.EQ, 5),CigarElem(Op.X, 2),CigarElem(Op.EQ, 30),CigarElem(Op.I, 3),CigarElem(Op.EQ, 35)))
  }

  it should "throw an exception if given an empty or null string" in {
    an[Exception] shouldBe thrownBy { Cigar(null.asInstanceOf[String]) } // don't do this!
    an[Exception] shouldBe thrownBy { Cigar("") } // don't do this!
  }

  Seq("  ", "M75", "75", "*", "NotACigar", "75Y", "M", "-5M").foreach { cigar =>
    it should s"throw an exception if given an invalid Cigar string '$cigar'" in {
      an[Exception] shouldBe thrownBy { Cigar(cigar) }
    }
  }

  "Alignment.paddedString" should "produce a very simple string given a very simple alignment" in {
    val expected =
      """
      +AACCGGTT
      +||||||||
      +AACCGGTT
       """.stripMargin('+').trim.lines.toSeq

    val alignment = Alignment(expected.head.replace("-", ""), expected.last.replace("-", ""), 1, 1, Cigar("8M"), 1)
    alignment.paddedString() shouldBe expected
  }

  it should "handle an alignment that has a single mismatch in it" in {
    val expected =
      """
      +AACCGGTT
      +||||||.|
      +AACCGGGT
       """.stripMargin('+').trim.lines.toSeq

    Seq("8M", "6=1X2=").foreach { cigar =>
      val alignment = Alignment(expected.head.replace("-", ""), expected.last.replace("-", ""), 1, 1, Cigar("8M"), 1)
      alignment.paddedString() shouldBe expected
    }
  }

  it should "handle an alignment with some insertions and deletions in it" in {
    val expected =
      """
      +AA--GGGTAAACC-GGGTTT
      +||  ||||||||| || |||
      +AACCGGGTAAACCCGG-TTT
       """.stripMargin('+').trim.lines.toSeq

    val alignment = Alignment(expected.head.replace("-", ""), expected.last.replace("-", ""), 1, 1, Cigar("2=2D9=1D2=1I3="), 1)
    alignment.paddedString() shouldBe expected
  }

  it should "handle a messy alignment with indels and mismatches" in {
    val expected =
      """
      +AA--GGGGGAACC-GGGTTT
      +||  |||..|||| || |||
      +AACCGGGTAAACCCGG-TTT
       """.stripMargin('+').trim.lines.toSeq

    val alignment = Alignment(expected.head.replace("-", ""), expected.last.replace("-", ""), 1, 1, Cigar("2M2D9M1D2M1I3M"), 1)
    alignment.paddedString() shouldBe expected
  }

  Seq("5S10M", "10M5H", "5M50N5M", "50P10M").foreach { cigar =>
    it should s"throw an exception with unsupported operator contained in cigar $cigar" in {
      val cig       = Cigar(cigar)
      val alignment = Alignment("AAAAAAAAAA", "AAAAAAAAAA", 1, 1, cig, 1)
      an [Exception] shouldBe thrownBy { alignment.paddedString() }
    }
  }

  it should "use alternative characters if asked to" in {
    val expected =
      """
      |AA..GGGGGAACC.GGGTTT
      |++--+++##++++-++-+++
      |AACCGGGTAAACCCGG.TTT
       """.stripMargin.trim.lines.toSeq

    val alignment = Alignment(expected.head.replace(".", ""), expected.last.replace(".", ""), 1, 1, Cigar("2M2D9M1D2M1I3M"), 1)
    alignment.paddedString(matchChar='+', mismatchChar='#', gapChar='-', padChar='.') shouldBe expected
  }
}
