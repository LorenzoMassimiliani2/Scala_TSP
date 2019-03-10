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
    //time +"\n" +
      s + " s, " + ms + " ms, " + us + " us, " + ns + " ns."
  }

  def main(args: Array[String]): Unit = {

    val n_places = if(args.length > 0) args(0).toInt else 14
    val n_core = if(args.length > 1) args(1).toInt else 1


    val generateData = new GenerateData2()
    generateData.main(n_places)
    val tsp = new TSP_SPARK(n_core)
    println("Time spent: " + formatTime(time(tsp.main())))

  }


}
