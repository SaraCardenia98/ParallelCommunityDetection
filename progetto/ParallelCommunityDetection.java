package progetto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.neo4j.driver.AuthToken;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import scala.Tuple2;

public class ParallelCommunityDetection {

	public static void main(String[] args) {

		Logger.getLogger("org").setLevel(Level.ERROR);
		Logger.getLogger("akka").setLevel(Level.ERROR);
		SparkConf sc = new SparkConf();
		sc.setAppName("Parallel Community Detection");
		sc.setMaster("local[*]");
		JavaSparkContext jsc = new JavaSparkContext(sc);
		
		/*
		 * Lettura del file degli archi
		 */
		JavaRDD<String> dArchi = jsc.textFile("data/archi143.csv");

		/*
		 * Creazione della JavaPairRDD che associa a ciascun nodeId la sua lista di adiacenza
		 */
		JavaPairRDD<String, String> dArchi2 = dArchi
				.mapToPair(x -> new Tuple2<String, String>(x.split(";")[0], x.split(";")[1]));
		JavaPairRDD<String, String> dAdiacenti = dArchi2.reduceByKey((x, y) -> x + "," + y);
		JavaRDD<String> dAdiacenti2 = dAdiacenti.map(x -> x._1 + ";" + x._2);
		
		/*
		 * Creazione HashMap che associa a ciascun nodeId la sua lista di adiacenza
		 */
		Map<String, String> map =  dAdiacenti.collectAsMap();
		HashMap<String, String> adiacenze = new HashMap<String, String>(map); 
		
		int k = 300; // Numero di iterazioni dell'algoritmo

		for (int i = 1; i <= k; i++) {
			
			System.out.println("\nIterazione " + i);
			
			/*
			 * Creazione delle tuple iniziali 
			 */
			JavaPairRDD<String, Tuple> dTuple = dAdiacenti2.mapToPair(x -> new Tuple2<String, Tuple>
				(x.split(";")[0] + "," + x.split(";")[0], new Tuple(0, "active", 1, "", x.split(";")[1])));

			/*JavaPairRDD<String, String> stampa = dTuple.mapToPair(new StampaTuple());
			System.out.println("Tuple iniziali");
			System.out.println(stampa.take(50));*/
			
			/*
			 * STAGE 1 Questa parte di codice va ripetuta fino a quando 
			 * tutte le tuple hanno status "inactive"
			 */

			boolean controllo = true; // Controlla se tutte le tuple hanno status "inactive"

			while (controllo) {
				
				/*
				 * Separiamo le tuple attive da quelle inattive
				 */
				JavaPairRDD<String, Tuple> dTupleA = dTuple.filter(x -> x._2.getStatus().equals("active"))
						.filter(x -> x._2.getAdj() != null);
				JavaPairRDD<String, Tuple> dTupleI = dTuple.filter(x -> x._2.getStatus().equals("inactive"));

				/*
				 * Fase di MAP
				 */
				dTupleA = dTupleA.flatMapToPair(new NuoveTuple(adiacenze));
				dTuple = dTupleA.union(dTupleI);

				/*
				 * Fase di REDUCE
				 */
				dTuple = dTuple.reduceByKey((x, y) -> {

					Tuple nuoveTuple = new Tuple();

					if (x.getDistance() < y.getDistance()) {
						return new Tuple(x.getDistance(), x.getStatus(), x.getWeight(), x.getPath(), x.getAdj());
					} else if (x.getDistance() == y.getDistance()) {
						nuoveTuple = new Tuple(x.getDistance(), x.getStatus() + ":" + y.getStatus(),
								x.getWeight() + y.getWeight(), x.getPath() + ":" + y.getPath(), x.getAdj());
						return nuoveTuple;
					} else {
						return new Tuple(y.getDistance(), y.getStatus(), y.getWeight(), y.getPath(), y.getAdj());
					}
				});
				
				dTuple = dTuple.flatMapToPair(x -> {
					List<Tuple2<String, Tuple>> nuoveTuple = new ArrayList<Tuple2<String, Tuple>>();
					if (x._2.getPath().contains(":")) {
						String[] percorso = x._2.getPath().split(":");
						String[] status = x._2.getStatus().split(":");
						
						for(int q = 0; q < status.length; q++) {
							nuoveTuple.add(new Tuple2<String, Tuple>(x._1, new Tuple(x._2.getDistance(), status[q],
								x._2.getWeight(), percorso[q], x._2.getAdj())));
						}		
					} else {
						nuoveTuple.add(x);
					}
					return nuoveTuple.iterator();
				});

				JavaPairRDD<String, Integer> status = dTuple
						.mapToPair(x -> new Tuple2<String, Integer>(x._2.getStatus(), 1)).reduceByKey((x, y) -> x + y);

				/*
				 * Se status ha una sola riga, le tuple sono tutte inattive e lo STAGE 1 termina
				 */
				if (status.count() == 1) {
					controllo = false;
				}

			}

			/*System.out.println("Tuple finali");
			JavaPairRDD<String, String> stampa2 = dTuple.mapToPair(new StampaTuple());
			System.out.println(stampa2.take(50));*/

			/*
			 * STAGE 2
			 */

			/*
			 * Fase di MAP
			 */
			JavaPairRDD<String, Double> dEdgeBetweenness = dTuple.flatMapToPair(new EdgeBetweenness());

			/*
			 * Fase di REDUCE
			 */
			JavaPairRDD<String, Double> dEdgeBetweenness2 = dEdgeBetweenness.reduceByKey((x, y) -> x + y);
		
			/*
			 * STAGE 3
			 */
			JavaPairRDD<Integer, Tuple2<String, Double>> dEdgeBetweenness3 = dEdgeBetweenness2
					.mapToPair(x -> new Tuple2<Integer, Tuple2<String, Double>>(1, x));

			JavaPairRDD<Integer, Tuple2<String, Double>> dMaxEdgeBetweenness = dEdgeBetweenness3.reduceByKey((x, y) -> {
				if (x._2 > y._2) {
					return x;
				} else if (x._2 == y._2) {
					return x;
				} else {
					return y;
				}
			});

			/*
			 * STAGE 4: eliminiamo l'arco selezionato nello STAGE 3
			 */
			JavaPairRDD<String, String> dArcoDaTagliare = dMaxEdgeBetweenness
					.mapToPair(x -> new Tuple2<String, String>(x._2._1.split(",")[0], x._2._1.split(",")[1]));

			//System.out.println("Arco da tagliare: " + dArcoDaTagliare.take(1));
			
			String source = dArcoDaTagliare.keys().take(1).get(0);
			String target = dArcoDaTagliare.values().take(1).get(0);
			
			dArchi2 = dArchi2.filter(x -> !(x._1.equals(source) && x._2.equals(target)));
			dAdiacenti = dArchi2.reduceByKey((x, y) -> x + "," + y);
			dAdiacenti2 = dAdiacenti.map(x -> x._1 + ";" + x._2);
			
			adiacenze.remove(source);
			JavaPairRDD<String, String> nuoviAdj = dAdiacenti.filter(x -> x._1.equals(source));
			List<String> nuoviAdj2 = nuoviAdj.values().take(1);
			if(!(nuoviAdj2.isEmpty())) {
				adiacenze.put(source, nuoviAdj2.get(0));
			}
			
		}
		
		JavaPairRDD<String, String> dArchiFinali = dAdiacenti2.flatMapToPair(new ArchiFinali());
		
		/*
		 * Creazione grafo finale su neo4j
		 */
		
		String uri = "bolt://localhost:7687";
		AuthToken token = AuthTokens.basic("neo4j", "fine");		
		Driver driver = GraphDatabase.driver(uri, token);
		Session s = driver.session();
		
		String query = "MATCH (n) DETACH DELETE n";
		Result result = s.run(query);
		
		String query2 = "load csv from 'file:///nodi143.csv' as line fieldterminator ';' create (:Nodo {id: line[0]})";
		Result result2 = s.run(query2);
		
		List<Tuple2<String, String>> grafo = dArchiFinali.collect();
		for(Tuple2<String, String> n: grafo) {
			String from = n._1;
			String to = n._2;
			String cql = "match (n1:Nodo {id: '" + from + "'}),(n2:Nodo {id: '" + to + "'}) create (n1)-[:ARCO]->(n2)";
			s.run(cql);
		}
		System.out.println("Grafo creato");
		s.close();
		
		Scanner scan;
		scan = new Scanner(System.in);
		scan.next();

	}
}
