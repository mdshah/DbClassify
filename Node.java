import java.util.ArrayList;

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
	
	public Node(double spec, int cov, String name) {
		this.setSpec(spec);
		this.setCov(cov);
		this.setName(name);
		this.children = new ArrayList<Node>();
		this.probes = new ArrayList<String>();
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
}
