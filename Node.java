import java.util.ArrayList;
import java.util.HashSet;

/**
 * @author Manushi Shah
 *
 */
public class Node {
	
	private double spec;
	private int cov;
	private String name;
	ArrayList<Node> children;
	private ArrayList<String> probes;
	private ArrayList<ArrayList<String>> topDocs;
	
	public Node(double spec, int cov, String name) {
		this.setSpec(spec);
		this.setCov(cov);
		this.setName(name);
		this.children = new ArrayList<Node>();
		this.probes = new ArrayList<String>();
		this.topDocs = new ArrayList<ArrayList<String>>();
	}

	public double getSpec() {
		return spec;
	}

	public void setSpec(double spec) {
		this.spec = spec;
	}

	public int getCov() {
		return cov;
	}

	public void setCov(int cov) {
		this.cov = cov;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public void addProbe(String probe) {
		probes.add(probe);
	}
	
	public void setProbe(ArrayList<String> probe) {
		this.probes = probe;
	}
	
	public ArrayList<String> getProbe() {
		return probes;
	}
	
	public void addTopDocs(ArrayList<String> urls) {
			topDocs.add(urls);
	}
	
	public ArrayList<ArrayList<String>> getTopDocs() {
		return topDocs;
	}
}
