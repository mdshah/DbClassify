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
	private static String ACCOUNTKEY = "67o8I/cEs9YvBBHN2D25/1U3CUnSiXA746DnSts1d0I=";

	//default precision and query values
	private double spec_thres;
	private double cov_thres;
	private String host;
	private String bingUrl;
	private ArrayList<String> classification;
	private Node root; 
	private HashMap<String, Node> nodes;

	/**
	 * Main method that retrieves the documents and starts relevance feedback for further improvement
	 * @param args command line arguments - 0 - AccountKey, 1 - precision, 2 - query 
	 */
	public static void main(String[] args) {

		double spec_thres = 0.06;
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
		this.nodes = new HashMap<String, Node>();
		this.classification = new ArrayList<String>();
		this.host = host;
		//this.bingUrl = "https://api.datamarket.azure.com/Data.ashx/Bing/SearchWeb/v1/Web?Query=%27site%3a"+host+"%20"+query+"%27&$top=10&$format=JSON";

		createCategories();
		createQueryList();
	}

	private void createCategories() {

		root = new Node(1.0, 0, "Root");
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
		String dbclassification = classify(root);	

		System.out.println();
		System.out.println("Classification: ");

		HashSet<String> allClassification = new HashSet<String>();

		String result = "";
		for(int i = 0; i < classification.size(); i++) {
			if(classification.get(i).equals("Root")) {
				if(!result.equals(""))
					System.out.println(result);
				result = "Root";
			}
			else result += "/" + classification.get(i);	
		}

		if(!result.equals(""))
			System.out.println(result);

		System.out.println();

		for(String s : classification)
			allClassification.add(s);

		System.out.println("Extracting topic content summaries... ");
		processDocs(allClassification);
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
		if(curCategory.children.size() < 1) {
			classification.add(curCategory.getName());
			return curCategory.getName();
		}

		int totalDocs = 0;
		for(Node n : curCategory.children) {
			int coverage = 0; 
			for(String s : n.getProbe()) {
				int docs = retrieveResults(formatQuery(s), n);
				totalDocs += docs;
				coverage += docs;
			}
			n.setCov(coverage);
		}

		for(Node n : curCategory.children) { 
			double specificity = curCategory.getSpec() * n.getCov() * 1.0 / totalDocs;
			n.setSpec(specificity);
		}

		printHeader(curCategory);

		for(Node n : curCategory.children) {
			if(n.getSpec() >= spec_thres && n.getCov() >= cov_thres) {
				classification.add(curCategory.getName());
				Result += curCategory.getName() + "/" + classify(n);
			}
		}		

		if (Result.equals("")) {
			classification.add(curCategory.getName());
			return curCategory.getName();
		}
		else return Result; 
	}

	/**
	 * Query the bing API for retrieving top most documents for current query. 
	 */
	public int retrieveResults(String query, Node n) {
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
			n.addTopDocs(getResults(ja));
			//	System.out.println("Query : " + query);
			JSONObject resultObject = ja.getJSONObject(0);
			String hitsString = resultObject.get("WebTotal").toString();
			numberOfMatches = java.lang.Integer.parseInt(hitsString);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return numberOfMatches;
	}

	/**
	 * For each round, print the results, ask user for input, output the results to transcript file.
	 * @param jo The result object retrieved from BingAPI. 
	 */
	public ArrayList<String> getResults(JSONArray ja) {
		ArrayList<String> topLinks = new ArrayList<String>();
		try {		

			JSONObject result = ja.getJSONObject(0);
			JSONArray resultObject = result.getJSONArray("Web");

			int valid = 0; 
			for (int i = 0; i < resultObject.length(); i++) {
				if(valid > 3)
					continue;

				String url = resultObject.getJSONObject(i).get("Url").toString();
				topLinks.add(url);
				valid++;
			}	
		} catch (Exception e) {
			e.printStackTrace();
		}

		return topLinks;
	}


	public ArrayList<ArrayList<String>> buildDocList(String category) {

		ArrayList<ArrayList<String>> queryDocs = new ArrayList<ArrayList<String>>();
		Node curr = nodes.get(category);
		if(curr != null) {
			for(Node n : curr.children) {
				for(ArrayList<String> cases : n.getTopDocs()) {
					queryDocs.add(cases);
				}
			}
		}

		return queryDocs;
	}

	public void processDocs(HashSet<String> classification) {

		HashMap<String, ArrayList<ArrayList<String>>> allDocs = new HashMap<String, ArrayList<ArrayList<String>>>(); 

		for(String category : classification) {

			ArrayList<ArrayList<String>> categoryList = buildDocList(category);
			Node currNode = nodes.get(category);

			for(Node n : currNode.children) {
				ArrayList<ArrayList<String>> subCategoryList = new ArrayList<ArrayList<String>>();
				if(classification.contains(n.getName())) subCategoryList = buildDocList(n.getName());  
				if(null != subCategoryList) {
					for(ArrayList<String> docs : subCategoryList) {
						categoryList.add(docs);
					}
				}
			}

			allDocs.put(category, categoryList);
			System.out.println(category + " : " + categoryList.size());
		}

		for(String s : allDocs.keySet()) {
			HashSet<String> uniqueDocs = new HashSet<String>();
			HashMap<String, Integer> wordFreq = new HashMap<String, Integer>();
			System.out.println("Creating Content Summary for: " + s);

			int count = 1; 
			for(ArrayList<String> topDocsByQuery : allDocs.get(s)) {
				System.out.println(count + "/" + allDocs.get(s).size());
				for(String url : topDocsByQuery) {
					if(!uniqueDocs.contains(url)) {
						System.out.println("Getting Page: " + url);
						Set<String> currentTerms = getWordsLynx.runLynx(url);
						for(String term : currentTerms) {
							Integer freq = wordFreq.get(term);
							if(freq == null) wordFreq.put(term, 1);
							else wordFreq.put(term, freq + 1);
						}
						System.out.println();
						System.out.println();
						uniqueDocs.add(url);
					}
				}
				count++;
			}
			writeResults(wordFreq, s);
		}
	}

	public void writeResults(HashMap<String, Integer> wordFreq, String query){
		if(wordFreq.size() > 0) {

			List<String> sortedKeys=new ArrayList<String>(wordFreq.keySet());
			Collections.sort(sortedKeys);

			try {
				BufferedWriter out = new BufferedWriter(new FileWriter(query + "-" + host + ".txt", false));
				for(String word : sortedKeys) {	
					double freq = wordFreq.get(word) * 1.0;
					out.write(word + "#" + freq + "\n");
				}
				out.close();

			} catch (IOException e) {
				e.printStackTrace();
			} 
		}
	}

	/**
	 * Fortmat the query to put in the URL, replace " " with +. 
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
