import java.util.*;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.codec.binary.Base64;

import JSONParser.JSONArray;
import JSONParser.JSONObject; 
import java.util.Hashtable;
import java.util.Enumeration;

public class QProber {
	private double tes;
	private int tec;
	private String website;
	private Hashtable<String, Integer> categories = new Hashtable<String, Integer>();
	private ArrayList<String>[] queries = new ArrayList[10];
	private double[] specificity = new double[10];
	private int[] coverage = new int[10];

	
	public QProber(double tes, int tec, String website) {

			this.tes = tes;
			this.tec = tec;
			this.website=website;
			
		for (int i = 0; i < queries.length; i++)
			queries[i] = new ArrayList<String>();

	}

	void createCategoryList() {
		categories.put("Root", 0);
		categories.put("Computers", 1);
		categories.put("Health", 2);
		categories.put("Sports", 3);
		categories.put("Hardware", 4);
		categories.put("Programming", 5);
		categories.put("Fitness", 6);
		categories.put("Diseases", 7);
		categories.put("Basketball", 8);
		categories.put("Soccer", 9);
		System.out.println("Categories added");
	}
	
	
	void createQueryList() {
		try {

			FileReader file = new FileReader("queries.txt");
			BufferedReader in = new BufferedReader(file);
			String line = "";

			while ((line = in.readLine()) != null) {
				String[] words = line.split("\\s+");
				if (words.length == 0)
					continue;
				if (categories.containsKey(words[0])) {
					int categoryNo = this.categories.get(words[0]).hashCode();
//					System.out.println("Yes, out of the given catgories");
					String prober = "";
					for (int i = 1; i < words.length; i++) {
						if (i > 1)
							prober += " ";
						prober += words[i];
					}
					queries[categoryNo].add(prober);
				}
			}
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		for(int x=0;x<10;x++)
		System.out.println(queries[x]);
	}

	public int retrieveResults(int categoryNo, String query) {
		int numberOfMatches=0;
		
		try {
	String accountKey = "PJD4UwjOC50tzY6BN95L7ftRuQ5EMavSK14aCsiiEjc=";
	String bingUrl = "https://api.datamarket.azure.com/Data.ashx/Bing/SearchWeb/v1/Composite?Query=%27site%3a"+website+"%20"+query+"%27&$top=10&$format=JSON";
//	System.out.println(bingUrl);
	byte[] accountKeyBytes = Base64.encodeBase64((accountKey + ":" + accountKey).getBytes());
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
//	System.out.println(jo+"\n");
	
	JSONArray ja;
	jo = jo.getJSONObject("d");
	ja = jo.getJSONArray("results");
	JSONObject resultObject = ja.getJSONObject(0);
//	System.out.println(resultObject.get("WebTotal"));
	String hitsString= resultObject.get("WebTotal").toString();
	  numberOfMatches = java.lang.Integer.parseInt(hitsString);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return numberOfMatches;
	}
	
	
	void classify(String curCategory) {

		int categoryNo = this.categories.get(curCategory).hashCode();
		if (categoryNo==0)
		{
			for (int i = categoryNo;i<4;i++) {
				String queryFinal="";
				coverage[i] = 0;
				for (int j = 0; j < queries[i].size(); j++){
				String modQuery[] = queries[i].get(j).trim().split(" ");
				if(modQuery.length>1){
				queryFinal = modQuery[0]+"+"+modQuery[1];}
				else queryFinal=modQuery[0];
				System.out.println(queryFinal);
					coverage[i] += retrieveResults (i, queryFinal);
			
				}
				System.out.println(coverage[i]);
			}
		}
		
		else{
			
			for (int i = categoryNo;i<categoryNo+3;i++) {
				String queryFinal="";
				coverage[i] = 0;
				for (int j = 0; j < queries[i].size(); j++){
				String modQuery[] = queries[i].get(j).split(" ");
				if(modQuery.length>1){
				queryFinal = modQuery[0]+"+"+modQuery[1];}
				else queryFinal=modQuery[0];
//				System.out.println(queryFinal);
					coverage[i] += retrieveResults (i, queryFinal);
				}
			}
		}
	
		
		for(int x:coverage)
			System.out.println("printing coverage");
		
		for (int j = categoryNo; j <4; j++) {

			int total = 0;
			for (int i = categoryNo ; i<4 ; i++)
				total += coverage[i];
			System.out.println("total"+ total);
			System.out.println("testing coverage"+coverage[j]);
			specificity[j] = coverage[j] / total;

			Enumeration<String> e = this.categories.keys();
			String key = "";
			String cur = "";
			while (e.hasMoreElements()) {
				cur = e.nextElement();
				if (j == this.categories.get(cur).hashCode())	
				break;
				
			}

			System.out.println("Specificify for category:" + cur + " is " + specificity[j]);
			System.out.println("Coverage for category:" + cur + " is " + coverage[j]);
			
			System.out.println(this.tes+", "+ this.tec);
			if (specificity[j] >= this.tes && coverage[j] >= this.tec) {
			
				classify("Health");
			}
		}
	}

		
	public static void main(String[] args) {
		
		double tes=0.6;
		int tec=100;
		String website = "cancer.org";
		
		QProber qp = new QProber(tes, tec, website);
		qp.createCategoryList();
		qp.createQueryList();
		
		qp.classify("Root");
		
	}
	
}