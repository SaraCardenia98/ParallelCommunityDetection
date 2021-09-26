package progetto;

import org.apache.spark.api.java.function.PairFunction;

import scala.Tuple2;

public class StampaTuple implements PairFunction<Tuple2<String, Tuple>, String, String> {

	@Override
	public Tuple2<String, String> call(Tuple2<String, Tuple> t) throws Exception {
		
		return new Tuple2<String, String>(t._1, "<" + t._2.getDistance()+";"+t._2.getStatus()+";"+t._2.getWeight()+";"+t._2.getPath()+";"+t._2.getAdj() + ">");
			
		
	}

}
