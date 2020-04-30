package dk.kb.netarchivesuite.solrwayback.parsers;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntry;
import dk.kb.netarchivesuite.solrwayback.service.dto.IndexDoc;
import dk.kb.netarchivesuite.solrwayback.solr.NetarchiveSolrClient;

public class HtmlParserUrlRewriter {

	private static final Logger log = LoggerFactory.getLogger(HtmlParserUrlRewriter.class);
    
	
	//Problem is jsoup text(...) or html(...) encodes & and for the <style> @import... </style> the HTML urls must not be encoded. (blame HTML standard)
	//So it can not be set using JSOUP and must be replaced after.
	private static final String AMPERSAND_REPLACE="_STYLE_AMPERSAND_REPLACE_";
	
	// private static Pattern backgroundUrlPattern = Pattern.compile(".*background:url\\([\"']?([^\"')]*)[\"']?\\).*"); is this better?
	
	//TODO use for more CSS replacement
	private static Pattern urlPattern = Pattern.compile("[: ,]url\\([\"']?([^\"')]*)[\"']?\\)");
	
	private static Pattern backgroundUrlPattern_OLD = Pattern.compile(".*background:url\\((.*)\\).*");
	
	// See explanation where it is used.
	private static Pattern backgroundUrlPattern = Pattern.compile(".*background(-image)?:\\s*url\\((.*)\\).*");
	
	
	private static final String CSS_IMPORT_PATTERN_STRING = 
			"(?s)\\s*@import\\s+(?:url)?[(]?\\s*['\"]?([^'\")]*\\.css[^'\") ]*)['\"]?\\s*[)]?.*";
	private static Pattern  CSS_IMPORT_PATTERN = Pattern.compile(CSS_IMPORT_PATTERN_STRING);

	//replacing urls that points into the world outside solrwayback because they are never harvested
    private static final String NOT_FOUND_LINK=PropertiesLoader.WAYBACK_BASEURL+"services/notfound/";
	
	public static void main(String[] args) throws Exception{

	   
	  
	  
		  String css1="@import url('gamespot_white-1b5761d5e5bc48746b24cb1d708223cd-blessed1.css?z=1384381230968');#moderation .user-history div.icon div.approved;"+"\n"+
                "@import url(slidearrows.css);\n"+
                "@import url(shadow_frames.css);\n"+
                "body {";



		String css= new String(Files.readAllBytes(Paths.get("/home/teg/gamespot.css")));

//		System.out.println(css);
/*
		String[] result = css.split("\n", 100);
		//Get the lines starting with @import until you find one that does not. 
		int index = 0;
		while (result[index].startsWith("@import ") && index <result.length){    		
			String importLine = result[index++];

			System.out.println("css import found:"+importLine);

			Matcher m = CSS_IMPORT_PATTERN.matcher(importLine);
			if (m.matches()){
				String url= m.group(1);

				System.out.println("found url:"+url);

			}
		}

		System.out.println("done");
		System.exit(1);
*/
		String html="<?xml version=\"1.1\" encoding=\"iso-8859-1\"?>"+
				"<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"da\">"+
				"<head>"+
				"   <title>kimse.rovfisk.dk/katte/</title>"+
				"   <style type=\"text/css\" media=\"screen\">@import \"/css/forum.css\";</style>"+ 
				"   <meta http-equiv=\"Content-Type\" content=\"text/html; charset=ISO-8859-1\" />"+         
				"   <link rel=\"stylesheet\" href=\"/style.css\"  type=\"text/css\" media=\"screen\" />"+
				" <link rel=\"alternate\" type=\"application/rss+xml\" title=\"RSS 2.0\" href=\"/rss2.php\" />"+
				"</head>"+
				"<body>"+
				" <style type=\"text/css\" media=\"screen\">@import \"//www.dr.dk/drdkGlobal/spot/spotGlobal.css\";</style> "+
				" <style type=\"text/css\" media=\"screen\">@import \"/design/www/global/css/globalPrint.css\";</style> "+
                "<style type=\"text/css\" media=\"screen\">@import url(http://en.statsbiblioteket.dk/portal_css/SB%20Theme/resourceplonetheme.sbtheme.stylesheetsmain-cachekey-7e976fa2b125f18f45a257c2d1882e00.css);</style> "+
                "<a class=\"toplink\" href=\"test\" class=\"button\" style=\"background:url(img/homeico.png) no-repeat ; width:90px; margin:0px 0px 0px 16px;\">kimse.rovfisk.dk  </a><a class=\"toplink\" href=\"/katte/\">katte / </a><br /><br /><table cellspacing=\"8\"><tr><td></td><td class=\"itemw\"><a href=\"/katte/?browse=DSC00175.JPG\"><img class=\"lo\" src=\"/cache/katte/DSC00175.JPG\" /></a></td>"+
                "<td></td><td class=\"itemw\"><a href=\"/katte/?browse=DSC00209.JPG\"><img class=\"lo\" src=\"/cache/katte/DSC00209.JPG\" /></a></td>"+
                " <img  src=\"http://belinda.statsbiblioteket.dk:9721/solrwayback/services/downloadRaw?source_file_path=/netarkiv/0122/filedir/285330-279-20180113095114447-00002-kb-prod-har-001.kb.dk.warc.gz&amp;offset=685207223\" srcset=\"http://skraedderiet.dk/wp-content/uploads/2018/01/saks_watermark-001_transparent.jpg 477w, http://skraedderiet.dk/wp-content/uploads/2018/01/saks_watermark-001_transparent-150x150.jpg 150w, http://skraedderiet.dk/wp-content/uploads/2018/01/saks_watermark-001_transparent-298x300.jpg 298w\" sizes=\"(max-width: 235px) 100vw, 235px\">"+              
                "</table><br />  </body>"+
                "</html>";

		HashSet<String> urlSet = new HashSet<String>();
		
		Document doc = Jsoup.parse(html,"http:/test.dk"); //TODO baseURI?
		
		collectRewriteUrlsForImgSrcset(urlSet,doc);
	     
	       
	       
		
	      
	      System.exit(1);
		//collectStyleBackgroundRewrite(urlSet,doc, "a", "style", "http:/thomas.dk");


		URL baseUrl = new URL("http:www.google.com/someFolder/");
		URL url = new URL( baseUrl , "../test.html");
		System.out.println(url.toString());


	}


