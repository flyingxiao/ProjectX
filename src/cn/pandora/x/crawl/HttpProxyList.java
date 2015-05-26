package cn.pandora.x.crawl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class HttpProxyList {
	
	private static OutputStreamWriter resultFile = null;
	private static PrintWriter printWriter = null;
	private static String encode = "UTF-8";
	
//	private static String proxyURL = "http://www.kuaidaili.com/free/inha/"; //国内高匿代理
//	private static String fileName = "http-proxy-inha.csv";
	
	//private static String proxyURL = "http://www.kuaidaili.com/free/intr/"; //国内普通代理
	//private static String fileName = "http-proxy-intr.csv";
	
	//private static String proxyURL = "http://www.kuaidaili.com/free/outha/"; //国外高匿代理
	//private static String fileName = "http-proxy-outha.csv";
	
	//private static String proxyURL = "http://www.kuaidaili.com/free/outtr/"; //国外普通代理
	//private static String fileName = "http-proxy-outtr.csv";

	public static void main(String[] args) {
		Map<String, String> proxy = new HashMap<String, String>();
		proxy.put("http-proxy-inha.csv", "http://www.kuaidaili.com/free/inha/");
		proxy.put("http-proxy-intr.csv", "http://www.kuaidaili.com/free/intr/");
		proxy.put("http-proxy-outha.csv", "http://www.kuaidaili.com/free/outha/");
		proxy.put("http-proxy-outtr.csv", "http://www.kuaidaili.com/free/outtr/");
		
		for (String key : proxy.keySet()) {
			getProxyList(key, proxy.get(key));
		}
		
		System.exit(0);
	}
	
	public static void getProxyList(String fileName, String proxyURL) {
		File file = new File(fileName);
		boolean fileExists = false;
		//
		int totalPage = 476;
		int startPage = 1;
		try {
			if (file.exists()) {
				fileExists = true;
				
				//get the start page
				InputStream fis = null;
		        BufferedReader bufferedreader = null;
				try {
			        fis = new FileInputStream(fileName);
		            bufferedreader = new BufferedReader(new InputStreamReader(fis, encode));
		            
		            int len = 0;
		            while (bufferedreader.readLine() != null) {
		                len++;
		            }
		            
		            startPage = len / 15 + 1;
				} catch (Exception e) {
		            e.printStackTrace();
		        } finally {
		            if (bufferedreader != null) {
		                try {
		                    bufferedreader.close();
		                } catch (IOException e) {
		                    e.printStackTrace();
		                }
		            }
		            if (fis != null) {
		                try {
		                    fis.close();
		                } catch (IOException e) {
		                    e.printStackTrace();
		                }
		            }
		        }
	        }
			resultFile = new OutputStreamWriter(new FileOutputStream(file, true), encode);
			printWriter = new PrintWriter(resultFile);
			
			//if file exist, ignore the header
			if (!fileExists) {
				printWriter.write("IP,PORT,匿名度,类型,位置,响应速度,最后验证时间\n");
			}
			
			for (int i = startPage; i <= totalPage; i++) {
				Document doc = null;
				//timeout, retry 5 times
				int retry = 1;
				for (; retry <= 5; retry++) {
					try {
						doc = Jsoup.connect(proxyURL + i).get();
					} catch (SocketTimeoutException e) {
						e.printStackTrace();
						continue;
					}
					break;
				}
				if (doc == null) {
					System.out.println("连续重试超过5次，未成功！跳过此页：" + i + "： " + proxyURL);
					continue;
				}
				
				//get the total page num in the first page
				if (i == startPage) {
					Elements listnav = doc.select("#listnav").select("ul").select("li");
					Element totalEle = listnav.get(listnav.size() - 2); // the one before the last one 
					totalPage = Integer.parseInt(totalEle.text()); // reset the total page
				}
				
				//one page content
				StringBuffer content = new StringBuffer();
				//get the http proxy list in the page
				Elements proxyList = doc.select("#list").select("tbody").select("tr");
				for (Element proxy : proxyList) {
					Elements proxyProperty = proxy.children();
					for (Element property : proxyProperty) {
						content.append(property.text() + ",");
					}
					content.append("\n");
				}
				printWriter.write(content.toString());
				
				System.out.println("Job done : " + (i / totalPage) * 100 + "%; total : " + totalPage + ", done : " + i);
				
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		} finally {
			if (printWriter != null) {
            	printWriter.flush();
                printWriter.close();
            }
            if (resultFile != null) {
                try {
                    resultFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
		}
	}
}
