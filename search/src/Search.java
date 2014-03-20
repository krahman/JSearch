import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
/**
 * 
 */

/**
 * @author c1pher
 *
 */
public class Search {

	/**
	 * 
	 */
	public String indexFile = "";
	public String postinglistFile = "";
	public String docmapFile = "";	
	public String termdocindexfile ="";
	public TreeMap<Integer, String> docmapindex = null;
	public TreeMap<String, Integer> postinglist = null;	
	public TreeMap<Integer, List<Integer>> documentlist = null;
	public Hashtable<Integer, Hashtable<Integer, List<Integer>>> termdocposindex = null;
	public Hashtable<Integer, List<Integer>> termdocindex = null;
		
	public Search() {
		postinglistFile = "d:/search/postinglist.txt";
		docmapFile = "d:/search/docmap.txt";
		indexFile = "d:/search/search.zip";
		termdocindexfile = "d:/search/term_doc_index.txt";
		documentlist = new TreeMap<Integer, List<Integer>>();
		termdocposindex = new Hashtable<Integer, Hashtable<Integer,List<Integer>>>();
		termdocindex = new Hashtable<Integer, List<Integer>>();
		//getTermDocIndex(termdocindexfile);
		try {
			docmapindex = getDocsMap();
			postinglist = getPostingList();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * @param args
	 * @throws IOException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException{		
		Search search = new Search();
		//search.loadIndex();
		String query = "";
		System.out.println();
		System.out.println("Type your query [search [query]] below or type 'quit' to exit :");
		System.out.println();
		InputStreamReader is = new InputStreamReader(System.in);
		BufferedReader br = new BufferedReader(is);		
		while (!(query.equals("quit"))){
			query = br.readLine();
			if (!(query.equals("quit"))){
				search.checkArgument(query.split(" "));				
			}						
		}
	}	
	
	private void checkArgument(String[] query){		
		int i = 0, j;
        String arg;
        char flag;
        boolean vflag = false;
        String outputfile = "";
        int no = 0; 
        String relevant = "";
        String terms = "";
        if (i == 0 && query[i].equals("search")){
        	if (!(query.length==1)){
        	arg = query[i++];      
		        for (int l = i; l < query.length; l++){        
			        while (i < query.length && query[i].startsWith("-")) {
			            arg = query[i++];    			            
			            if (arg.substring(0, 7).equals("--hits=")) {
			            	int length = arg.length();
			            	no = Integer.parseInt(arg.substring(7,length));
			                System.out.println("First "+no+" documents");			                
			                vflag = true;
			            } 
			            
			            else if (arg.substring(0, 11).equals("--relevant=")) {
			            	int length = arg.length();
			            	relevant = arg.substring(11,length);
			                System.out.println(relevant);
			                vflag = true;
			            }
			        }
			        if (i == query.length){
			        	System.out.println();
			        	System.err.println("Usage: search --hits=27 |or| " +
			        			"--revlevant=(relevant document) query");			        	
			        } else {
			        	System.out.println();
			        	arg = query[i++];
			        	terms += arg + " ";			        	
			        }	        
		        }
        	} else {
        		System.out.println();
	        	System.err.println("Usage: search --hits=27 |or| " +
	        			"--revlevant=(relevant document) query");	        	
        	}
        } else {
        	System.out.println();
        	System.err.println("Usage: Begin with the word " +
        			"'search' to start searching.");	
        	System.out.println();
        }
        try {
			getDisplayedInt(getMatchDocs(terms));
			System.out.println();
			System.out.println("Type your query [search [query]] below or type 'quit' to exit :");
			System.out.println();			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void loadIndex()throws IOException{
		
		ZipInputStream zis;
		FileInputStream fis = null;	
		ZipFile zipFile = null;
		ZipEntry ze;
		
		try {
			fis = new FileInputStream(indexFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		zis = new ZipInputStream(fis);
		try {
			zipFile = new ZipFile(indexFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		while ((ze = zis.getNextEntry()) != null){
			String[] splittedString = null;
			InputStream is = zipFile.getInputStream(ze);
			InputStreamReader r = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(r);
			splittedString = br.readLine().split(";");	
			String termid ="";
			List<Integer> ldocs = new LinkedList<Integer>();
			for (int i = 0; i < splittedString.length; i++){
				String[] subToken = splittedString[i].toString().split(":");
				termid = subToken[0];
				String docid = subToken[1];							
				
				if (termdocindex.containsKey(Integer.parseInt(termid))){
					List<Integer> ldoc = termdocindex.get(Integer.parseInt(termid));
					ldoc.add(Integer.parseInt(docid));
					termdocindex.put(Integer.parseInt(termid), ldoc);
				} else {
					List<Integer> ldoc = new LinkedList<Integer>();
					ldoc.add(Integer.parseInt(docid));
					termdocindex.put(Integer.parseInt(termid), ldoc);
				}
			}
			br.close();
			is.close();
			r.close();
		}
		zipFile.close();	
	}
	
	private int getTermId(String searchTerm) throws IOException{
		
		int termid = 0;		
		TreeMap<String, Integer> postinglist = getPostingList();
		if (postinglist.containsKey(searchTerm)){			
			termid = postinglist.get(searchTerm);
		}	
		return termid;
	}
		
	private void getTermDocPosIndex(int theTermId)throws IOException{
		
		ZipInputStream zis;
		FileInputStream fis = null;	
		ZipFile zipFile = null;
		ZipEntry ze;
		
		int record = 0;		
		try {
			fis = new FileInputStream(indexFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		zis = new ZipInputStream(fis);
		try {
			zipFile = new ZipFile(indexFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Hashtable<Integer, List<Integer>> docpos = new Hashtable<Integer, List<Integer>>();
		List<Integer> results = new LinkedList<Integer>();
		List<Integer> ldocs = new LinkedList<Integer>();
		while ((ze = zis.getNextEntry()) != null){
			String indexData = "";
			String[] splittedString = null;
			InputStream is = zipFile.getInputStream(ze);
			InputStreamReader r = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(r);
			indexData = br.readLine();
			splittedString = indexData.split(";");
			
			for (int i = 0; i < splittedString.length; i++){							
				if (splittedString[i].contains(theTermId+":")){
					String[] subToken = splittedString[i].toString().split(":");						
					String termid = subToken[0];
					if (Integer.parseInt(termid)==theTermId){
						String docid = subToken[1];								
						String[] positions = subToken[2].toString().split(",");
						List<Integer> lpos = new LinkedList<Integer>();
						for (int j=0; j< positions.length; j++)
							lpos.add(Integer.parseInt(positions[j]));						
						docpos.put(Integer.parseInt(docid), lpos);
						ldocs.add(Integer.parseInt(docid));
						
						record++;
					}
				}
			}			
			termdocposindex.put(theTermId, docpos);
		}
		if (results.isEmpty())
			results.addAll(ldocs);
		else 
			results.retainAll(ldocs);
		getDisplayedInt(results);
		
		zipFile.close();			
	}
	
	private String getStemmedQuery(String searchTearm){
		String stemmed = "";
		Stemmer s = new Stemmer();		
		char[] queryArr = searchTearm.toCharArray();
		for (int i=0; i<queryArr.length;i++){
			s.add(queryArr[i]);
		}
		s.stem();
		stemmed = s.toString();
		return stemmed;
	}
	
	public TreeMap<String, Integer> getPostingList() throws IOException{
		TreeMap<String, Integer> postinglist = new TreeMap<String, Integer>();
		FileInputStream fis = new FileInputStream(postinglistFile);
		InputStreamReader r = new InputStreamReader(fis);   
		BufferedReader br = new BufferedReader(r);		
		String[] entries = br.readLine().toString().split(";");
		for (int i=0; i < entries.length; i++){
			String [] termid= entries[i].split(":");
			postinglist.put(termid[0], Integer.parseInt(termid[1]));
		}		
		return postinglist;
	}
	public TreeMap<Integer, String> getDocsMap() throws IOException{
		TreeMap<Integer, String> docmap = new TreeMap<Integer, String>();
		FileInputStream fis = new FileInputStream(docmapFile);
		InputStreamReader r = new InputStreamReader(fis);   
		BufferedReader br = new BufferedReader(r);		
		String[] entries = br.readLine().toString().split(";");
		for (int i=0; i < entries.length; i++){
			String[] tokens = entries[i].split(":");
				docmap.put(Integer.parseInt(tokens[0]), tokens[1]);				
		}		
		return docmap;
	}	
	
	private void getMatchIndex(String query) throws IOException{
		
	}
	
	private void getNotMatchIndex(){
		
	}
	
	private List<Integer> getMatchDocs(String query) throws IOException{
		String[] terms = query.split(" ");		
		List<Integer> results = new LinkedList<Integer>();
		for (String term : terms){
			String searchTerm = getStemmedQuery(term);
			int termid = getTermId(searchTerm);
			List<Integer> docs = new LinkedList<Integer>();
			if (termid > 0){				
				if (termdocposindex.containsKey(termid)){
					Hashtable<Integer, List<Integer>> docpos = termdocposindex.get(termid);
					Iterator docIter = docpos.keySet().iterator();
					Object doc = null;
					while(docIter.hasNext()){
						doc = docIter.next();			
						List<Integer> lpos = docpos.get(doc);						
						//list of docs
						docs.add((Integer) doc);
					}					
					
				} else {
					getTermDocPosIndex(termid);
				}				
			}		
			
			if (results.isEmpty())
				results.addAll(docs);
			else 
				results.retainAll(docs);
			
		}
		return results;	
	}
	
	public TreeMap<Integer, List<Integer>> getWeightedDocs (String query) throws IOException {
		TreeMap<Integer, List<Integer>> rankedResults = new TreeMap<Integer, List<Integer>>();
		
		// find partial results and add up term occurances
		String[] terms = query.split(" ");
		HashMap<Integer, Integer> docids = new HashMap<Integer, Integer>();
		for (String term : terms) {
			int termid = getTermId(term);
			Hashtable<Integer, List<Integer>> docpos = null;				
			if(termdocposindex.contains(termid)){
				docpos = termdocposindex.get(termid);
			} else {				
				getTermDocPosIndex(termid);
				docpos = termdocposindex.get(termid);
			}
			Iterator docIter = docpos.keySet().iterator();
			Object doc = null;
			List<Integer> pResults = new LinkedList<Integer>();
			while(docIter.hasNext()){
				doc = docIter.next();
				pResults.add((Integer) doc);
			}			
			for (int tf : pResults) {
				if (docids.containsKey(tf))
					docids.put(tf, docids.get(tf) + 1);
				else
					docids.put(tf, 1);
			}
		}
		
		// sort results into ranks
		for (Integer docid : docids.keySet()) {
			if (rankedResults.containsKey(docids.get(docid))) {
				List<Integer> l = rankedResults.get(docids.get(docid));
				l.add(docid);
				rankedResults.put(docids.get(docid), l);
			} else {
				List<Integer> l = new LinkedList<Integer>();
				l.add(docid);
				rankedResults.put(docids.get(docid), l);
			}
		}
		
		return rankedResults;
	}
	
	private void getNotMatchDocs(){
		
	}
	
	private void getResultWithOR(){
		
	}
	
	private void getResultWithAND(){
		
	}
	
	private void rankResult(){
		
	}
	
	private void getDisplayed(TreeMap<Integer, List<Integer>> rankResults){
		int record = 0;
		Iterator itr = rankResults.keySet().iterator();
		Object obj;
		while(itr.hasNext()){
			obj = itr.next();
			List<Integer> ls = rankResults.get(obj);
			for (int l: ls){
				String filename = docmapindex.get(l);						
				//System.out.println("["+record+"] Document No: "+filename+".");
			}
		}						
	}
	
	private void getDisplayedInt(List<Integer> results){
		int record = 0;
		for (int result: results){
			record++;
			String filename = docmapindex.get(result);						
			System.out.println("["+record+"] Document No: "+filename+".");		
		}				
	}
}
