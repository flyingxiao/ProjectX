package cn.pandora.x.domain;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class WhoisWeb implements Callable<String> {

	private String domainName;

	public final static int DEFAULT_PORT = 43;

	private static ExecutorService threadPool = null;
	private static RandomAccessFile rf;
	private static BufferedWriter bufferedWriter_whois;
	private static BufferedWriter bufferedWriter_whois_fail;
	private static String offsetFilePath;

	private static final Log log = LogFactory.getLog(WhoisWeb.class);

	public WhoisWeb(String domainName) {
		this.domainName = domainName;
	}
	
//	public static void main(String[] args) throws IOException {
//		String dn = "2723byron.info";
//		WhoisWeb ww = new WhoisWeb(dn);
//		ww.queryWhoisServer(dn);
//	}
	
	public static void main(String[] args) throws InterruptedException,	ExecutionException {
		String basePath = null;
		if (args.length > 0) {
			basePath = args[0];
		}
		
		if (basePath == null) {
			basePath = "";
		}
		
		if (!basePath.equals("")) {
			if (!basePath.endsWith("/")) {
				basePath += "/";
			}
		}
		
		int corePoolSize = 1;
		int maximumPoolSize = 1;
		long keepAliveTime = 3000;
		
		BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<Runnable>(
				10000);
		RejectedExecutionHandler handler = new ThreadPoolExecutor.CallerRunsPolicy();
		threadPool = new ThreadPoolExecutor(corePoolSize, maximumPoolSize,
				keepAliveTime, TimeUnit.MILLISECONDS, workQueue, handler);
		
		offsetFilePath = basePath + "offset";
		try {
			rf = new RandomAccessFile(basePath + "in", "r");
			bufferedWriter_whois = new BufferedWriter(new FileWriter(basePath
					+ "whois_result", true));
			bufferedWriter_whois_fail = new BufferedWriter(new FileWriter(basePath
					+ "whois_fail", true));
		
			List<String> domainList = null;
		
			while (!isFinished()) {
				log.info("---------------start---------------");
				domainList = readTargets(10);
				execute(domainList);
				log.info("---------------end---------------");
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			threadPool.shutdown();
			if (rf != null) {
				try {
					rf.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (bufferedWriter_whois != null) {
				try {
					bufferedWriter_whois.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (bufferedWriter_whois_fail != null) {
				try {
					bufferedWriter_whois_fail.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		log.info("done");
	}
	
	public static void execute(List<String> domainList) {
		List<Future<String>> result = new ArrayList<Future<String>>();
		for (String d : domainList) {
			WhoisWeb task = new WhoisWeb(d);
			Future<String> callable = (Future<String>) threadPool.submit(task);
			result.add(callable);
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		String whoisInfo = null;
		try {
			for (Future<String> callable : result) {
				try {
					whoisInfo = (String) callable.get();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
				if (whoisInfo == null) {
					continue;
				}
				try {
					bufferedWriter_whois.write(whoisInfo);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} finally {
			if (bufferedWriter_whois != null) {
				try {
					bufferedWriter_whois.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (bufferedWriter_whois_fail != null) {
				try {
					bufferedWriter_whois_fail.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public String call() {
		String whoisContent = null;
		try {
			int retry = 1;
			for (; retry <= 5; retry++) {
				try {
					whoisContent = queryWhoisServer(domainName);
				} catch (SocketTimeoutException e) {
					continue;
				}
				break;
			}
			if (whoisContent == null) {
				System.out.println("Read timed out #" + domainName + "#");
				bufferedWriter_whois_fail.write(domainName+ "\n");
			}
		} catch (Exception e) {
			System.out.println(e.getMessage() + " #" + domainName + "#");
			try {
				bufferedWriter_whois_fail.write(domainName+ "\n");
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		return whoisContent;
	}

	private String queryWhoisServer(String domainName) throws IOException {
		String result = null;
		
		CloseableHttpClient httpclient = HttpClients.createDefault();

		try {
			HttpHost target = new HttpHost("www.markmonitor.com");
			//https://www.markmonitor.com/cgi-bin/affsearch.cgi?dn=2723byron.info

			// 创建httpget.
			HttpGet httpget = new HttpGet("/cgi-bin/affsearch.cgi?dn=" + domainName);
			
			httpget.setHeader("User-Agent",
			 "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US) AppleWebKit/534.13 (KHTML, like Gecko) Chrome/9.0.597.0 Safari/534.13");

			// 执行get请求.
			CloseableHttpResponse response = httpclient.execute(target, httpget);

			try {
				// 获取响应实体
				HttpEntity entity = response.getEntity();
				if (entity != null) {
					Document doc = Jsoup.parse(EntityUtils.toString(entity, "UTF-8"));
					Element link = doc.select("PRE").first();
					result = link.text();
				}
			} finally {
				response.close();
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			// 关闭连接,释放资源
			try {
				httpclient.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return result.toString();
	}

	public static boolean isFinished() throws IOException {
		long position = 0;
		position = getOffset(offsetFilePath);
		if (position >= rf.length()) {
			return true;
		}
		return false;
	}

	public static List<String> readTargets(int number) throws IOException {
		List<String> targets = new ArrayList<String>();
		Long position = getOffset(offsetFilePath);
		log.info("偏移量：" + position);
		try {
			rf.seek(position);
			for (int i = 0; i < number; i++) {
				String target = rf.readLine();
				if (target != null && !"".equals(target.trim())) {
					targets.add(target);
				}
			}
		} catch (IOException e) {
			throw e;
		} finally {
			try {
				position = rf.getFilePointer();
				// 将文件的当前偏移量保存到本地文件
				recordOffset(position);
			} catch (IOException e) {
				throw e;
			}
		}
		return targets;
	}

	/**
	 * 
	 * @return 当前文件偏移量
	 * @throws IOException
	 * 
	 */
	public static Long getOffset(String filename) throws IOException {
		Long offset = 0L;
		File file = new File(filename);
		if (!file.exists()) {
			return offset;
		}
		InputStream in = null;
		InputStreamReader reader = null;
		BufferedReader br = null;
		try {
			in = new FileInputStream(filename);
			reader = new InputStreamReader(in);
			br = new BufferedReader(reader);

			String line = "";
			while ((line = br.readLine()) != null) {
				offset = Long.valueOf(line);
			}
		} catch (IOException e) {
			throw e;
		} finally {
			try {
				in.close();
				reader.close();
				br.close();
			} catch (IOException e) {
				throw e;
			}
		}
		return offset;
	}

	/**
	 * 记录文件的偏移量
	 * 
	 * @param position
	 *            偏移量文件路径
	 * @throws IOException
	 * 
	 */
	private static void recordOffset(Long position) throws IOException {
		String content = String.valueOf(position);
		BufferedWriter bufferedWriter = null;
		try {
			bufferedWriter = new BufferedWriter(new FileWriter(offsetFilePath));
			bufferedWriter.write(content);
			bufferedWriter.newLine();
		} catch (IOException ex) {
			throw ex;
		} finally {
			try {
				bufferedWriter.close();
			} catch (IOException ex) {
				throw ex;
			}
		}
	}
}