	/* CSS can start witht the following and need to be url rewritten also.
	 * @import "mystyle.css";
	 * @import url(slidearrows.css);
	 * @import url(shadow_frames.css) print;
	 */

	public static String replaceLinksCss(ArcEntry arc) throws Exception{

		String type="downloadRaw"; //not supporting nested @imports...        		
		String css = arc.getBinaryContentAsStringUnCompressed();		
		String url=arc.getUrl();

		String[] result = css.split("\n", 100); //Doubt there will be more than 100 of these.
		//Get the lines starting with @import until you find one that does not. 
		int index = 0;
				
		while (index <result.length && result[index].startsWith("@import ") ){    		
		  String importLine = result[index++];
		  importLine = importLine.substring(0, Math.min(200, importLine.length()-1)); //Import is in the start of line, it can be very long (minimized) 		  
			Matcher m = CSS_IMPORT_PATTERN.matcher(importLine); // 
			if (m.matches()){
				String cssUrl= m.group(1);		   
				URL base = new URL(url);
				String resolvedUrl = new URL( base ,cssUrl).toString();
				 resolvedUrl =  resolvedUrl.replace("/../", "/");
				IndexDoc indexDoc = NetarchiveSolrClient.getInstance().findClosestHarvestTimeForUrl(resolvedUrl, arc.getCrawlDate());		         
				if (indexDoc!=null){    		    			 
					String newUrl=PropertiesLoader.WAYBACK_BASEURL+"services/"+type+"?source_file_path="+indexDoc.getSource_file_path() +"&offset="+indexDoc.getOffset(); 					
					css=css.replace(cssUrl, newUrl);					
				}else{
	                log.info("could not resolved CSS import url:"+resolvedUrl);  
				    css=css.replace(cssUrl, NOT_FOUND_LINK);				
					log.info("CSS @import url not harvested:"+cssUrl);
				}	    		 	    		 
			}
		}
		
		//Replace URL's
		
		return css;
	}

	/**
	 * Extracts the HTML from the ArcEntry and replaces links and other URLs with the archived versions that are
	 * closest to the ArcEntry in time.
	 * @param arc an arc-entry that is expected to be a HTML page.
	 * @return the page with links to archived versions instead of live web version.
	 * @throws Exception if link-resolving failed.
	 */
	public static HtmlParseResult replaceLinks(ArcEntry arc) throws Exception{
		return replaceLinks(
				arc.getBinaryContentAsStringUnCompressed(), arc.getUrl(), arc.getWaybackDate(), solrNearestResolver);
	}

