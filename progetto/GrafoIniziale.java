package progetto;

import java.util.List;

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

public class GrafoIniziale {

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
		 * Creazione delle tuple iniziali per ciascun nodo del grafo
		 */
		JavaPairRDD<String, String> dArchi2 = dArchi
				.mapToPair(x -> new Tuple2<String, String>(x.split(";")[0], x.split(";")[1]));

		/*
		 * Creazione grafo iniziale su neo4j
		 */

		String uri = "bolt://localhost:7687";
		AuthToken token = AuthTokens.basic("neo4j", "inizio");
		Driver driver = GraphDatabase.driver(uri, token);
		Session s = driver.session();
		
		String query0 = "MATCH (n) DETACH DELETE n";
		Result result0 = s.run(query0);

		String query = "load csv from 'file:///nodi143.csv' as line fieldterminator ';' create (:Nodo {id: line[0]})";
		Result result = s.run(query);

		List<Tuple2<String, String>> grafo = dArchi2.collect();
		for (Tuple2<String, String> n : grafo) {
			String from = n._1;
			String to = n._2;
			String cql = "match (n1:Nodo {id: '" + from + "'}),(n2:Nodo {id: '" + to + "'}) create (n1)-[:ARCO]->(n2)";
			s.run(cql);
		}
		
		System.out.println("Grafo creato");
		s.close();

	}
}
