import java.io.File

import com.scalableminds.smaz.CodeBookCooker

import scala.io.Source

object CodeBookTuner {

  def main(args: Array[String]) {
    if (args.length < 4) {
      println("Wrong number of arguments. Need three files numIterations | input | codebook | reverse")
      return
    }
    
    val numIterations = args(0)
    val inputFile = args(1)
    val codOut = args(2)
    val revOut = args(3)
    val testStrings = Source.fromFile(new File(inputFile)).getLines().toArray
    val reverseCodeBook = CodeBookCooker.tuneOn(testStrings, numIterations.toInt)

    CodeBookCooker.writeReverseCodeBook(reverseCodeBook, new File(revOut))
    CodeBookCooker.writeCodeBook(reverseCodeBook, new File(codOut))
  }
}
