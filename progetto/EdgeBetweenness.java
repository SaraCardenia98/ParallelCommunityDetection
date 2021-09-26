package progetto;

import java.util.ArrayList;

import java.util.Iterator;
import java.util.List;

import org.apache.spark.api.java.function.PairFlatMapFunction;

import scala.Tuple2;

/*
 * Questa classe calcola la "edge betweenness" di tutti gli archi
 */

public class EdgeBetweenness implements PairFlatMapFunction<Tuple2<String, Tuple>, String, Double> {

	@Override
	public Iterator<Tuple2<String, Double>> call(Tuple2<String, Tuple> t) throws Exception {
		
		List<Tuple2<String, Double>> output = new ArrayList<Tuple2<String, Double>>();
		
		String[] path = t._2.getPath().split(",");
		int pesoI = t._2.getWeight();
		double pesoD = pesoI;
		
		for(int i = 1; i < (path.length-1); i++) {
			
			output.add(new Tuple2<String, Double> (path[i]+","+path[i+1], 1/(pesoD)));
		}
		
		return output.iterator();
	}
	
	

}
