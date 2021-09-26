package progetto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.spark.api.java.function.PairFlatMapFunction;

import scala.Tuple2;

/*
 * Questa classe viene utilizzata per l'implementazione della fase Map del primo stage.
 * Consente di generare per ogni tupla con status "active" delle nuove tuple.
 */

public class NuoveTuple implements PairFlatMapFunction<Tuple2<String, Tuple>, String, Tuple> {

	private HashMap<String, String> adiacenze;

	public NuoveTuple(HashMap<String, String> adiacenze) {
		this.adiacenze = adiacenze;
	}
	
	@Override
	public Iterator<Tuple2<String, Tuple>> call(Tuple2<String, Tuple> t) throws Exception {

		List<Tuple2<String, Tuple>> output = new ArrayList<Tuple2<String, Tuple>>();

		/*
		 * Per ciascuna tupla se ne crea una inattiva in cui la distanza viene
		 * incrementata di 1 e il nodo target è aggiunto al path.
		 */
		output.add(new Tuple2<String, Tuple>(t._1, new Tuple(t._2.getDistance() + 1, "inactive", t._2.getWeight(),
				t._2.getPath() + "," + t._1.split(",")[0], t._2.getAdj())));

		String[] adiacenti = t._2.getAdj().split(",");

		/*
		 * In aggiunta, per ciascuna tupla vengono create tante tuple quanti sono i nodi adiacenti
		 */
		
		for (int i = 0; i < adiacenti.length; i++) {

			output.add(
					new Tuple2<String, Tuple>(adiacenti[i] + "," + t._1.split(",")[1], new Tuple(t._2.getDistance() + 1,
							"active", t._2.getWeight(), t._2.getPath() + "," + t._1.split(",")[0], adiacenze.get(adiacenti[i]))));
		}

		return output.iterator();
	}
}