	/**
	 * Replaces links and other URLs with the archived versions that are closest to the links in the html in time.
	 * @param html the web page to use as basis for replacing links.
	 * @param url the URL for the html (needed for resolving relative links).
	 * @param crawlDate the ideal timestamp for the archived versions to link to.
	 * @param nearestResolver handles url -> archived-resource lookups based on smallest temporal distance to crawlDate.
	 * @throws Exception if link resolving failed.
	 */
	public static HtmlParseResult replaceLinks(
			String html, String url, String crawlDate, NearestResolver nearestResolver) throws Exception {
		AtomicInteger numberOfLinksReplaced = new  AtomicInteger();
		AtomicInteger numberOfLinksNotFound = new  AtomicInteger();

		long start = System.currentTimeMillis();

		Document doc = Jsoup.parse(html, url);

		//List<String> urlList = new ArrayList<String>();
		HashSet<String> urlSet = getUrlResourcesForHtmlPage(doc, url);
		log.info("#unique urlset to resolve for arc-url '" + url + "' :" + urlSet.size());

		List<IndexDoc> docs = nearestResolver.findNearestHarvestTime(urlSet, crawlDate);

		//Rewriting to url_norm, so it can be matched when replacing.
		HashMap<String, IndexDoc> urlReplaceMap = new HashMap<String,IndexDoc>();
		for (IndexDoc indexDoc: docs){
//			log.info("warn: url:"+indexDoc.getUrl() +" url_norm:"+indexDoc.getUrl_norm());
			urlReplaceMap.put(indexDoc.getUrl_norm(),indexDoc);
		}

		replaceUrlForElement(urlReplaceMap,doc, "img", "src", "downloadRaw",  numberOfLinksReplaced , numberOfLinksNotFound);
		replaceUrlForElement(urlReplaceMap,doc, "embed", "src", "downloadRaw",  numberOfLinksReplaced , numberOfLinksNotFound);
		replaceUrlForElement(urlReplaceMap,doc, "source", "src", "downloadRaw",  numberOfLinksReplaced , numberOfLinksNotFound);
		replaceUrlForElement(urlReplaceMap,doc, "body", "background", "downloadRaw" ,  numberOfLinksReplaced ,  numberOfLinksNotFound);
		replaceUrlForElement(urlReplaceMap,doc, "link", "href", "view",  numberOfLinksReplaced ,  numberOfLinksNotFound);
		replaceUrlForElement(urlReplaceMap,doc, "script", "src", "downloadRaw",  numberOfLinksReplaced,  numberOfLinksNotFound);
		replaceUrlForElement(urlReplaceMap,doc, "td", "background", "downloadRaw",  numberOfLinksReplaced ,  numberOfLinksNotFound);
		replaceUrlForFrame(urlReplaceMap,doc, "view",  numberOfLinksReplaced ,  numberOfLinksNotFound); //No toolbar
		replaceUrlForIFrame(urlReplaceMap,doc, "view",  numberOfLinksReplaced ,  numberOfLinksNotFound); //No toolbar
		replaceUrlsForImgSrcset(urlReplaceMap, doc, url, numberOfLinksReplaced, numberOfLinksNotFound);
		replaceUrlsForSourceSrcset(urlReplaceMap, doc, url, numberOfLinksReplaced, numberOfLinksNotFound);
		replaceStyleBackground(urlReplaceMap,doc, "a", "style", "downloadRaw",url,  numberOfLinksReplaced,  numberOfLinksNotFound);
		replaceStyleBackground(urlReplaceMap,doc, "div", "style", "downloadRaw",url,  numberOfLinksReplaced,  numberOfLinksNotFound);
		replaceUrlsForStyleImport(urlReplaceMap,doc,"downloadRaw",url ,  numberOfLinksReplaced,  numberOfLinksNotFound);


		//This are not resolved until clicked
		rewriteUrlForElement(doc, "a" ,"href",crawlDate);
		rewriteUrlForElement(doc, "area" ,"href",crawlDate);
		rewriteUrlForElement(doc, "form" ,"action",crawlDate);

		log.info("Number of resolves:"+urlSet.size() +" total time:"+(System.currentTimeMillis()-start));
		log.info("numberOfReplaced:"+numberOfLinksReplaced + " numbernotfound:"+numberOfLinksNotFound);

		String html_output= doc.toString();
		html_output=html_output.replaceAll(AMPERSAND_REPLACE, "&");

		HtmlParseResult res = new HtmlParseResult();
		res.setHtmlReplaced(html_output);
		res.setNumberOfLinksReplaced(numberOfLinksReplaced.intValue());
		res.setNumberOfLinksNotFound(numberOfLinksNotFound.intValue());
		return res;
	}

