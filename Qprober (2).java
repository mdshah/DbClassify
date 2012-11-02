import java.util.*;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.codec.binary.Base64;

import jsonfiles.JSONArray;
import jsonfiles.JSONObject;  
import java.util.Hashtable;
import java.util.Enumeration;

public class Qprober{
	
	int count;
	private double tes;
	private int tec;
	private String website;
	private String finalCat = "Root";
	private Hashtable<String, Integer> categories = new Hashtable<String, Integer>();
	private ArrayList<String>[] queries = new ArrayList[10];
	private double[] specificity = new double[10];
	private int[] coverage = new int[10];
	private String[] categoryArray= {"Root", "Computers", "Health" , "Sports", "Hardware", "Programming" , "Fitness", "Diseases", "Basketball", "Soccer"};
	
	public Qprober(double tes, int tec, String website) {

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
//		System.out.println("Categories added");
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
	}

	public int retrieveResults(int categoryNo, String query) {
		int numberOfMatches=0;
		String jsonUrl="";
		try {
	String accountKey = "67o8I/cEs9YvBBHN2D25/1U3CUnSiXA746DnSts1d0I=";
	String bingUrl = "https://api.datamarket.azure.com/Data.ashx/Bing/SearchWeb/v1/Composite?Query=%27site%3a"+website+"%20"+query+"%27&$top=10&$format=JSON";
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
//	System.out.println(json);
	JSONObject jo = new JSONObject(json);
	
	JSONArray ja;
	jo = jo.getJSONObject("d");
	ja = jo.getJSONArray("results");
	JSONObject resultObject = ja.getJSONObject(0);
	String hitsString= resultObject.get("WebTotal").toString();
	  numberOfMatches = java.lang.Integer.parseInt(hitsString);
	  
	  JSONArray web;
	  web = resultObject.getJSONArray("Web");
	  int size = web.length();
	  if (size>4){
	  for (int i = 0; i < 4; i++)
	  {
	      JSONObject resultObj = web.getJSONObject(i);
	     jsonUrl= resultObj.get("Url").toString();
//		  System.out.println(jsonUrl);
	    	  
	  }
//	  System.out.println("---");
	  }

	  else if (size<4){
		  for (int i = 0; i < size; i++)
		  {
		      JSONObject resultObj = web.getJSONObject(i);
		      jsonUrl= resultObj.get("Url").toString();
//			  System.out.println(jsonUrl);
		  }
//		  System.out.println("---");
		  }
	  
		} catch (Exception e) {
			e.printStackTrace();
		}
	try{
		if(count==0)
		{
			FileWriter fw = new FileWriter("sample-Root-diabetes.txt",true); 
		    fw.write(jsonUrl);
		    fw.close();
		}
		
		else{
			FileWriter fw = new FileWriter("sample-Health-diabetes.txt",true); 
		    fw.write(jsonUrl);
		    fw.close();
		}
	} catch (Exception e) {
		e.printStackTrace();
	}
		
		return numberOfMatches;
	}
	
	
	void classify(String curCategory) {
	
		int subcat_from, subcat_to;
		int categoryNo = this.categories.get(curCategory).hashCode();
		if (categoryNo==0){
			subcat_from=1;
			subcat_to=3;}
		else if (categoryNo>0 && categoryNo<4){
			subcat_from=categoryNo*2+2;
			subcat_to=categoryNo*2+3;}
		else{
			subcat_from = -1;
			subcat_to = -1;
		}
		
		if (subcat_from < 0 || subcat_to < 0)
			return;

		for (int i = subcat_from ; i<=subcat_to ; i++) {
				String queryFinal="";
				coverage[i] = 0;
				for (int j = 0; j < queries[i].size(); j++){
				String modQuery[] = queries[i].get(j).trim().split(" ");
				if(modQuery.length>1){
				queryFinal = modQuery[0]+"+"+modQuery[1];}
				else queryFinal=modQuery[0];
					coverage[i] += retrieveResults (i, queryFinal);
			
				}
			}
		
		for (int j = subcat_from ; j <= subcat_to ; j++) {
			int total = 0;
			for (int i = subcat_from ; i<=subcat_to ; i++)
				total += coverage[i];

			specificity[j] = (double) coverage[j] / (double) total;
		}

		for (int x =0;x<10;x++)
			System.out.println(categoryArray[x]+" "+coverage[x]+" "+specificity[x]);
		
		for (int j = subcat_from ; j <= subcat_to ; j++) {
			
			if (specificity[j] >= this.tes && coverage[j] >= this.tec) {
				finalCat= finalCat + "/" + categoryArray[j];
				count=count+1;
				classify(this.categoryArray[j]);
				
				
				
			}
		}
		
		
	}
	
	public void printResults(){
			System.out.println(finalCat);
		
	}

	public static void main(String[] args) {
		
		double tes=0.6;
		int tec=100;
		String website = "diabetes.org";
		System.out.println(website+"\n");
		Qprober qp = new Qprober(tes, tec, website);
		qp.createCategoryList();
		qp.createQueryList();		
		qp.classify("Root");
		qp.printResults();
	}
	
}
