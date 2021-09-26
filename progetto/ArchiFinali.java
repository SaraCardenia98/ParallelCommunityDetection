package progetto;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.spark.api.java.function.PairFlatMapFunction;

import scala.Tuple2;

public class ArchiFinali implements PairFlatMapFunction<String, String, String> {

	@Override
	public Iterator<Tuple2<String, String>> call(String s) throws Exception {

		List<Tuple2<String, String>> output = new ArrayList<Tuple2<String, String>>();

		if (s.split(";").length > 1) {
			String[] adiacenti = s.split(";")[1].split(",");

			for (int i = 0; i < adiacenti.length; i++) {

				output.add(new Tuple2<String, String>(s.split(";")[0], adiacenti[i]));
			}
		}

		return output.iterator();
	}
}