	/**
	 * Resolves instances of documents based on time distance.
	 */
	public interface NearestResolver {
		/**
		 * Locates one instance of each url, as close to timeStamp as possible.
		 * @param urls the URLs to resolve.
		 * @param timeStamp a timestamp formatted as {@code TODO: state this}
		 * @return  IndexDocs for the located URLs containing at least
		 *          {@code url_norm, url, source_file, source_file_offset} for each document.
		 */
		List<IndexDoc> findNearestHarvestTime(Collection<String> urls, String timeStamp) throws Exception;
	}
	// Default Solr-index based NearestResolver.
	private static NearestResolver solrNearestResolver = new NearestResolver() {
		@Override
		public List<IndexDoc> findNearestHarvestTime(Collection<String> urls, String timeStamp) throws Exception {
			return NetarchiveSolrClient.getInstance().findNearestHarvestTimeForMultipleUrls(urls, timeStamp);
		}
	};

	public static String generatePwid(ArcEntry arc) throws Exception{

      long start = System.currentTimeMillis();
      String html = new String(arc.getBinary(),arc.getContentEncoding());
      String url=arc.getUrl();

       String collectionName = PropertiesLoader.PID_COLLECTION_NAME;
      Document doc = Jsoup.parse(html,url); //TODO baseURI?

     
       HashSet<String> urlSet =  getUrlResourcesForHtmlPage(doc, url);
           
      log.info("#unique urlset to resolve:"+urlSet.size());

      ArrayList<IndexDoc> docs = NetarchiveSolrClient.getInstance().findNearestHarvestTimeForMultipleUrls(urlSet,arc.getCrawlDate());

      StringBuffer buf = new StringBuffer();
      for (IndexDoc indexDoc: docs){
          buf.append("<part>\n");        
          buf.append("urn:pwid:"+collectionName+":"+indexDoc.getCrawlDate()+":part:"+indexDoc.getUrl() +"\n");
          buf.append("</part>\n");
          //pwid:netarkivet.dk:time:part:url      
      }
     return buf.toString();
	}

  
	public static HashSet<String> getResourceLinksForHtmlFromArc(ArcEntry arc) throws Exception{

      String html = arc.getBinaryContentAsStringUnCompressed();

      String url=arc.getUrl();


      Document doc = Jsoup.parse(html,url); //TODO baseURI?

      
      HashSet<String> urlSet =  getUrlResourcesForHtmlPage(doc, url);
                 
     return urlSet;
    }
	
	

	public static void replaceUrlForElement( HashMap<String,IndexDoc>  map,Document doc,String element, String attribute , String type ,  AtomicInteger numberOfLinksReplaced,   AtomicInteger numberOfLinksNotFound) {

		for (Element e : doc.select(element)) {    		 
			String url = e.attr("abs:"+attribute);

			if (url == null  || url.trim().length()==0){
				continue;
			}
			url =  url.replace("/../", "/");
			
			IndexDoc indexDoc = map.get(Normalisation.canonicaliseURL(url));   
			if (indexDoc!=null){    		    			 
				String newUrl=PropertiesLoader.WAYBACK_BASEURL+"services/"+type+"?source_file_path="+indexDoc.getSource_file_path()+"&offset="+indexDoc.getOffset();    			 
				e.attr(attribute,newUrl);    			     		 
			    numberOfLinksReplaced.getAndIncrement();
			}
			else{
			     e.attr(attribute,NOT_FOUND_LINK);
				log.info("No harvest found for:"+url);
				numberOfLinksNotFound.getAndIncrement();;
			 }

		}
	}
	
	/*
	 * Will not generate toolbar
	 */
	   public static void replaceUrlForFrame( HashMap<String,IndexDoc>  map,Document doc, String type ,  AtomicInteger numberOfLinksReplaced,   AtomicInteger numberOfLinksNotFound) {
          String element="frame";
          String attribute="src";
	     
	        for (Element e : doc.select(element)) {          
	            String url = e.attr("abs:"+attribute);

	            if (url == null  || url.trim().length()==0){
	                continue;
	            }
	            url =  url.replace("/../", "/");
	            IndexDoc indexDoc = map.get(Normalisation.canonicaliseURL(url));   
	            if (indexDoc!=null){                             
	                String newUrl=PropertiesLoader.WAYBACK_BASEURL+"services/"+type+"?source_file_path="+indexDoc.getSource_file_path()+"&offset="+indexDoc.getOffset()+"&showToolbar=false";           
	                e.attr(attribute,newUrl);                            
	                numberOfLinksReplaced.getAndIncrement();
	            }
	            else{
	                 e.attr(attribute,NOT_FOUND_LINK);
	                log.info("No harvest found for:"+url);
	                numberOfLinksNotFound.getAndIncrement();;
	             }

	        }
	    }
	     /*
	    * Will not generate toolbar
	     */
	       public static void replaceUrlForIFrame( HashMap<String,IndexDoc>  map,Document doc, String type ,  AtomicInteger numberOfLinksReplaced,   AtomicInteger numberOfLinksNotFound) {
	          String element="iframe";
	          String attribute="src";
	         
	            for (Element e : doc.select(element)) {          
	                String url = e.attr("abs:"+attribute);

	                if (url == null  || url.trim().length()==0){
	                    continue;
	                }
	                url =  url.replace("/../", "/");   
	                IndexDoc indexDoc = map.get(Normalisation.canonicaliseURL(url));   
	                if (indexDoc!=null){                             
	                    String newUrl=PropertiesLoader.WAYBACK_BASEURL+"services/"+type+"?source_file_path="+indexDoc.getSource_file_path()+"&offset="+indexDoc.getOffset()+"&showToolbar=false";           
	                    e.attr(attribute,newUrl);                            
	                    numberOfLinksReplaced.getAndIncrement();
	                }
	                else{
	                     e.attr(attribute,NOT_FOUND_LINK);
	                    log.info("No harvest found for:"+url);
	                    numberOfLinksNotFound.getAndIncrement();;
	                 }

	            }
	        }
	    

	   

