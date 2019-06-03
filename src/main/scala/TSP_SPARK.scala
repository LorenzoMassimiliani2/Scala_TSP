import org.apache.log4j.{Level, Logger}
import org.apache.spark
import org.apache.spark.SparkContext




class TSP_SPARK(n_core:Int) extends java.io.Serializable {


  private var nodes: Set[String] = Set()



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
  private def excludeEdge(distance:Map[(String, String), Float], edge: (String, String), oldLb:Float) = {
    var matrix = distance

    // Int.MaxValue rappresenta il valore infinito
    matrix = matrix.updated((edge._1, edge._2), Int.MaxValue)

    (reduce(matrix, oldLb), edge)
  }





  // prende in input la matrice, l'arco da includere e il vecchio valore del Lb e ritorna
  // il valore ridotto della matrice e il nuovo Lb
  private def includeEdge(distance:Map[(String, String), Float], edge: (String, String), oldLb:Float, listaArchi: List[(String, String)] ) = {
    var matrix = distance

    // lb viene aggiornato aggiungendo il costo nel nodo che si vuole includere
    val lb = oldLb + matrix(edge._1, edge._2)

    // si cercano gli archi che non possono essere più parte della soluzione
    matrix = matrix.filterKeys(x=> (x._2 != edge._2) && (x._1 != edge._1))

    // Viene portato a infinito il valore degli archi che devono essere eliminati
    if(matrix.contains(edge._2, edge._1)) matrix = matrix.updated((edge._2, edge._1), Int.MaxValue)

    // Si guarda se gli archi inclusi formano un ciclo prima che siano stati inclusi tutti i nodi

    val head = edge._1
    val map = (edge::listaArchi).toMap
    var i = 0
    var cycle = false
    var node = map(head)

    while (i<map.size) {
      // se siamo tornati all'head significa che c'è un ciclo
      if(node==head) cycle = true
      // altrimenti viene aggiornato il valore che itera
      else node = if( map.contains(node) ) map(node) else node

      i=i+1
    }

    // indichiamo che abbiamo trovato un ciclo senza aver trovato una soluzione restituendo -1 come LB
    if(cycle &&  listaArchi.size < nodes.size-1)    (matrix, -1.toFloat)

    // altrimenti restituiamo la matrice modificata ridotta
    else reduce(matrix, lb)

  }




  // funzione che verrà eseguita in parallelo e che si occupa di cercare l'arco la cui eslusione massimizza il LB
  // e trova i due figli: includi arco e escludi arco
  // matrix: matrice delle distanze nodo-nodo
  // LB: lower bound di partenza
  // n_edges: numero di archi inseriti
  // list_edges: lista di archi già inseriti
  private def findNewConfigs(matrix:Map[(String, String), Float], lb:Float, n_edges:Int, list_edges: List[(String, String)]) = {


    // ExcludeEdges rappresenta una lista (Matrice delle distanze, LB, arco considerato nell'esclusione)
    var ExcludeEdges:List[( (Map[(String, String), Float], Float), (String, String))] = List()

    // Per ogni arco della matrice calcoliamo la nuova configurazione data dell'esclusione di quell'arco
    matrix.foreach(x=> {ExcludeEdges = excludeEdge(matrix, x._1, lb) :: ExcludeEdges})

    // se il numero di configurazioni è 0 siamo in una configurazione non utilizzabile
    if(ExcludeEdges.nonEmpty) {

      // ordiniamo le configurazioni per LB
      ExcludeEdges = ExcludeEdges.sortBy(_._1._2)

      // viene preso l'arco con il LB piu grande
      val ((_, _), arco) = ExcludeEdges.last

      // aggiungiamo alla lista le configurazioni date dall'esclusione dell'arco e della sua inclusione
      val ((matrix3, lb3), _) = excludeEdge(matrix, arco, lb)
      var (matrix4, lb4) = includeEdge(matrix, arco, lb, list_edges)

      if(n_edges+1 == nodes.size) {
       // per indicaree che la matrice ha è gia una soluzione ammissibile
       matrix4 = matrix4 + (("","")->0)
      }

      // ritornano le due configurazioni date dall'eslusione e dall'inclusione del nodo che massimizza il LB
      List ((matrix3, lb3, n_edges, list_edges), (matrix4, lb4, n_edges + 1, arco :: list_edges))
    }
    else
      Nil
  }






  // funzione principale
  def main(): Unit = {

    // file che verrà letto
    val filename = "src/main/data/data.csv"
    var distance: Map[(String, String), Float] = Map ()   //map (nodo1, nodo2) = costo

    val data =  scala.io.Source.fromFile(filename).getLines()

    // a partire dal file contenente le distanze vengono create due variabili
    // distance: matrice delle distanze nodi-nodi
    // nodes: lista dei nodi
    for (line <- data) {
      val elements = line.replace("\"", "").split(",")
      distance += (elements(0), elements(1)) -> elements(2).toFloat
      nodes += elements(0)
      nodes += elements(1)
    }

    // prima riduzione della matrice
    val(matrix, lb) = reduce(distance, 0)



    Logger.getLogger("org").setLevel(Level.ERROR)
    // Create a SparkContext with n_core
    val sc = new SparkContext("local[" + n_core +"]", "TSP_SPARK")



    // list è una lista che conterrà tutte le configurazioni valutate e inizia con la matrice ridotta
    var listConfigs: List[( Map[(String, String), Float], Float, Int, List[(String, String)])] = List() :+ (matrix, lb, 0, List())

    var continue = true

    // finché la lista che contiene le configurazioni non è vuota
    while (continue) {

      // le configurazioni vengono divise in quelle che saranno processate parallelamente (quelle con il LB piu basso)
      // e quelle che veranno ignorate durante questa iterazione
      val configs = listConfigs.sortBy(_._2)

      val (configs_considered, configs_notConsidered) = configs.splitAt(n_core)

      // newRdd conterrà le configurazione che saranno valutate durante questa interazione
      var newRdd =  sc.parallelize(configs_considered)

      // Se sono già stati trovati tutti gli archi è stata trovata la soluzione ottima
      if(configs_considered.exists(x=>x._3==nodes.size )) {

        // trovo la configurazione con il LB piu basso tra quelle che possono essere una soluzione
        val element =  configs_considered.filter(_._3 == nodes.size).minBy(_._2)

        val edges = element._4.toMap
        var head = edges.head._1
        var i = 0
        println()

        // viene fatto un ciclo per stampare il percorso ottimo
        print("PATH: " + head)
        while (i<nodes.size) {
          head = edges(head)
          print(", " + head)
          i = i + 1
        }
        println()

        // stampa del LB
        println("TOTAL MILES: " + element._2)
        return

      }


      // Viene applicata la funzione in modo parallelo e vengono eliminate le configurazioni vuote e quelle non ammissibili
      newRdd = newRdd.flatMap(x => findNewConfigs(x._1, x._2, x._3, x._4)).filter(x=>  x._1.nonEmpty).filter(x=>x._2 != -1.toFloat)

      // I risultati vengono raccolti
      val results = newRdd.collect()

      // vengono unite le nuove configurazioni con quelle non ancora esaminate
      val new_configs = configs_notConsidered.union(results)
      if(new_configs.length==0) continue = false

      // viene creato il nuovo rdd
      listConfigs = new_configs
    }

  }

}
