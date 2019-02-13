import org.apache.log4j.{Level, Logger}
import org.apache.spark.SparkContext







class TSP_SPARK extends java.io.Serializable {


  val N_CORE = 4
  private var nodes: Set[String] = Set()



  private def reduce(distance:Map[(String, String), Float], oldLb:Float) = {

    var matrix = distance
    var lb = oldLb

    // riduzione sulle righe
    // min_row conterrà il valore minimo per ogni riga della matrice matrix
    val min_row = {
      for {
        node1 <- matrix.keySet.unzip._1
      } yield node1 -> matrix.filterKeys(_._1 == node1).values.min
    }.toMap

    lb += min_row.values.sum

    // aggiorno ogni nodo dimunendo il valore dell'arco del min_row
    for (
      node1 <- matrix.keySet.unzip._1;
      node2 <- matrix.keySet.unzip._2
    ) {
      if (matrix.contains(node1, node2)) {
        matrix += (node1, node2) -> (matrix(node1, node2) - min_row(node1))
      }
    }

    // riduzione sulle colonne
    // min_col conterrà il valore minimo per ogni colonna della matrice matrix
    val min_col = {
      for {
        node1 <-  matrix.keySet.unzip._2
      } yield node1 -> matrix.filterKeys(_._2 == node1).values.min
    }.toMap

    lb += min_col.values.sum

    // aggiorno ogni nodo dimunendo il valore dell'arco del min_col
    for (
      node1 <- matrix.keySet.unzip._1;
      node2 <- matrix.keySet.unzip._2
    ) {
      if (matrix.contains(node1, node2)) {
        matrix += (node1, node2) -> (matrix(node1, node2) - min_col(node2))
      }
    }

    // restrituisce la matrice aggiornata e il nuovo lower bound
    (matrix, lb)
  }







  // prende in input la matrice, l'arco da escludere e il vecchio valore del Lb e ritorna
  // il valore ridotto della matrice, il nuovo Lb e l'arco escluso
  private def escludiArco(distance:Map[(String, String), Float], arco: (String, String), oldLb:Float) = {
    var matrix = distance

    if (matrix.contains(arco._1, arco._2)) {
      matrix = matrix.filterKeys(_ != arco)
    }

    (reduce(matrix, oldLb), arco)
  }






  // prende in input la matrice, l'arco da includere e il vecchio valore del Lb e ritorna
  // il valore ridotto della matrice e il nuovo Lb
  private def includiArco(distance:Map[(String, String), Float], arco: (String, String), oldLb:Float) = {
    var matrix = distance
    val lb = oldLb + matrix(arco)

    matrix = matrix.filterKeys(_._1 != arco._1)
    matrix = matrix.filterKeys(_._2 != arco._2)
    if(matrix.contains((arco._2, arco._1))) matrix -= ((arco._2, arco._1))

    reduce(matrix, lb)
  }





  private def procedure(matrix:Map[(String, String), Float], lb:Float, n_archi:Int, lista_archi: List[(String, String)]) = {


    if(n_archi==nodes.size) {
      lista_archi.foreach(println(_))
      println("Cost: "+ lb)
      List ((matrix, -1.toFloat,-1, lista_archi), (matrix, -1.toFloat,-1, lista_archi) )
    }


    // si calcola il LB generato per l'eslusione di ogni arco
    var states:List[( (Map[(String, String), Float], Float), (String, String))] = List()
    matrix.foreach(x=> {states = escludiArco(matrix, x._1, lb) :: states})

    // se il numero di stati è 0 siamo in una configurazione non utilizzabile
    if(states.nonEmpty) {

      // ordiniamo gli archi per LB
      states = states.sortBy(_._1._2)
      // viene preso l'arco con il LB piu piccolo
      val ((_, _), arco) = states.last

      // aggiungiamo alla lista le configurazioni date dall'esclusione dell'arco e della sua inclusione
      val ((matrix3, lb3), _) = escludiArco(matrix, arco, lb)
      val (matrix4, lb4) = includiArco(matrix, arco, lb)

      List ((matrix3, lb3, n_archi, lista_archi), (matrix4, lb4, n_archi + 1, arco :: lista_archi))
    }
    else
      Nil
  }






  def time[R](block: => R): R = {
    val t0 = System.nanoTime()
    val result = block    // call-by-name
    val t1 = System.nanoTime()
    println("Elapsed time: " + (t1 - t0)/100000 + "ns")
    result
  }






  def main(): Unit = {

    val filename = "src/main/data.csv"
    var distance: Map[(String, String), Float] = Map ()   //map (nodo1, nodo2) = costo

    val data =  scala.io.Source.fromFile(filename).getLines()

    for (line <- data.drop(1)) {
      val elements = line.replace("\"", "").split(",")
      distance += (elements(0) + "-" + elements(1), elements(3) + "-" + elements(4)) -> elements(2).toFloat
      nodes += elements(0) + "-" + elements(1)
      nodes += elements(3) + "-" + elements(4)
    }

    // prima riduzione della matrice
    val(matrix, lb) = reduce(distance, 0)

    Logger.getLogger("org").setLevel(Level.ERROR)


    // Create a SparkContext using every core of the local machine
    val sc = new SparkContext("local[" + N_CORE +"]", "TSP_SPARK")

    // L è una lista che conterrà tutte le configurazioni valutate e inizia con la matrice ridotta
    val list: List[( Map[(String, String), Float], Float, Int, List[(String, String)])] = List() :+ (matrix, lb, 0, List())
    var rdd = sc.parallelize(list)


    while (!rdd.isEmpty) {
      val configs = rdd.collect().sortBy(_._2)
      val (configs_considered, configs_notConsidered) = configs.splitAt(N_CORE)

      var newRdd =  sc.parallelize(configs_considered)
      newRdd = newRdd.flatMap(x => procedure(x._1, x._2, x._3, x._4))
      val results = newRdd.collect()

      if(results.exists(x=>x._3==nodes.size-1)) {
          val element =  results.filter(_._3 == nodes.size-1).minBy(_._2)
          element._4.foreach(println(_))
          println("Cost: " + element._2)
        return
      }

      val new_configs = configs_notConsidered.union(results)
      rdd = sc.parallelize(new_configs)
    }



  }

}