	public static void replaceStyleBackground( HashMap<String,IndexDoc>  map,Document doc,String element, String attribute , String type, String baseUrl,   AtomicInteger numberOfLinksReplaced,  AtomicInteger numberOfLinksNotFound) throws Exception{

		for (Element e : doc.select(element)) {

			String style = e.attr(attribute);

			if (style == null  || style.trim().length()==0){
				continue;
			}    		  

			String urlUnresolved = getStyleMatch(style);   		   		

			if ( urlUnresolved != null){
				URL base = new URL(baseUrl);
				String resolvedUrl = new URL( base ,urlUnresolved).toString();			
				resolvedUrl =  resolvedUrl.replace("/../", "/");
				IndexDoc indexDoc = map.get(Normalisation.canonicaliseURL(resolvedUrl));   
				if (indexDoc!=null){    		    			 
					String newUrl=PropertiesLoader.WAYBACK_BASEURL+"services/"+type+"?source_file_path="+indexDoc.getSource_file_path() +"&offset="+indexDoc.getOffset();    			     		
					String styleFixed=style.replaceAll(urlUnresolved,newUrl);    			     
					e.attr(attribute,styleFixed); 
				     numberOfLinksReplaced.getAndIncrement();
				}
				else{
				  String styleFixed=style.replaceAll(urlUnresolved,NOT_FOUND_LINK);                    
                  e.attr(attribute,styleFixed); 				  			     
                  log.info("No harvest found for(style):"+resolvedUrl);
                  numberOfLinksNotFound.getAndIncrement();
				}


			}

		}   }

	/*
	 * For stupid HTML standard 
	 * <a href="test" style="background:url(img/homeico.png) no-repeat ; width:90px; margin:0px 0px 0px 16px;">
	 * 
	 */
	public static void collectStyleBackgroundRewrite(HashSet<String> set,Document doc,String element, String attribute ,  String baseUrl) throws Exception {

		for (Element e : doc.select(element)) {

			String style = e.attr(attribute);

			if (style == null  || style.trim().length()==0){
				continue;
			}    		     		 
			String unResolvedUrl = getStyleMatch(style);   		   		

			if (unResolvedUrl != null){
	           unResolvedUrl =   unResolvedUrl.replace("/../", "/");
			  URL base = new URL(baseUrl);
				URL resolvedUrl = new URL( base , unResolvedUrl);			
				set.add(Normalisation.canonicaliseURL(resolvedUrl.toString()));
			}

		}
	}

	/*
	 * 
	 * background:url(img/homeico.png) no-repeat ; width:90px
	 * result:  ... ->  img/homeico.png
	 * 
	 * For div-tag:
	 * background-image:url('https://www.proscenium.dk/wp-content/uploads/2018/12/StatensKunstfond-PR-300x169.jpg');
	 * result: https://www.proscenium.dk/wp-content/uploads/2018/12/StatensKunstfond-PR-300x169.jpg
	 * 
	 * Dont know if " is allowed instead of '
	 */
	private static String getStyleMatch(String style){
		//log.info("matching style:"+style);
	  Matcher m = backgroundUrlPattern.matcher(style);
		if (m.matches()){

		  String url= m.group(2); 
		  if (url.startsWith("'") && url.endsWith("'")){
           url=url.substring(1, url.length()-1);		    
		  }
	      //log.info("style found:"+url);
          url =   url.replace("/../", "/");
          return url;
		}
        //log.info("style not found");
		return null;    	
	}

