## Implementazione del Travelling Salesman Problem in Scala e Spark

Il progetto è stato realizzato per implementare l'algoritmo per la risoluzione del problema del
commesso viaggiatore con tecnica branch & bound.

L'algoritmo è stato scritto in linguaggio Scala, utilizzando la libreria Scala-Spark per
la parallelizzazione di alcune procedure.

I run sono stati effettuati sulla piattaforma AWS importando il progetto in formato .jar.
Successivamente sono stati analizzati i tempi di esecuzione al variare della quantità dei dati
in input e del numero dei core.

#### Struttura del progetto
* **_src/main/_**
    * **_data/_**
        * **_dataPlace.csv_**: database contenente le distanze tra alcune delle città degli USA (rappresentate con id numerici);
        * **_sf12010placename.csv_**: mappa (id_città -> nome città);
        * **_data.csv_**: database generato dal programma che considera solo alcune città e le distanze tra tutte le coppie di esse;
    * **_scala/_**
        * _**GenerateData.scala**_: codice per la generazione di data.csv;
        * **_Main.scala_**: funzione principale che utilizza GenerateData.scala e TSP_SPARK.scala;
        * **_TSP.scala_**: implementazione sequenziale con tecnica branch & bound per TSP;
        * **_TSP_SPARK.scala_**: implementazione parallela con tecnica branch & bound per TSP;
* **_PresentazioneSCPMassimilianiVainigli.pdf_**: presentazione del progetto e dei risultati.