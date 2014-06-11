package com.github.hailinzeng;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

/**
 * Download all files and pages in mediawiki
 * 
 * @author Hailin Zeng
 */

public class WikiSpider {
	/**
	 * Get all files uploaded: { URL -> FileName }
	 */
	public static Map<String, String> getFileUrls() throws ParseException,
			IOException {
		Map<String, String> urls = new HashMap<String, String>();

		HttpClient httpclient = new DefaultHttpClient();

		String pageurl = WikiConf.FILE_LIST_URL;

		do {
			// get page size
			HttpGet httpget = new HttpGet(pageurl);
			httpget.setHeader("Cookie", WikiConf.Cookie);

			HttpResponse response = httpclient.execute(httpget);
			HttpEntity entity = response.getEntity();

			String html = EntityUtils.toString(entity);

			Document doc = Jsoup.parse(html);

			Elements nameList = doc.getElementsByClass("TablePager_col_img_name");
			
			for (Element e : nameList) {
				String url = e.childNode(2).attr("href");
				String filename = e.childNode(0).attr("title").substring(3);

				urls.put(url, filename);
			}
			
			// has next page?
			Elements nextPages = doc.getElementsMatchingOwnText("下一页");
			Element nextPage = nextPages.first();
			
			if (nextPage.tagName().equals("a")) {
				pageurl = WikiConf.HOST_PREFIX + nextPage.attr("href");
			} else {
				break;
			}
			
		} while (true);

		return urls;
	}

	public static void downloadAllFiles() {
		try {
			File filedir = new File(WikiConf.OUTPUT_FOLDER + "file/");
			if(!filedir.exists()){
				filedir.mkdirs();
			}
			
			// load already download
			Set<String> alreadyDownloadFiles = new HashSet<String>();
			File[] filelist = filedir.listFiles();
			for(File f : filelist){
				alreadyDownloadFiles.add(f.getName());
			}
			
			Map<String, String> fileList = getFileUrls();

			for (Entry<String, String> file : fileList.entrySet()) {
				String url = file.getKey();
				String filename = file.getValue();

				System.out.println(url + "\t" + filename);
				
				if(alreadyDownloadFiles.contains(filename)){
					continue;
				}
				
				URL fileurl = new URL(WikiConf.HOST_PREFIX + url);
				ReadableByteChannel rbc = Channels.newChannel(fileurl.openStream());

				FileOutputStream fos = null;				
				try{
					fos = new FileOutputStream(WikiConf.OUTPUT_FOLDER + "file/" + filename);
					fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
				}catch(Exception ex){
					ex.printStackTrace();
				}finally{
					try{
						fos.close();
					}catch(Exception ex){
						ex.printStackTrace();
					}
				}
			}
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Get all pages: { URL -> FileName }
	 */
	public static Map<String, String> getPageUrls()
			throws ClientProtocolException, IOException {
		Map<String, String> urls = new HashMap<String, String>();

		HttpClient httpclient = new DefaultHttpClient();

		String pageurl = WikiConf.PAGE_LIST_URL;

		// main
		HttpGet httpget = new HttpGet(pageurl);
		httpget.setHeader("Cookie", WikiConf.Cookie);

		HttpResponse response = httpclient.execute(httpget);
		HttpEntity entity = response.getEntity();

		String html = EntityUtils.toString(entity);

		Document doc = Jsoup.parse(html);

		Elements nameList = doc
				.getElementsByClass("mw-allpages-alphaindexline");

		for (Element e : nameList) {
			String urllistPage = e.childNode(0).attr("href");

			httpget = new HttpGet(WikiConf.HOST_PREFIX + urllistPage);
			httpget.setHeader("Cookie", WikiConf.Cookie);

			response = httpclient.execute(httpget);
			entity = response.getEntity();

			html = EntityUtils.toString(entity);

			Document doc2 = Jsoup.parse(html);

			// table
			Elements pageList = doc2
					.getElementsByClass("mw-allpages-table-chunk");

			// table -> tbody -> tr
			for (Node page : pageList.first().childNode(0).childNodes()) {
				for (int i = 0; i < page.childNodeSize(); i++) {
					String url = page.childNode(i).childNode(0).attr("href");
					String title = page.childNode(i).childNode(0).attr("title");

					urls.put(url, title);
				}
			}
		}

		return urls;
	}

	public static void downloadAllPages() {
		
		File filedir = new File(WikiConf.OUTPUT_FOLDER + "page/");
		if(!filedir.exists()){
			filedir.mkdirs();
		}
				
		try {
			// load already download
			Set<String> alreadyDownloadFiles = new HashSet<String>();
			File[] filelist = filedir.listFiles();
			for(File f : filelist){
				alreadyDownloadFiles.add(f.getName());
			}
			
			Map<String, String> fileList = getPageUrls();

			for (Entry<String, String> file : fileList.entrySet()) {
				String url = file.getKey();
				String filename = file.getValue();
				
				String normalfilename = makeNormalNameforPage(filename);
				
				System.out.println(url + "\t" + filename + "\t" + normalfilename);
				
				if(alreadyDownloadFiles.contains(normalfilename)){
					continue;
				}

				HttpGet httpget = new HttpGet(WikiConf.HOST_PREFIX + url);
				httpget.setHeader("Cookie", WikiConf.Cookie);

				HttpClient httpclient = new DefaultHttpClient();
				HttpResponse response = null;
				try {
					response = httpclient.execute(httpget);
				}catch(Exception ex){
					ex.printStackTrace();
				}
				if(response == null){
					continue;
				}
				
				HttpEntity entity = response.getEntity();

				String html = EntityUtils.toString(entity);

				Document doc = Jsoup.parse(html);

				Elements contentE = doc.getElementsByClass("mw-body");

				byte[] content = contentE.text().getBytes();

				FileOutputStream fos = null;
				try{
					fos = new FileOutputStream(WikiConf.OUTPUT_FOLDER + "page/" + normalfilename);
					fos.write(content, 0, content.length);
				}catch(Exception ex){
					ex.printStackTrace();
				}finally{
					try{
						fos.close();
					}catch(Exception ex){
						ex.printStackTrace();
					}
				}
			}
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static String makeNormalNameforPage(String pageTitle){
		String normalfilename = pageTitle.replace("\\\\", "-bsl-")
				.replace(" ", "-sp-").replace("/", "-sl-")
				.replace(":", "-se-")
				+ ".txt";
		return normalfilename;
	}

	public static void main(String[] args) {
		if(WikiConf.HOST_PREFIX.isEmpty()){
			System.out.println("plz config in WikiConf");
			System.exit(0);
		}

	//	downloadAllFiles();
		
		downloadAllPages();
	}
}
