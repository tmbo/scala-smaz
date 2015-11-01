package com.scalableminds.smaz

import java.io._
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.util
import org.apache.logging.log4j.LogManager;

object Smaz {
  type CodeBook = Array[String]

  /**
   * The library uses some control bytes to signal different content that needs
   * to be taken care of in different ways during decompression.
   */
  object ControlBytes{
    val UTF8_AHEAD = 253
    val SINGLE_CHAR_ASCII = 254
    val MULTI_CHAR_ASCII = 255
  }

  val MAXIMAL_VERB_BUFFER_LENGTH = 256

  val NUMBER_OF_CODES = 253

  val CODE_HASH_MAP_SIZE = 241
}

/**
 * Smaz class for compression small strings. Port to scala from 
 * <a href="https://github.com/antirez/smaz/">antirez</a>
 * This class is immutable and can be used in multiple threads.
 *
 * @author tmbo
 */
trait Smaz {
  import Smaz._
  
  val logger = LogManager.getLogger()

  def compressWithStats(inString: String,
               codeBook: CodeBook, accessLog: Array[Int]): Array[Byte] = {
    compressImpl(inString, codeBook, accessLog = Some(accessLog))
  }
  
  def compress(inString: String,
                codeBook: CodeBook): Array[Byte] = {
    compressImpl(inString, codeBook, accessLog = None)
  }
  
  /**
   * Compress the passed string and return a byte array containing the compressed data.
   *
   * @param inString string to compress
   * @return byte array compressed byte array
   */
  protected def compressImpl(inString: String,
    codeBook: CodeBook,
    accessLog: Option[Array[Int]]): Array[Byte] = {

    val verb = new StringBuilder
    val output = new ByteArrayOutputStream
    val charBuffer = CharBuffer.wrap(inString)
    var remainingCharacters = charBuffer.remaining
    val useStats = accessLog.isDefined
    
    /**
     * Fush all characters that could not be compressed 
     * @param verb characters tp flush
     * @param output target
     */    
    @inline
    def flushVerbBuffer(verb: StringBuilder, 
      output: ByteArrayOutputStream): Unit = {
      if (verb.nonEmpty) {
        outputVerb(output, verb.toString())
        verb.setLength(0)
      }
    }

    /**
     * This implementation of smaz comes with utf8 support. Nevertheless, utf8 
     * is not compressed. Instead we will use an indicator byte to signal that 
     * the following bytes are UTF8 encoded. 
     * @param current current character
     */
    @inline
    def handleUTF8(current: Int): Unit = {
      val utf8Str = new StringBuilder()
      var next = current
      var reachedEndOfString = false
      // Read until we read the first ascii character or we reach the end of the string
      while (!isASCII(next) && !reachedEndOfString) {
        utf8Str.append(next.toChar)
        remainingCharacters -= 1
        if (remainingCharacters > 0)
          next = charBuffer.get
        else
          reachedEndOfString = true
      }
      flushVerbBuffer(verb, output)
      val encoded = Charset.forName("utf-8").encode(utf8Str.toString())
      output.write(ControlBytes.UTF8_AHEAD)
      output.write(encoded.limit)
      output.write(util.Arrays.copyOf(encoded.array(), encoded.limit))
      
      // Reposition the cursor on the first character that is an 
      // ascii char after the utf8 string
      if (remainingCharacters > 0)
        charBuffer.position(charBuffer.position - 1)
    }
    
    @inline
    def isASCII(c: Int) = c <= 127
    
    while (remainingCharacters > 0) {
      var hashForLength1 = 0
      var hashForLength2 = 0
      var hashForLength3 = 0
      charBuffer.mark
      val current = charBuffer.get
      if (isASCII(current)){
        hashForLength1 = current << 3
        hashForLength2 = hashForLength1
        
        if (remainingCharacters > 1) 
          hashForLength2 += charBuffer.get
        if (remainingCharacters > 2) 
          hashForLength3 = hashForLength2 ^ charBuffer.get
        else 
          hashForLength3 = 0
        
        charBuffer.reset
        var j = 7
        if (j > remainingCharacters) j = remainingCharacters
        var found = false
        while (j > 0) {
          val slot = j match {
            case 1 =>
              CharBuffer.wrap(codeBook(hashForLength1 % CODE_HASH_MAP_SIZE))
            case 2 =>
              CharBuffer.wrap(codeBook(hashForLength2 % CODE_HASH_MAP_SIZE))
            case _ =>
              CharBuffer.wrap(codeBook(hashForLength3 % CODE_HASH_MAP_SIZE))
          }
          val slotLength = slot.length
          var slotIndex = 0
          var slotEndIndex = slotIndex + j + 1
          while (!found && slotLength > 0 && slotEndIndex <= slotLength) {
            if (slot.get(slotIndex) == j && remainingCharacters >= j &&
              (slot.subSequence(slotIndex + 1, slotEndIndex).toString == charBuffer.subSequence(0, j).toString)) {
              flushVerbBuffer(verb, output)
              val coded = slot.get(slot.get(slotIndex) + 1 + slotIndex)
              output.write(coded)
              charBuffer.position(charBuffer.position + j)
              if (useStats)
                accessLog.get(coded) = accessLog.get(coded) + 1
              remainingCharacters -= j
              found = true
            } else {
              slotIndex += 1
              slotEndIndex = slotIndex + j + 1
            }
          }
          j -= 1
        }
        if (!found) {
          if (remainingCharacters > 0) {
            remainingCharacters -= 1
            verb.append(charBuffer.subSequence(0, 1).toString)
          }
          charBuffer.position(charBuffer.position + 1)
        }
        val verbLength = verb.length
        if (verbLength == MAXIMAL_VERB_BUFFER_LENGTH || 
          verbLength > 0 && remainingCharacters == 0) {
          flushVerbBuffer(verb, output)
        }
      } else {
        handleUTF8(current = current)
      }
      remainingCharacters = charBuffer.remaining
    }
    output.toByteArray
  }

