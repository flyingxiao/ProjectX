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

import cn.cnnic.registry.language.LanguageUtil;
import cn.cnnic.registry.language.exceptions.PunyException;

public class WhoisResultAnalysis implements Callable<String> {

	private String whoisInfo;

	private static ExecutorService threadPool = null;
	private static RandomAccessFile rf;
	private static BufferedWriter bufferedWriter_whois;
	private static String offsetFilePath;
	private String[] properties = {"Creation Date:", "Registry Expiry Date:", "Sponsoring Registrar:", "Sponsoring Registrar IANA ID:", "Registrant ID:", "Registrant Name:", "Registrant Organization:", "Registrant City:", "Registrant Country:", "Registrant Email:", "Registrant Phone:", "Registrant Fax:"};

	private static final Log log = LogFactory.getLog(WhoisResultAnalysis.class);

	public WhoisResultAnalysis(String whoisInfo) {
		this.whoisInfo = whoisInfo;
	}
	
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
		
		int corePoolSize = 20;
		int maximumPoolSize = 20;
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
					+ "whois_info", true));
		
			List<String> whoisList = null;
		
			while (!isFinished()) {
				log.info("---------------start---------------");
				whoisList = readTargets(10000);
				execute(whoisList);
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
		}
		
		log.info("done");
	}
	
	public static void execute(List<String> whoisList) {
		List<Future<String>> result = new ArrayList<Future<String>>();
		for (String w : whoisList) {
			WhoisResultAnalysis task = new WhoisResultAnalysis(w);
			Future<String> callable = (Future<String>) threadPool.submit(task);
			result.add(callable);
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
		}
	}

	@Override
	public String call() {
		String[] whoisContent = whoisInfo.split("\n");
		String punycode = whoisContent[0].replace("Punycode:", "").trim();
		String domainName = null;
		try {
			domainName = LanguageUtil.punycode2Chinese(punycode);
		} catch (PunyException e) {
			e.printStackTrace();
		}
		StringBuilder result = new StringBuilder();
		result.append(domainName).append(",").append(punycode).append(",");
		
		for (String property : properties) {
			String itemValue = null;
			for (String item : whoisContent) {
				if (item == null || "".equals(item)) {
					continue;
				}
				if (item.startsWith(property)) {
					itemValue = item.replace(property, "").replace(",", "").trim();
					break;
				}
			}
			result.append(itemValue == null ? "" : itemValue).append(",");
		}
		
		result.append("\n");
		
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
			String line = rf.readLine();
			for (int i = 0; i < number; i++) {
				if (line == null) { //file over
					break;
				}
				int count = 0;
				StringBuffer target = new StringBuffer();
				while (line != null) { //文件尾
					
					line = new String(line.getBytes("ISO-8859-1"),"GBK");
					
					if (count == 0 && !line.startsWith("Punycode: ")) {
						position = rf.getFilePointer();
						line = rf.readLine();
						continue;
					}
					if (line.startsWith("Punycode: ") && count > 0) {//上一个域名结束
						break;
					}
					target.append(line).append("\n");
					
					position = rf.getFilePointer();
					line = rf.readLine();
					count++;
				}
				targets.add(target.toString());
			}
		} catch (IOException e) {
			throw e;
		} finally {
			try {
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
