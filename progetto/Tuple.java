package progetto;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

public class Tuple implements Serializable{

private String path, adj, status;
private int distance, weight;
	
	public Tuple(int distance, String status, int weight, String path, String adj) {
		this.distance = distance;
		this.status = status;
		this.weight = weight;
		this.path = path;
		this.adj = adj;
	}
	
	public Tuple(int distance, String status, int weight, String path) {
		this.distance = distance;
		this.status = status;
		this.weight = weight;
		this.path = path;
	}
	
	public Tuple() {}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getAdj() {
		return adj;
	}

	public void setAdj(String adj) {
		this.adj = adj;
	}

	public int getDistance() {
		return distance;
	}

	public void setDistance(int distance) {
		this.distance = distance;
	}

	public int getWeight() {
		return weight;
	}

	public void setWeight(int weight) {
		this.weight = weight;
	}
}