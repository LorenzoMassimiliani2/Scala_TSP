import com.sun.org.apache.bcel.internal.classfile.Node
import javax.validation.constraints.Null

import scala.io.Source
import org.apache.log4j.{Level, Logger}
import org.apache.spark.SparkContext

import scala.collection.immutable.HashSet

class TSP {

  var nodes: Set[String] = Set()



  // funzione che riduce la matrice e calcola il nuovo LB
  // distance = matrice (nodo-nodo) -> valore dell'arco
  // oldLb = lower bound prima della riduzione
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

    // Int.MaxValue rappresenta il valore infinito
    matrix = matrix.updated((arco._1, arco._2), Int.MaxValue)

    (reduce(matrix, oldLb), arco)
  }






  // prende in input la matrice, l'arco da includere e il vecchio valore del Lb e ritorna
  // il valore ridotto della matrice e il nuovo Lb
  private def includiArco(distance:Map[(String, String), Float], arco: (String, String), oldLb:Float, listaArchi: List[(String, String)] ) = {

    var matrix = distance

    // lb viene aggiornato aggiungendo il costo nel nodo che si vuole includere
    val lb = oldLb + matrix(arco._1, arco._2)

    // si cercano gli archi che non possono essere più parte della soluzione
    matrix = matrix.filterKeys(x=> (x._2 != arco._2) && (x._1 != arco._1))

    // Viene portato a infinito il valore degli archi che devono essere eliminati
    if(matrix.contains(arco._2, arco._1)) matrix = matrix.updated((arco._2, arco._1), Int.MaxValue)

    // Si guarda se gli archi inclusi formano un ciclo prima che siano stati inclusi tutti i nodi

    var head = arco._1
    var map = (arco::listaArchi).toMap
    var i = 0
    var cicle = false
    var node = map(head)

    while (i<map.size) {

      // se siamo tornati all'head significa che c'è un ciclo
      if(node==head) cicle = true

      // altrimenti viene aggiornato il valore che itera
      else node = if( map.contains(node) ) map(node) else node

      i=i+1
    }

    // indichiamo che abbiamo trovato un ciclo senza aver trovato una soluzione restituendo -1 come LB
    if(cicle &&  listaArchi.size < nodes.size-1)  (matrix, -1.toFloat)

    // altrimenti restituiamo la matrice modificata ridotta
    else reduce(matrix, lb)
  }








  def main(): Unit = {


    val filename = "src/main/data/data.csv"
    var distance: Map[(String, String), Float] = Map ()   //map (nodo1, nodo2) = costo

    val data =  scala.io.Source.fromFile(filename).getLines()


    // a partire dal file contenente le distanze vengono create due variabili
    // distance: matrice delle distanze nodi-nodi
    // nodes: lista dei nodi
    for (line <- data) {
      val elements = line.replace("\"", "").split(",")
      distance += (elements(0), elements(1)) -> elements(2).toFloat
      nodes = nodes + elements(0)
      nodes = nodes + elements(1)


    }


    // Le distanze tra un nodo e se stesso sono impostate a infinito
    nodes.foreach(x => {distance += ((x,x) -> Int.MaxValue) })

    // prima riduzione della matrice
    val(matrix, lb) = reduce(distance, 0)

    // L è una lista che conterrà tutte le configurazioni valutate e inizia con la matrice ridotta
    var L: List[( Map[(String, String), Float], Float, Int, List[(String, String)] )] = List()
    L = L :+ (matrix, lb, 0, List())


    while (L.nonEmpty) {


      // da L si prende la configurazione con il LB più basso
      val (matrix, lb, n_archi, lista_archi)= L.sortWith(_._2 < _._2).head
      L = L.drop(1)

      // se è stata nella configurazione attuale abbiamo incluso tutti gli archi abbiamo trovato la soluzione ottima
      if(n_archi==nodes.size) {
        lista_archi.foreach(println(_))
        println(matrix)
        println("cost:" + lb)
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
        var ((matrix3, lb3), _) = escludiArco(matrix, arco, lb)
        if(lb==lb3) lb3 = lb3
        L = L :+ (matrix3, lb3, n_archi, lista_archi)
        val (matrix4, lb4) = includiArco(matrix, arco, lb, lista_archi)
        if(lb4!= -1.toFloat) L = L :+ (matrix4, lb4, n_archi + 1, arco :: lista_archi)

      }
    }

  }

}
