object Main {

// funzione che stampa il tempo di esecuzione di block
  def time[R](block: => R): Long = {
    val t0 = System.nanoTime()
    val result = block    // call-by-name
    val t1 = System.nanoTime()
    t1 - t0
  }

// formattazione del time
  def formatTime(time: Long): String = {
    val s = (time*scala.math.pow(10, -9)).toInt
    val ms =  (time*scala.math.pow(10, -6) - s*scala.math.pow(10, 3)).toInt
    val us =  (time*scala.math.pow(10, -3) - ms*scala.math.pow(10, 3) - s*scala.math.pow(10, 6)).toInt
    val ns =  (time - us*scala.math.pow(10, 3) - ms*scala.math.pow(10, 6) - s*scala.math.pow(10, 9)).toInt
    //time +"\n" +
      s + " s, " + ms + " ms, " + us + " us, " + ns + " ns."
  }

// funzione che viene eseguita dal JAR
// Prende due parametri:
// n_places = numero di nodi con i quali si vuole lavorare
// n_core = numero di unità di calcola che si vogliono utilizzare
// Se i parametri non sono specificati prendono valore di default
  def main(args: Array[String]): Unit = {

    val n_places = if(args.length > 0) args(0).toInt else 12
    val n_core = if(args.length > 1) args(1).toInt else 2

    // viene generato un file contenente le distanze tra n_places citta
    val generateData = new GenerateData()
    generateData.main(n_places)

    val tsp = new TSP_SPARK(n_core)

    println("Time spent: " + formatTime(time(tsp.main())))
  }
}
