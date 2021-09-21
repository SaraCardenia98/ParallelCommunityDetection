# ParallelCommunityDetection


## Abstract
L'obiettivo del progetto è quello di implementare un algoritmo distribuito per l'individuazione delle comunità all'interno di una rete. 
Per farlo abbiamo implementato in Spark la versione distribuita che utilizza il paradigma MapReduce dell'algoritmo di Girvan–Newman, chiamato SPB-MRA (Shortest Path Betweenness Map Reduce Algorithm). Lo svolgimento del progetto fa riferimento all'articolo *"Parallel community detection on large graphs with MapReduce and GraphChi"* scritto da S. Moon, J. G. Lee, M. Kang, M. Choy, J. W. Lee.
Il linguaggio di programmazione utilizzato è Java. Come DBMS di supporto, per la memorizzazione e visualizzazione del grafo, viene utilizzato Neo4j.


## I dati
Fonte: Network Repository
Link: https://networkrepository.com/email-enron-only.php
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
| `TupleAggiornate.java` | classe che distingue le tuple con o senza nodi adiacenti al nodo target |
| `EdgeBetweenness.java` | classe utilizzata per calcolare la "edge betweenness" di ciascun arco |
| `EliminaAdiacenti.java` | classe che taglia l'arco selezionato nello Stage 3 |
| `ArchiFinali.java` | classe che restituisce gli archi del grafo finale |
| `StampaTuple.java` | classe utilizzata per stampare correttamente oggetti di tipo "Tuple" |
| `GrafoIniziale.java` | classe main utilizzata per la creazione del grafo iniziale su Neo4j |

