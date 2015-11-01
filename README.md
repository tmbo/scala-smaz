# scala-smaz
scala-smaz is a scala port of the Smaz short string compression algorithm by Salvatore Sanfilippo and released as a C library at: https://github.com/antirez/smaz

The library supports and compresses ASCII characters. When used on short word sequences the compression is about 30% - 40%. UTF-8 characters wont get compressed and therefore your data should consist of mostly ASCII characters, but is allowed to contain an UTF-8 character here and there.

### Dependency
Add the following dependency to your build file:

```sbt
libraryDependencies += "com.scalableminds.smaz" %% "scala-smaz" % "1.0.2"
```

The library is compiled against scala 2.11.

### Usage
The library comes with a predifined `codebook`. A `codebook` defines which character sequences get compressed by replacing them with single byte values. The default `codebook` is supposed to compress short english sentences or word sequences. You can use it in the following way:
```scala
import com.scalableminds.smaz.DefaultSmaz

val testString = "this is a simple test"
val compressed: Array[Byte] = DefaultSmaz.compress(testString)
val uncompressed: String    = DefaultSmaz.decompress(compressed)
assert(testString == uncompressed)
```

If the strings you want to compress are not short sequences of english words, you can improve the compression performance by creating your own `codebook`. E.g. you could create a `codebook` that is speciallized in compressing single english words:
```scala
import com.scalableminds.smaz.{CodeBookCooker, SmazWithCustomCodeBook}

// Choose as many as possible (but keep in mind, the longer and more examples you got, the longer the 
// training is going to take)
val testStrings = Array("hello", "world", "compress", "me", "as", "good", "as", "possible")

// train the compression library on the test strings. This might take a while
val reversedCodeBook = CodeBookCooker.tuneOn(testStrings)
val codeBook = CodeBookCooker.codeBookFromReverse(reversedCodeBook)

// Create your own compression object
object WordSmaz extends SmazWithCustomCodeBook{
  val CODEBOOK = codeBook
  val REVERSE_CODEBOOK = reversedCodeBook
}

//Use the custom compression
WordSmaz.compress("hello")
```

The test strings should resemble the distribution of character sequences of the data you want to compress.

### License
MIT. Please have a look at the [LICENSE](LICENSE) file.
