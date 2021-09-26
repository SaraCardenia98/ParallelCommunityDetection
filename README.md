# ParallelCommunityDetection


## Abstract

L'obiettivo del progetto è quello di implementare un algoritmo distribuito per l'individuazione delle comunità all'interno di una rete. 
Per farlo abbiamo implementato in Spark la versione distribuita che utilizza il paradigma MapReduce dell'algoritmo di Girvan–Newman, chiamato SPB-MRA (Shortest Path Betweenness Map Reduce Algorithm). Lo svolgimento del progetto fa riferimento all'articolo *"Parallel community detection on large graphs with MapReduce and GraphChi"* scritto da S. Moon, J. G. Lee, M. Kang, M. Choy, J. W. Lee.
Il linguaggio di programmazione utilizzato è Java. Come DBMS di supporto, per la memorizzazione e visualizzazione del grafo, viene utilizzato Neo4j.


## Il dataset

Fonte: Network Repository \
Link: https://networkrepository.com/email-enron-only.php \
Il grafo orientato è costituito da 143 nodi e 623 archi e rappresenta la corrispondenza email tra una rete di utenti.
I file dei dati caricati nella cartella del progetto sono i seguenti:
- archi143.csv
- nodi143.csv


## I file .java

| Classe        | Descrizione           |
|:---------- |:------------- |
| `ParallelCommunityDetection.java` | classe main |
| `Tuple.java` | classe utilizzata per la creazione degli oggetti di tipo "Tuple" |
| `NuoveTuple.java` | classe che implementa la fase Map del primo Stage |
| `EdgeBetweenness.java` | classe utilizzata per calcolare la "edge betweenness" di ciascun arco |
| `ArchiFinali.java` | classe che restituisce gli archi del grafo finale |
| `StampaTuple.java` | classe utilizzata per stampare correttamente oggetti di tipo "Tuple" |
| `GrafoIniziale.java` | classe main utilizzata per la creazione del grafo iniziale su Neo4j |


## Descrizione dell'algoritmo

L'algoritmo prevede 4 Stages. 
Lavora con tuple costituite da 7 attributi:
- targetId: è il nodo destinazione di uno shortest path ed è inizializzato con il sourceId.
- sourceId: è il nodo origine di uno shortest path ed è inizializzato con il targetId.
- distance: indica la lunghezza di uno shortest path ed è inizializzata con 0. Il valore viene incrementato di 1 ad ogni iterazione dello Stage 1
- status: può assumere i valori “active” o “inactive”. Se è “inactive”, lo shortest path per una determinata coppia di nodi è già stato trovato.
- weight: indica il numero di shortest paths con la stessa coppia di nodi sourceId e targetId. Viene inizializzato con il valore 1.
- pathInfo: indica la lista di vertici di uno shortest path. Inizialmente assume il valore null.
- adjList: indica la lista di nodi adiacenti al nodo target.


### Importazione dei dati 

La nostra applicazione legge il file `.csv` contenente tutti gli archi del grafo, creando una `JavaRDD<String> dArchi`. Tramite una operazione di `.mapToPair` su dArchi, crea una `JavaPairRDD<String, String> dArchi2` con in chiave il nodo origine e in valore il nodo destinazione. Con la `.reduceByKey` vengono concatenati i nodi destinazione di ciascun nodo, ottenendo una `JavaPairRDD<String, String> dAdiacenti`. Per facilitare le operazioni successive, abbiamo deciso di trattenere le informazioni in `dAdiacenti` in una variabile locale attraverso una `HashMap<String, String> adiacenze`.

### Stage 1

Le tuple iniziali, create tramite la classe `Tuple.java`, sono pari al numero di nodi del grafo e vengono inizializzate come descritto sopra.
L'obiettivo di questa fase è di calcolare gli shortest paths tra ciascuna coppia di nodi del grafo.

- Fase Map: per ciascuna tupla in input, se lo status è "inactive" non è necessaria alcuna operazione; se lo status è "active" esso viene cambiato in "inactive", viene aggiunto 1 alla distanza e il nodo target viene aggiunto al pathInfo. 
In aggiunta, vengono generate nuove tuple che hanno come targetId ciascuno dei nodi adiacenti al sourceId. Per queste nuove tuple lo status ha valore "active", la adjList viene aggiornata inserendo i nodi adiacenti al nuovo nodo target e gli altri valori sono gli stessi della prima tupla generata in questa fase. 
Per la creazione di tali tuple viene utilizzata la classe `NuoveTuple.java` che prende in input la variabile locale `adiacenze`.

- Fase Reduce: tra le tuple con la stessa coppia targetId, sourceId, rimangono solo quelle con distanza minima. Se più tuple hanno la stessa distanza, il peso diventa pari al numero di tuple che condividono lo stesso minimo. 
L'applicazione gestisce quest'ultimo caso creando inizialmente un'unica tupla che contiene le informazioni di tutte le tuple con stessa distanza separate da ":". Vengono poi create le tuple definitive tramite la funzione `.flatMapToPair`, andando a recuperare le informazioni salvate prima.

Questo Stage viene ripetuto fino a che lo status di tutte le tuple è "inactive".


### Stage 2

In questa fase, si calcola la edge betweenness di tutti gli archi del grafo. Per fare ciò viene utilizzata la classe `EdgeBetweenness.java`. 

- Fase Map: si crea una tupla per ciascun arco del percorso e le assegna come valore il reciproco del peso.

- Fase Reduce: si sommano i pesi delle tuple con la stessa chiave.


### Stage 3

Viene selezionato l'arco con la maggiore edge betweennes.

- Fase Map: si aggiunge valore 1 per ciascuna tupla come chiave.

- Fase Reduce: si seleziona la tupla con la maggiore edge betweennes.


### Stage 4

L'arco selezionato nello Stage 3 viene rimosso, tramite una operazione di `.filter`, rimuovendolo da `dArchi2`. Si aggiornano sia la `JavaPairRDD` che la `HashMap` contenenti la lista di adiacenze.


## Neo4j

Essendo l'applicazione costruita per individuare comunità all'interno di una rete, abbiamo ritenuto particolarmente adatto al nostro caso rappresentare il grafo iniziale e quello finale utilizzando il DBMS Neo4j. 
La creazione del grafo iniziale su Neo4j avviene attraverso la classe `GrafoIniziale.java`, mentre quella del grafo finale avviene direttamente nel main di `ParallelCommunityDetection.java`. 
