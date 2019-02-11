import scala.io.Source
import org.apache.log4j.{Level, Logger}
import org.apache.spark.SparkContext

class TSP {

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




  def main(): Unit = {

    Logger.getLogger("org").setLevel(Level.ERROR)
    // Create a SparkContext using every core of the local machine
    val sc = new SparkContext("local[1]", "TSP")


    val filename = "src/main/data.csv"
    var distance: Map[(String, String), Float] = Map ()   //map (nodo1, nodo2) = costo
    var nodes: Set[String] = Set()

    val data = sc.textFile(filename)
    val header = data.first()

    for (line <- data.collect().filter(x=>x!=header)) {
        val elements = line.replace("\"", "").split(",")
        distance += (elements(0) + "-" + elements(1), elements(3) + "-" + elements(4)) -> elements(2).toFloat
        nodes += elements(0) + "-" + elements(1)
        nodes += elements(3) + "-" + elements(4)

    }

    // prima riduzione della matrice
    val(matrix, lb) = reduce(distance, 0)

    // L è una lista che conterrà tutte le configurazioni valutate e inizia con la matrice ridotta
    var L: List[( Map[(String, String), Float], Float, Int, List[(String, String)])] = List()
    L = L :+ (matrix, lb, 0, List())


    while (L.nonEmpty) {

      // da L si prende la configurazione con il LB più basso
      val (matrix, lb, n_archi, lista_archi)= L.sortWith(_._2 < _._2).head
      L = L.drop(1)

      // se è stata nella configurazione attuale abbiamo incluso tutti gli archi abbiamo trovato la soluzione ottima
      if(n_archi==nodes.size) {
        val map_archi = lista_archi.toMap
        var tmp = nodes.head
        map_archi.foreach(x => {
          print("("+map_archi(tmp) + ") ")
          tmp=map_archi(tmp)
        })
        print("("+map_archi(tmp) + ") ")
        println("Cost: "+ lb)
        return
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
        L = L :+ (matrix3, lb3, n_archi, lista_archi)
        val (matrix4, lb4) = includiArco(matrix, arco, lb)
        L = L :+ (matrix4, lb4, n_archi + 1, arco :: lista_archi)
      }
    }

  }

}