	public static void collectRewriteUrlsForElement(HashSet<String> set,Document doc,String element, String attribute ) {

		for (Element e : doc.select(element)) {
			String url = e.attr("abs:"+attribute);
                 if ("embed".equals(element)) {System.out.println("embed url found:"+url);}
			if (url == null  || url.trim().length()==0){
				continue;
			}
			url = url.replace("/../", "/");
//			System.out.println("adding url:"+(Normalisation.canonicaliseURL(url)));
			set.add(Normalisation.canonicaliseURL(url));   		    		 		
		}
	}

	
       // srcset="http://www.test.dk/img1 477w, http://www.test.dk/img2 150w" 
	   // comma seperated, size is optional.
	   // data-srcset is not html standard but widely used
	   public static void collectRewriteUrlsForImgSrcset(HashSet<String> set,Document doc) {
	        for (Element e : doc.select("img")) {
	          //Can be one of each, but only one for each img tab.  
	            String urls1 = e.attr("abs:srcset");
	            String urls2 = e.attr("abs:data-srcset");
	            String urls = null;
	            if ( urls1 != null && !urls1.trim().isEmpty()){
	              urls = urls1;	              
	            } else {
	              urls = urls2;
	            }	            	              
	            lenientAddURLs(urls, doc.baseUri(), set);
	        }
	    }
	
	    // srcset="http://www.test.dk/img1 477w, http://www.test.dk/img2 150w" 
       // comma seperated, size is optional.
       public static void collectRewriteUrlsForSourceSrcset(HashSet<String> set,Document doc) {

            for (Element e : doc.select("source")) {
                String urls = e.attr("abs:srcset");

                if (urls == null  || urls.trim().length()==0){
                    continue;
                }
                // split.
                String[] urlList = urls.split(",");
                for (String current : urlList){
                  current=current.trim();                 
                 String url =current.split(" ")[0].trim();
                 url = url.replace("/../", "/");
                 String url_norm= Normalisation.canonicaliseURL(url);
                 
                 //log.info("Collect srcset url:"+url_norm);                 
                 set.add(url_norm);                  
                }                               
                                            
            }
        }
	   
	//<style type="text/css" media="screen">@import url(http://en.statsbiblioteket.dk/portal_css/SB%20Theme/resourceplonetheme.sbtheme.stylesheetsmain-cachekey-7e976fa2b125f18f45a257c2d1882e00.css);</style> 
	public static void collectRewriteUrlsForStyleImport(HashSet<String> set, Document doc, String baseUrl) throws Exception{

      for (Element e : doc.select("style")) {         
        String styleTagContent = e.data();          
        Matcher m = CSS_IMPORT_PATTERN.matcher(styleTagContent);
        if (m.matches()){
            String cssUrl= m.group(1);         
              URL base = new URL(baseUrl);
              URL resolvedUrl = new URL( base , cssUrl);           
                            
              set.add(Normalisation.canonicaliseURL(resolvedUrl.toString()));
        }                   
      }
  }


    // srcset="http://www.test.dk/img1 477w, http://www.test.dk/img2 150w" 
    // comma seperated, size is optional.
    public static void replaceUrlsForImgSrcset(HashMap<String,IndexDoc>  map,Document doc, String baseUrl,   AtomicInteger numberOfLinksReplaced,   AtomicInteger numberOfLinksNotFound) {
         for (Element e : doc.select("img")) {
             //Both srcset and data-srcset is alowed, but only one of them
             String url1 = e.attr("abs:srcset");
             String url2 = e.attr("abs:data-srcset");
             String urls;
             String type;
              if (url1 != null && !url1.trim().isEmpty()){
                urls = url1;
                type="srcset";
              }
              else{                
                urls = url2; //Can still be null
                type="data-srcset";
              }
             
             if (urls == null  || urls.trim().length()==0){
                 continue;
             }
             // Unfortunately 'abs:srcset' is broken in Jsoup: In only converts the first URL to absolute.
			 // We need to to the conversion explicitly.
			 String urlsReplaced = lenientConvertURLs(urls, map, baseUrl, numberOfLinksReplaced, numberOfLinksNotFound);
             log.info("urls replaced for type:"+type+" urls:"+urlsReplaced);
             e.attr(type,urlsReplaced);  
         }
     }

	/**
	 * Parses the urls, ensures they are absolute and adds them to absURLs.
 	 * @param urls one or more URLs separated by {@code ,}. Each URL can be either a plain URL or an URL followed by
	 *             a space and some text.
	 * @param baseURL the base URL for the HTML page. Needed for relative URLs.
	 * @param absURLs the absolute URLs will be added here.
	 * @return a String with the converted URLs.
	 */
	private static void lenientAddURLs(String urls, String baseURL, Set<String> absURLs) {
		if (urls == null) {
			return;
		}
		URL base = null;
		try {
			base = new URL(baseURL);
		} catch (MalformedURLException e) {
			log.debug("lenientAddURLs: Unable to parse baseURL '" + baseURL + "', which means all URLs in '" +
					  urls + "' will be converted as-is");
		}
		for (String url: COMMA_SPLITTER.split(urls)) {
			String abs = SPACE_SPLITTER.split(url.trim(), 2)[0].replace("/../", "/");
			if (abs.isEmpty()) {
				continue;
			}
			try {
				// TODO: Consider speeding up by checking if abs is prefixed with {@code https?://}
				abs = base == null ? abs : new URL(base, abs).toString();
			} catch (MalformedURLException e) {
				log.debug("lenientAddURLs: Unable to create an absolute URL using new URL('" + base + "', '" +
						  abs + "'), the problematic URL will be passed as-is");
			}
			absURLs.add(Normalisation.canonicaliseURL(abs));
		}
	}

