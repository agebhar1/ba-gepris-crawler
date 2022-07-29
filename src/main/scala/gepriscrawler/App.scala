package gepriscrawler

import java.io.File
import scala.io.StdIn.readLine

// Main entry point of the Crawler App
object App extends App {

println("New crawl (N) or resume (R) crawl?")
val mode = readLine()

if(mode.toUpperCase=="N")
{
  println("Enter the path where to save the crawl")
  val path = readLine()
  GeprisCrawler.startNewCrawl(new File(path).getAbsolutePath())
}
if(mode.toUpperCase=="R")
{
  println("Enter the path to the partial crawl")
  val path = readLine()
  println("Which stage should be resumed? (-1 if the the program should determine itself.")
  val stage = readLine().toInt
  GeprisCrawler.resumeExistingCrawl(
          exportRootPath = new File(path).getAbsolutePath(),
          stageToStartFrom = (if(stage == -1) None else Some(stage)))
}

}





