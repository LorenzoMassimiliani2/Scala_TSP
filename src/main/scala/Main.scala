object Main {

  def time[R](block: => R): Long = {
    val t0 = System.nanoTime()
    val result = block    // call-by-name
    val t1 = System.nanoTime()
    //println("Elapsed time: " + ((t1 - t0)/1000000000).toInt + "s")
    t1 - t0
  }

  def formatTime(time: Long): String = {
    val s = (time*scala.math.pow(10, -9)).toInt
    val ms =  (time*scala.math.pow(10, -6) - s*scala.math.pow(10, 3)).toInt
    val us =  (time*scala.math.pow(10, -3) - ms*scala.math.pow(10, 3) - s*scala.math.pow(10, 6)).toInt
    val ns =  (time - us*scala.math.pow(10, 3) - ms*scala.math.pow(10, 6) - s*scala.math.pow(10, 9)).toInt
    time + "\n" + s + " s, " + ms + " ms, " + us + " us, " + ns + " ns."
  }

  def main(args: Array[String]): Unit = {
    val generateData = new GenerateData(1,10)
    val tsp = new TSP_SPARK()
    println(formatTime(time(tsp.main())))
  }


}