	/**
	 * Generic URL converter that supports multiple URLs and values after the URLs.
 	 * @param urls one or more URLs separated by {@code ,}. Each URL can be either a plain URL or an URL followed by
	 *             a space and some text.
	 * @param resolvedURLs a map with previously resolved URLs, used for replacing the URLs with links to archived
	 *                     versions.
	 * @param numberOfLinksReplaced when an URL is converted, this is incremented.
	 * @param numberOfLinksNotFound when an URL could not be converted, this is incremented.
	 * @return a String with the converted URLs.
	 */
	private static String lenientConvertURLs(
			String urls, Map<String, IndexDoc> resolvedURLs, String baseURL,
			AtomicInteger numberOfLinksReplaced, AtomicInteger numberOfLinksNotFound) {
		URL base = null;
		try {
			base = new URL(baseURL);
		} catch (MalformedURLException e) {
			log.debug("lenientConvertURLs: Unable to parse baseURL '" + baseURL + "', which means all URLs in '" +
					  urls + "' will be converted as-is");
		}
		StringBuilder sb = new StringBuilder();
		for (String url: COMMA_SPLITTER.split(urls)) {
			url = url.trim();
			if (url.isEmpty()) {
				continue;
			}
			String[] tokens = SPACE_SPLITTER.split(url, 2);
			String abs = tokens[0].replace("/../", "/");
			if (abs.isEmpty()) {
				continue;
			}
			try {
				// TODO: Consider speeding up by checking if abs is prefixed with {@code https?://}
				abs = base == null ? abs : new URL(base, abs).toString();
			} catch (MalformedURLException e) {
				log.debug("lenientConvertURLs: Unable to create an absolute URL using new URL('" + base + "', '" +
						  abs + "'), the problematic URL will be passed as-is");
			}
			String norm = Normalisation.canonicaliseURL(abs);
			IndexDoc indexDoc = resolvedURLs.get(norm);

			if (sb.length() != 0) { // Separate by comma if there are more than 1 URL
				sb.append(", ");
			}

			if (indexDoc != null){
				String converted = PropertiesLoader.WAYBACK_BASEURL + "services/downloadRaw?source_file_path="+
								   indexDoc.getSource_file_path() + "&offset="+indexDoc.getOffset();
				sb.append(converted);
				numberOfLinksReplaced.getAndIncrement();
			} else{
				sb.append(NOT_FOUND_LINK);
				numberOfLinksNotFound.getAndIncrement();
			}

			if (tokens.length > 1) { // There might be something after the URL (from srcset or similar)
				sb.append(" ").append(tokens[1]);
			}
		}
		return sb.toString();
	}
	private static final Pattern COMMA_SPLITTER = Pattern.compile(", *");
	private static final Pattern SPACE_SPLITTER = Pattern.compile(" +");

