import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * 
 */

/**
 * @author Khalilur
 *
 */

public class CreateLexicon {
	
	public TreeMap<String, Integer> lexicon = 
		new TreeMap<String, Integer>();
	public TreeMap<Integer, String> documentmap = 
		new TreeMap<Integer, String>();
	public TreeMap<String, String> stopwords =
		new TreeMap<String, String>();
	public Integer termID;
	public CreateLexicon() throws IOException {
		// TODO Auto-generated constructor stub		
		termID=0;
	}

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args){
		// TODO Auto-generated method stub
		CreateLexicon cl;
		try {
			cl = new CreateLexicon();
			System.out.println("Generating lexicon...");
			FileInputStream fis = new FileInputStream("d:/search/tdt3.zip");
			ZipInputStream zis = new ZipInputStream(fis);
			ZipFile zipFile = new ZipFile("d:/search/tdt3.zip");
			ZipEntry ze;
			//Parsing process		
			int docCount = 0;
			double start = System.currentTimeMillis();		
			while ((ze = zis.getNextEntry()) != null){
				if (!ze.isDirectory()){					
					docCount++;				
					//Parse to XML				
					InputStream input = zipFile.getInputStream(ze);
					Reader r = new InputStreamReader(input);   
					StringWriter sw = new StringWriter();   
					char[] buffer = new char[1024];   
					for (int n; (n = r.read(buffer)) != -1; )   
					    sw.write(buffer, 0, n);
					
					// &amp | regular expression stripping off the text
					Pattern p = Pattern.compile(" & | &AMP; |&AMP;|&");		
			        Matcher m = p.matcher(" &amp; ");		        
			        m.reset(sw.toString());		        
			        cl.XMLReader(m.replaceAll(" &amp; "));
					r.close();				
				}						
			}
			zis.close();
			zipFile.close();
			
			try {			
				cl.WritePostingList();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
			System.out.println();		
			double end = ((System.currentTimeMillis() - start)/1000/60);	    
			System.out.println("Total documents : "+cl.documentmap.size()+" terms.");
			System.out.println("Total posting list : "+cl.lexicon.size()+" terms.");
			System.out.println("Overall process time : "+end+" minutes.");
			System.out.println("Indexing completed.");
			
			cl.lexicon.clear();
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
				
		

	}
	
	public void XMLReader(String xmlString){
		try {			
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();
			DefaultHandler handler = new DefaultHandler() {
				
				private boolean insideText;				
				private StringBuffer textBuffer;			
			
				@Override
				public void startElement(String ns, String ln, String qname, Attributes atts)
				throws SAXException
				{					
					if (qname.equalsIgnoreCase("TEXT")) {			
						insideText = true;			
						textBuffer = new StringBuffer();						
					}					
				}
			
				@Override
				public void characters(char[] ch, int offset, int length)
				throws SAXException
				{
					if (insideText) {
						textBuffer.append(new String(ch, offset, length));
					}
				}
			
				@Override
				public void endElement(String ns, String ln, String qname)
				throws SAXException
				{	
					Stemmer stemmer = new Stemmer();					
					if (qname.equalsIgnoreCase("TEXT")) {
						if (insideText) {
							try {			
								InputStream is = new 
								ByteArrayInputStream(textBuffer.toString()
										.trim().getBytes("UTF-8"));			
								BufferedReader br = 
									new BufferedReader(
											new InputStreamReader(is));
								StreamTokenizer st = new StreamTokenizer(br);
								int position = 0;								
								while (st.nextToken() != StreamTokenizer.TT_EOF) {
									position++;
									//positional index									
									switch (st.ttype) {
									case StreamTokenizer.TT_WORD:										
										// Stop words stripping process		
										String[] splitter = null;
										char[] stringArray=null;
										if (st.sval.contains("-")){																						
											splitter = st.sval.split("-");											
											for (int i = 0; i<splitter.length; i++){
												if (i > 0)position++;
												if (!isStopword(splitter[i].toString().toLowerCase())){
													stringArray = splitter[i].toString().toLowerCase().toCharArray();									    	
													for (int j=0; j<stringArray.length;j++){
														stemmer.add(stringArray[j]);
													}			
													stemmer.stem();		
													if(!stemmer.toString().contains("."))														
														if (!lexicon.containsKey(stemmer.toString())){
															termID++;
															lexicon.put(stemmer.toString(), termID);													
														}														
												}else{									    	
											    	break;
											    }
											}
										} else {
											if (!isStopword(st.sval.toLowerCase())){
												stringArray = st.sval.toLowerCase().toCharArray();									    	
												for (int i=0; i<stringArray.length;i++){
													stemmer.add(stringArray[i]);
												}			
												stemmer.stem();							
												if(!stemmer.toString().contains("."))
													if (!lexicon.containsKey(stemmer.toString())){
														termID++;
														lexicon.put(stemmer.toString(), termID);													
													}
											}else{									    	
										    	break;
										    }
										}											
									    break;
									default:
									    break;
									}
								}
								br.close();
							} catch (Exception e) {
								System.out.println("Exception: " + e);
							}
						}
					}
				}
			};						
			
			saxParser.parse(new InputSource(new StringReader(xmlString)), handler);			
		} catch (Exception e){
				e.printStackTrace();
		}
	}
	
	public void WritePostingList() throws IOException{
		//Posting list
		System.out.println("Writing posting list to disk.");
		double start = System.currentTimeMillis();
		Iterator iterator = lexicon.keySet().iterator();
		
		FileWriter toFile = new FileWriter("d:/search/postinglist.txt");		
		Object obj;		
		while (iterator.hasNext()){
			obj = iterator.next();			
			toFile.write(obj.toString()+":"+lexicon.get(obj)+";");		
		}		    	    
		toFile.flush(); 
		toFile.close();		
		
		System.out.println();		
		double end = ((System.currentTimeMillis() - start)/1000/60);	    
		System.out.println("Total : "+lexicon.size()+" terms.");
		System.out.println("Writing d:/search/postinglist.txt : "+end+" minutes.");
		System.out.println("Task completed.");
	}
	
	public boolean isStopword(String term) throws IOException{		
		boolean status = false;

		if(stopwords.containsKey(term)){			
			status = true;
		}else {
			status = false;
		}		
		return status;
	}
	
	public void loadStopWords() throws IOException{
		FileInputStream input = new FileInputStream("d:/search/stop_words.txt");
		BufferedReader br = new BufferedReader(new InputStreamReader(input));		
		String line = null;
		while((line=br.readLine())!=null){
			stopwords.put(line, null);
		}
	}
}
