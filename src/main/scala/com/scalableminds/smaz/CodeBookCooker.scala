package com.scalableminds.smaz

import java.io._

import scala.collection.mutable

object CodeBookCooker {
  val NUMBER_REPLACEMENTS_TO_EVALUATE = 10
  
  /**
   * Calculate the hash of a string into the code book
   */
  private def codeBookHashFor(s: String) = s.length match {
    case 1 =>
      (s(0) << 3) % Smaz.CODE_HASH_MAP_SIZE
    case 2 =>
      ((s(0) << 3) + s(1)) % Smaz.CODE_HASH_MAP_SIZE
    case l =>
      (((s(0) << 3) + s(1)) ^ s(2)) % Smaz.CODE_HASH_MAP_SIZE
  }

  /**
   * Create a code book from a reverse code book 
   * @param reverseCodeBook foundation
   * @return code book
   */
  def codeBookFromReverse(reverseCodeBook: Array[String]) = {
    val codeBook = Array.fill[String](Smaz.CODE_HASH_MAP_SIZE)("")
    reverseCodeBook.zipWithIndex.foreach {
      case (decoded, _) if decoded.length == 0 => // Skip
      case (decoded, code) =>
        val hash = codeBookHashFor(decoded)
        codeBook(hash) = codeBook(hash) + decoded.length.toChar + decoded + code.toChar
    }
    codeBook
  }

  /**
   * Create a pretty printed version of the code book that can be printed to an output and copied into code
   * @param reverseCodeBook base for the code book
   * @return a string containing a pretty printed version of the created code book
   */
  def prettyPrintCodeBookFromReverse(reverseCodeBook: Array[String]) = {
    val codeBook = Array.fill[String](Smaz.CODE_HASH_MAP_SIZE)("")
    reverseCodeBook.zipWithIndex.foreach {
      case (decoded, _) if decoded.length == 0 => // Skip
      case (decoded, code) =>
        val hash = codeBookHashFor(decoded)
        codeBook(hash) = codeBook(hash) + "\\%03o%s\\%03o".format(decoded.length, decoded, code)
    }
    codeBook.map(s => "\"" + s + "\"").mkString("Array(",", ", ")")
  }

  /**
   * Find a code in the code book that doesn't hold a lot of value. The value is estimated using the invocation
   * counts during the compression of the test strings
   * @param testStrings reference strings
   * @param reverseCodeBook code book to analyze
   * @return a code string with low compression power
   */
  private def findLeastValuable(testStrings: Array[String], 
                        reverseCodeBook: Array[String]): ((String, Int), Int) = {
    val codeBook = codeBookFromReverse(reverseCodeBook)
    
    val combinedAccessLog = new Array[Int](Smaz.NUMBER_OF_CODES)
    
    val size = testStrings.map(s =>
      Smaz.compressWithStats(s, codeBook, combinedAccessLog).length).sum
    
    println(s"compressed everything. size $size")
    println("cloned performance")
    val leastImportant = reverseCodeBook.zipWithIndex.sortBy {
      case (decoded, idx) =>
        val updatedRCB = reverseCodeBook.updated(idx, "")
        val encodedWord = Smaz.compress(decoded, codeBookFromReverse(updatedRCB))
        val estimatedSizeBenefit = combinedAccessLog(idx) * (encodedWord.length - 1)
        estimatedSizeBenefit
    }.head
    (leastImportant, size)
  }

  /**
   * Counts how often combination of strings occour in a set of test strings
   * @param testStrings strings to count character occourences for
   * @return character-ngram distribution
   */
  private def calculateNGramCounts(testStrings: Array[String]) = {
    lazy val counts = mutable.HashMap.empty[String, Int].withDefaultValue(0)
    def countNGrams(s: String): Unit = {
      (1 to s.length).foreach { i =>
        s.sliding(i).foreach(ng => counts.update(ng, counts(ng) + 1))
      }
    }

    testStrings.foreach(countNGrams)
    counts
  }

  /**
   * Defines the influence of the length of ngrams during the replacement search
   * @param l length
   * @return factor
   */
  private def lengthInfluence(l: Int) = {
    if (l == 1)
      1.0
    else
      (l + 1) * 0.5
  }

  /**
   * Search for a good code to add to the codebook to replace x 
   * @param x code to replace
   * @param testStrings test strings to evaluate on
   * @param reverseCodeBook reverse cook book
   * @param ngramCounts character ngram counts of the test strings
   * @return a candidate to be added to the code book
   */
  private def findBestReplacementFor(x: (String, Int), 
                             testStrings: Array[String], 
                             reverseCodeBook: Array[String], 
                             ngramCounts: mutable.Map[String, Int]) = {
    
    val codebook = codeBookFromReverse(reverseCodeBook)

    val rcbSet = reverseCodeBook.toSet

    val bestCandidates = ngramCounts.par
      .filterNot{ case (ngram, _) => rcbSet.contains(ngram)}.seq.toSeq
      .sortBy{ case (ngram, count) => -count * lengthInfluence(ngram.length)}
      .take(NUMBER_REPLACEMENTS_TO_EVALUATE)
      .map{ case (ngram, _) => ngram }

    val best = bestCandidates :+ x._1

    best.par.map { str =>
      val updatedRCB = reverseCodeBook.updated(x._2, str)
      val encodedWord = testStrings.map(s => Smaz.compress(s, codeBookFromReverse(updatedRCB)).length).sum
      str -> encodedWord
    }.seq.sortBy(_._2).head
  }

  /**
   * Experts a revert code book to a file
   */
  def writeReverseCodeBook(reverseCodebBook: Array[String], output: File) = {
    val writer = new PrintWriter(output)
    reverseCodebBook.foreach(s => writer.write(s + "\n"))
    writer.close()
  }

  /**
   * Create a tuned code book that gets optimized to encode the passed test strings. The test strings should resemble
   * character and word distributions of the data that will be encoded later on.
   * @param testStrings test strings
   * @return tuned reverse code book
   */
  def tuneOn(testStrings: Array[String], maxIterations: Int = Int.MaxValue) = {
    val ngramCounts = calculateNGramCounts(testStrings)
    println("Finished calculating ngrams")
    def tuneCodeBook(testStrings: Array[String], reverseCookbook: Array[String], i: Int): Array[String] = {
      println(s"Tune started")
      val x = findLeastValuable(testStrings, reverseCookbook)
      println(s"Found least valuable: '${x._1}'")
      val r = findBestReplacementFor(x._1, testStrings, reverseCookbook, ngramCounts)
      println(s"Iteration $i ($x <-> $r). Diff: ${x._2 - r._2}")

      if (x._1._1 == r._1 || i >= maxIterations) {
        reverseCookbook
      } else {
        val updated = reverseCookbook.updated(x._1._2, r._1)
        tuneCodeBook(testStrings, updated, i + 1)
      }
    }
    tuneCodeBook(testStrings, Smaz.REVERSE_CODEBOOK, 0)
  }
}