import java.io.{BufferedWriter, File, FileWriter}

class GenerateData {

  /* Genera circa n_states * n_places nodi con (n_states * n_places)^2 archi
   */

  val n_states = 10
  val n_places = 10

  val file = new File("src/main/data.csv")
  val bw = new BufferedWriter(new FileWriter(file))
  bw.write("\"state1\",\"place1\",\"mi_to_place\",\"state2\",\"place2\"\n")
  for (s1 <- 0 until n_states){
    for (p1 <- 0 until n_places){
      for (s2 <- 0 until n_states) {
        for (p2 <- 0 until n_places) {
          // Escludo gli archi tra un luogo e se stesso
          if (s2 != s1 || p2 != p1){
            bw.write("\"" + s1.toString + "\",\"" + p1.toString + "\",\"" +
              (scala.util.Random.nextInt(10000) + scala.util.Random.nextDouble).toString +
              "\",\"" + s2.toString + "\",\"" + p2.toString + "\"\n")
          }
        }
      }
    }
  }
  bw.close()

}
