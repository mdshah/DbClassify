import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

import org.apache.commons.codec.binary.Base64;

import JSONParser.JSONArray;
import JSONParser.JSONObject;

/** 
 * BingQuery class is used to retrieve data from the Bing API using the give accountKey and the query. 
 * Relevance feedback is used on the first query result to modify the given query and retrieve data again. 
 * The user again marks documents as relevant and non-relevant. This process is repeated till the desired precision 
 * is reached.  
 * @author Manushi Shah
 * @version 10-20-2012
 */
public class DBClassify {

	//default account key to use 
	private static String ACCOUNTKEY = "5+LYA3sb/FaaLPEiAxQyzc884SBOo7c3284SSItRwZI=";

	//default precision and query values
	private double spec_thres;
	private double cov_thres;
	private String host;
	private String bingUrl;
	private String classification;
	private Node root; 
	private HashMap<String, double[]> wordFreq;
	private HashMap<String, Node> nodes;
	BufferedWriter out;

	/**
	 * Main method that retrieves the documents and starts relevance feedback for further improvement
	 * @param args command line arguments - 0 - AccountKey, 1 - precision, 2 - query 
	 */
	public static void main(String[] args) {

		double spec_thres = 0.6;
		double cov_thres = 100; 
		String host = "fifa.com";

		if(args.length > 0)
			ACCOUNTKEY = args[0];
		if(args.length > 1)
			spec_thres = Double.parseDouble(args[1]);
		if(args.length > 2)
			cov_thres = Double.parseDouble(args[2]);
		if(args.length > 3)
			host = args[3];

		DBClassify bq = new DBClassify(spec_thres, cov_thres, host);
		bq.startQuery(); 
	}

	/**
	 * Constructor to create BingQuery instance 
	 * Initializes all data structures and default values to be able to use further. 
	 * @param query - initial query that user passes
	 * @param precision - user desired precision 
	 */
	public DBClassify(double spec_thres, double cov_thres, String host) {
		this.spec_thres = spec_thres;
		this.cov_thres = cov_thres;
		this.wordFreq = new HashMap<String, double[]>();
		this.nodes = new HashMap<String, Node>();
		this.host = host;
		//this.bingUrl = "https://api.datamarket.azure.com/Data.ashx/Bing/SearchWeb/v1/Web?Query=%27site%3a"+host+"%20"+query+"%27&$top=10&$format=JSON";

		//initialize writing output to transcript.text, with appending set to true.
		try {
			out = new BufferedWriter(new FileWriter("transcript.txt", true));
		} catch (IOException e) {
			e.printStackTrace();
		}
		createCategories();
		createQueryList();
	}

	private void createCategories() {

		root = new Node(0.0, 0, "Root");
		Node computers = new Node(0.0, 0, "Computers");
		Node hardware = new Node(0.0, 0, "Hardware");
		Node programming = new Node(0.0, 0, "Programming");
		Node health = new Node(0.0, 0, "Health");
		Node fitness = new Node(0.0, 0, "Fitness");
		Node diseases = new Node(0.0, 0, "Diseases");
		Node sports = new Node(0.0, 0, "Sports");
		Node basketball = new Node(0.0, 0, "Basketball");
		Node soccer = new Node(0.0, 0, "Soccer");

		computers.children.add(hardware);
		computers.children.add(programming);
		root.children.add(computers);

		health.children.add(fitness);
		health.children.add(diseases);
		root.children.add(health);

		sports.children.add(basketball);
		sports.children.add(soccer);
		root.children.add(sports);

		nodes.put("Root", root);
		nodes.put("Computers", computers);		
		nodes.put("Health", health);
		nodes.put("Sports", sports);
		nodes.put("Hardware", hardware);		
		nodes.put("Programming", programming);		
		nodes.put("Fitness", fitness);
		nodes.put("Diseases", diseases);
		nodes.put("Basketball", basketball);		
		nodes.put("Soccer", soccer);		
	}

