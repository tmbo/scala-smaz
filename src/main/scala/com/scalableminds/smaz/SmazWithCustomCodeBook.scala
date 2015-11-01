package com.scalableminds.smaz

trait SmazWithCustomCodeBook extends Smaz{
  import com.scalableminds.smaz.Smaz._

  def CODEBOOK: CodeBook

  def REVERSE_CODEBOOK: CodeBook

  def compressWithStats(inString: String, accessLog: Array[Int]): Array[Byte] = {
    compressImpl(inString, CODEBOOK, accessLog = Some(accessLog))
  }

  def compress(inString: String): Array[Byte] = {
    compressImpl(inString, CODEBOOK, accessLog = None)
  }

  def decompress(strBytes: Array[Byte], offset: Int, length: Int): String = {
    decompress(strBytes, offset, length, REVERSE_CODEBOOK)
  }

  def decompress(strBytes: Array[Byte]): String = {
    decompress(strBytes, 0, strBytes.length, REVERSE_CODEBOOK)
  }
}