  /**
   * Outputs the verbatim string to the output stream
   * @param outputStream target output stream
   * @param str data to write
   */
  private def outputVerb(outputStream: ByteArrayOutputStream, str: String) {
    if (str.length == 1) {
      outputStream.write(ControlBytes.SINGLE_CHAR_ASCII)
      outputStream.write(str.toCharArray()(0))
    }
    else {
      outputStream.write(ControlBytes.MULTI_CHAR_ASCII)
      outputStream.write(str.length)
      try {
        outputStream.write(str.getBytes)
      }
      catch {
        case e: IOException => {
          logger.error("Error outputting verbatim data", e)
        }
      }
    }
  }

  /**
   * Decompress byte array from compress back into String
   *
   * @param strBytes byte that should be decompressed
   * @return decompressed String
   * @see Smaz#compress(String)
   */
  def decompress(strBytes: Array[Byte], 
    offset: Int, 
    length: Int, 
    reverseCodeBook: CodeBook): String = {

    val out = new StringBuilder()
    var i = offset
    while (i < length + offset) {
      val b = (0xFF & strBytes(i)).toChar
      b match {
        case ControlBytes.UTF8_AHEAD =>
          i += 1
          val utf8Length = strBytes(i)
          out.append(new String(strBytes, i + 1, utf8Length, "utf-8"))
          i += utf8Length
        case ControlBytes.SINGLE_CHAR_ASCII =>
          i += 1
          out.append(strBytes(i).toChar)
        case ControlBytes.MULTI_CHAR_ASCII =>
          i += 1
          val decodedLength: Byte = strBytes(i)
          var j = 1
          while (j <= decodedLength) {
            out.append(strBytes(i + j).toChar)
            j += 1
          }
          i += decodedLength
        case _ =>
          val loc = 0xFF & b
          out.append(reverseCodeBook(loc))
      }
      i += 1
    }
    out.toString()
  }
}