	private void createQueryList() {
		try {

			FileReader file = new FileReader("queries.txt");
			BufferedReader in = new BufferedReader(file);
			String line;

			while ((line = in.readLine()) != null) {
				String[] words = line.split("\\s+");
				if (words.length == 0) continue;
				if (nodes.containsKey(words[0])) {
					String prober = "";
					for (int i = 1; i < words.length; i++) {
						prober += words[i];
						if(i != words.length - 1) prober += " ";
					}
					nodes.get(words[0]).addProbe(prober);
				}
			}

			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("");
	}

	public void startQuery() {
		System.out.println("Classifying... ");
		String classification = classify(root);		
		System.out.println("Classification: ");
		System.out.println(classification);
	}

	/**
	 * Print header at the beginning of each loop of relevance feedback 
	 */
	public void printHeader(Node N) {
		for(Node n : N.children) {
			System.out.println("Specificity for category:" + n.getName() + " is " + n.getSpec());
			System.out.println("Coverage for category:" + n.getName() + " is " + n.getCov());
		}
	}

	public String classify(Node curCategory) {
		String Result = "";
		if(curCategory.children.size() < 1)
			return curCategory.getName();
		
		int totalDocs = 0;
		for(Node n : curCategory.children) {
			int coverage = 0; 
			for(String s : n.getProbe()) {
				int docs = retrieveResults(formatQuery(s));
				totalDocs += docs;
				coverage += docs;
			}
			n.setCov(coverage);
		}
		
		for(Node n : curCategory.children) { 
			double specificity = curCategory.getSpec() * n.getCov() * 1.0 /totalDocs;
			n.setSpec(specificity);
		}
		
		printHeader(curCategory);
		for(Node n : curCategory.children) {
			if(n.getSpec() >= spec_thres && n.getCov() >= cov_thres)
				Result += curCategory.getName() + "/" + classify(n);
		}		
		
		if (Result.equals("")) return curCategory.getName();
		else return Result; 
	}

	/**
	 * Query the bing API for retrieving top most documents for current query. 
	 */
	public int retrieveResults(String query) {
		int numberOfMatches = 0; 

		try {
			this.bingUrl = "https://api.datamarket.azure.com/Data.ashx/Bing/SearchWeb/v1/Composite?Query=%27site%3a"+host+"%20"+query+"%27&$top=10&$format=JSON";

			byte[] accountKeyBytes = Base64.encodeBase64((ACCOUNTKEY + ":" + ACCOUNTKEY).getBytes());
			String accountKeyEnc = new String(accountKeyBytes);

			URL url = new URL(bingUrl);
			URLConnection urlConnection = url.openConnection();
			urlConnection.setRequestProperty("Authorization", "Basic " + accountKeyEnc);

			InputStream inputStream = (InputStream) urlConnection.getContent();		
			BufferedReader in = new BufferedReader(new InputStreamReader(inputStream,"UTF-8"));
			String inputLine;
			StringBuffer output=new StringBuffer("");
			while ((inputLine = in.readLine()) != null)
				output.append((inputLine));
			in.close();

			String json = output.toString();
			JSONObject jo = new JSONObject(json);	
			jo = jo.getJSONObject("d");
			JSONArray ja = jo.getJSONArray("results");
			JSONObject resultObject = ja.getJSONObject(0);
			String hitsString= resultObject.get("WebTotal").toString();
			numberOfMatches = java.lang.Integer.parseInt(hitsString);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return numberOfMatches;
	}

	public void writeResults(){
		try {
			for(String word : wordFreq.keySet()) {			
				out.write(word + "#" + wordFreq.get(word)[0] + "#" + wordFreq.get(word)[1]);
			}
			out.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}

	/**
	 * For each round, print the results, ask user for input, output the results to transcript file.
	 * @param jo The result object retrieved from BingAPI. 
	 */
	public void printResults(JSONObject jo) {
		try {
			jo = jo.getJSONObject("d");
			JSONArray ja = jo.getJSONArray("results");
			
			System.out.println("Extracting topic content summaries... ");
			System.out.println("Creating Content Summaries for:" + classification + "... ");
			System.out.println(" ");
			//System.out.println(output);

			for (int i = 1; i <= ja.length(); i++)
			{
				System.out.println("Getting Page: ");
				JSONObject resultObject = ja.getJSONObject(i-1);
				System.out.println(resultObject.get("Url"));
				System.out.println();
			}	
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Fortmat the query to put in the URL, replace " " with %20. 
	 * @param query2 original query 
	 * @return formatted query 
	 */
	private String formatQuery(String query2) {
		String queryTemp = "";
		String[] queryWords = query2.split(" ");
		if(queryWords.length > 1) {
			for(int i = 0; i < queryWords.length - 1; i++) {
				queryTemp += queryWords[i] + "+";
			}
			queryTemp += queryWords[queryWords.length - 1];
		}
		else queryTemp = query2;
		return queryTemp;
	}
}