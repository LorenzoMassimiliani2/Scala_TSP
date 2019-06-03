
import java.io.{BufferedWriter, File, FileWriter}

  class GenerateData {

    // funzione che crea un file contenente le distanze tra n_places citta

    def main(n_places:Int) {

      val filename = "src/main/data/dataPlace.csv"
      val mapfilename = "src/main/data/sf12010placename.csv"

      val dataMap = scala.io.Source.fromFile(mapfilename, "ISO-8859-1").getLines()
      var mapname:Map[String, String] = Map("1"->"2")

      for (line <-dataMap.drop(1)) {
        val elements = line.replace("\"", "").split(",")
        mapname = mapname +  (elements(0)+elements(2) -> elements(3))
      }

      val data =  scala.io.Source.fromFile(filename).getLines()
      val file = new File("src/main/data/data.csv")
      val bw = new BufferedWriter(new FileWriter(file))
      var places = Set[String]()

      for (line <- data.drop(1)) {
        val elements = line.replace("\"", "").split(",")

        if (places.contains(elements(0) + elements(1)) && places.contains(elements(3) + elements(4))) {
          bw.write(mapname(elements(0) + elements(1))+ ","+ mapname(elements(3) + elements(4)) ++","+ elements(2)   + "\n")
        }
        else if (places.size < n_places - 1) {
          places = places + (elements(0) + elements(1))
          places = places + (elements(3) + elements(4))
          bw.write(mapname(elements(0) + elements(1))+ ","+ mapname (elements(3) + elements(4)) ++","+ elements(2)+ "\n")
        }
        else if (places.size < n_places  && (places.contains(elements(0) + elements(1)) || places.contains(elements(3) + elements(4)))  ) {
          places = places + (elements(0) + elements(1))
          places = places + (elements(3) + elements(4))
          bw.write(mapname(elements(0) + elements(1))+ ","+ mapname(elements(3) + elements(4)) +","+ elements(2)+ "\n")
        }
      }
      bw.close()
      return
    }

}
