package de.hpi.dreamteam.ir.util

import com.scalableminds.smaz.DefaultSmaz
import org.specs2.mutable._

class SmazTest extends Specification {
  sequential

  "Smaz compressor" should {
    "compress and decompress simple string" in {
      val str = "this is a simple test"
      val compressed = DefaultSmaz.compress(str)
      val uncompressed = DefaultSmaz.decompress(compressed)
      str mustEqual uncompressed
    }

    "work with utf8" in {
      val str = "g ÿa"
      val compressed = DefaultSmaz.compress(str)
      val uncompressed = DefaultSmaz.decompress(compressed)
      str mustEqual uncompressed
    }

    "work with long utf8" in {
      val str = "ᚠᛇᚻ᛫ᛒᛦᚦ᛫ᚠᚱᚩᚠᚢᚱ᛫ᚠᛁᚱᚪ᛫ᚷᛖᚻᚹᛦᛚᚳᚢᛗ"
      val compressed = DefaultSmaz.compress(str)
      val uncompressed = DefaultSmaz.decompress(compressed)
      str mustEqual uncompressed
    }


    "run original smaz tests" in {
      val strings = Array("This is a small string", "foobar", "the end", "not-a-g00d-Exampl333", "Smaz is a simple compression library", "1000 numbers 2000 will 10 20 30 compress very little", "and now a few italian sentences:", "Nel mezzo del cammin di nostra vita, mi ritrovai in una selva oscura", "Mi illumino di immenso", "try it against urls", "http://google.com", "http://programming.reddit.com", "http://github.com/antirez/smaz/tree/master", "На берегу пустынных волн")
      val expectedCompression = Array(0.50, 0.33, 0.57, -0.15, 0.39, 0.10, 0.41, 0.32, 0.36, 0.37, 0.47, 0.45, 0.4, -0.18)
      val compressed = strings.map(s => DefaultSmaz.compress(s))
      val uncompressed = compressed.map(c => DefaultSmaz.decompress(c))
      val compressionLevel = compressed.zip(strings).map { case (c, s) => math.round((1 - c.length.toFloat / s.getBytes.length) * 100).toDouble / 100 }
      compressionLevel mustEqual expectedCompression
      uncompressed mustEqual strings
    }
  }
}