	// srcset="http://www.test.dk/img1 477w, http://www.test.dk/img2 150w"
    // comma seperated, size is optional.
    public static void replaceUrlsForSourceSrcset(HashMap<String,IndexDoc>  map,Document doc, String baseUrl,   AtomicInteger numberOfLinksReplaced,   AtomicInteger numberOfLinksNotFound) {
         for (Element e : doc.select("source")) {
             String urls = e.attr("abs:srcset");
             String urlsReplaced = urls; //They will be changed one at a time

             if (urls == null  || urls.trim().length()==0){
                 continue;
             }
             // split.
             String[] urlList = urls.split(",");
             for (String current : urlList){
              current=current.trim();                 
              String urlUnresolved =current.split(" ")[0].trim();
              urlUnresolved = urlUnresolved.replace("/../", "/");
              String url_norm = Normalisation.canonicaliseURL(urlUnresolved);
              //log.info("Replace srcset url part:'"+url_norm+"'");
              IndexDoc indexDoc = map.get(url_norm);   
              if (indexDoc!=null){                             
                  String newUrl=PropertiesLoader.WAYBACK_BASEURL+"services/downloadRaw?source_file_path="+indexDoc.getSource_file_path() +"&offset="+indexDoc.getOffset();                           
                  urlsReplaced = urlsReplaced.replace(urlUnresolved, newUrl);                                                         
                  //log.info("replaced srcset url:" + urlUnresolved +" by "+newUrl);
                  numberOfLinksReplaced.getAndIncrement();
              }
              else{
                String newUrl=NOT_FOUND_LINK;                           
                urlsReplaced = urlsReplaced.replace(urlUnresolved, newUrl);                
                log.info("No harvest found srcset url:"+urlUnresolved);
                numberOfLinksNotFound.getAndIncrement();
               }

                
             } 
             e.attr("srcset",urlsReplaced);  
                                         
         }
     }
    
	
	public static void replaceUrlsForStyleImport( HashMap<String,IndexDoc>  map, Document doc, String type, String baseUrl,   AtomicInteger numberOfLinksReplaced,   AtomicInteger numberOfLinksNotFound) throws Exception{
	
      for (Element e : doc.select("style")) {         
        String styleTagContent = e.data();          
        Matcher m = CSS_IMPORT_PATTERN.matcher(styleTagContent);
        if (m.matches()){
          String url = m.group(1);
          URL base = new URL(baseUrl);
          URL resolvedUrl = new URL( base , url);            
          

          IndexDoc indexDoc = map.get(Normalisation.canonicaliseURL(resolvedUrl.toString()));   
          if (indexDoc!=null){                             
              String newUrl=PropertiesLoader.WAYBACK_BASEURL+"services/"+type+"?source_file_path="+indexDoc.getSource_file_path()+"_STYLE_AMPERSAND_REPLACE_offset="+indexDoc.getOffset();                 
              log.info("replaced @import:"+e );
              log.info("replaced with:"+newUrl );
              e.html("@import url("+newUrl+");"); //e.text will be encoded                            
              log.info("new tag (html):"+e);
              numberOfLinksReplaced.incrementAndGet();
          }
          else{
            e.html("@import url("+NOT_FOUND_LINK+");"); //e.text will be encoded
              log.info("No harvest found for:"+resolvedUrl );
              numberOfLinksNotFound.incrementAndGet();
          }
          
        }                   
      }
  }

	
	/*
	 * Only rewrite to new service, no need to resolve until clicked
	 * 
	 */
	public static void rewriteUrlForElement(Document doc, String element, String attribute,  String waybackDate) {
		for (Element e : doc.select(element)) {
			String url = e.attr("abs:"+attribute);
			if (url == null  || url.trim().length()==0){
				continue;
			}    		                                      
       			//String newUrl=PropertiesLoader.WAYBACK_BASEURL+"services/"+"viewhref?url="+urlEncoded+"&crawlDate="+crawlDate;    			            
           //Format is: ?waybackdata=20080331193533/http://ekstrabladet.dk/112/article990050.ece 
            String newUrl=PropertiesLoader.WAYBACK_BASEURL+"services/web/"+waybackDate+"/"+url;
            e.attr("href",newUrl);    			     		 
		}   		  		
	}

	

	public static HashSet<String> getUrlResourcesForHtmlPage( Document doc, String url) throws Exception {
	  HashSet<String> urlSet = new HashSet<String>();
      collectRewriteUrlsForElement(urlSet,doc, "area", "href");
      collectRewriteUrlsForElement(urlSet, doc, "img", "src");
      collectRewriteUrlsForElement(urlSet, doc, "embed", "src");
      collectRewriteUrlsForElement(urlSet, doc, "source", "src");
      collectRewriteUrlsForImgSrcset(urlSet, doc);
      collectRewriteUrlsForSourceSrcset(urlSet, doc);
      collectRewriteUrlsForElement(urlSet,doc, "body", "background");
      collectRewriteUrlsForElement(urlSet, doc, "link", "href");
      collectRewriteUrlsForElement(urlSet , doc, "script", "src");
      collectRewriteUrlsForElement(urlSet, doc, "td", "background");
      collectRewriteUrlsForElement(urlSet,doc, "frame", "src");
      collectRewriteUrlsForElement(urlSet,doc, "iframe", "src");
      collectStyleBackgroundRewrite(urlSet , doc, "a", "style",url);
      collectStyleBackgroundRewrite(urlSet , doc, "div", "style",url);
      collectRewriteUrlsForStyleImport(urlSet, doc,url);            
      return urlSet;	  
	}
	
	public static HashMap<String,String> test(String html,String url) {
		HashMap<String,String> imagesSet = new HashMap<String,String>();  // To remove duplicates
		Document doc = Jsoup.parse(html,url); //TODO baseURI?
		//System.out.println(doc);
		return imagesSet;
	}

}
