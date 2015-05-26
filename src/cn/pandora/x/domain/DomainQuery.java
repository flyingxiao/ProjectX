package cn.pandora.x.domain;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.net.InetAddress;
import java.net.Socket;
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

public class DomainQuery implements Callable<String> {

	private String domainName;

	public final static int DEFAULT_PORT = 43;

	private static ExecutorService threadPool = null;
	private static RandomAccessFile rf;
	private static BufferedWriter bufferedWriter_whois;
	private static BufferedWriter bufferedWriter_whois_fail;
	private static String offsetFilePath;

	private static final Log log = LogFactory.getLog(DomainQuery.class);

	public DomainQuery(String domainName) {
		this.domainName = domainName;
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
			DomainQuery task = new DomainQuery(d);
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
		String serverName = "whois.afilias.info";
		String whoisContent = null;
		try {
			int retry = 1;
			for (; retry <= 5; retry++) {
				try {
					whoisContent = queryWhoisServer(domainName, serverName,
							DEFAULT_PORT);
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

	private String queryWhoisServer(String domainName, String serverName,
			int port) throws IOException {
		StringBuilder result = new StringBuilder();
		Socket theSocket = null;
		Writer out = null;
		InputStreamReader in = null;

		try {
			InetAddress server = InetAddress.getByName(serverName);
			// 建立socket
			theSocket = new Socket(server, port);
			// 设置读超时
			theSocket.setSoTimeout(10000);
			// 创建输出流
			out = new OutputStreamWriter(theSocket.getOutputStream(), "UTF-8");
			out.write(domainName + "\r\n");
			out.flush();

			// 缓冲输入流
			in = new InputStreamReader(theSocket.getInputStream(), "UTF-8");

			char[] buffer = new char[1024];
			int chars_read;

			// read until stream closes
			while ((chars_read = in.read(buffer)) != -1) {
				result.append(buffer, 0, chars_read);
			}
		} finally {
			try {
				if (out != null) {
					out.close();
				}
				if (in != null) {
					in.close();
				}
				if (theSocket != null) {
					theSocket.close();
				}
			} catch (IOException e) {
				log.error("IOException_error#" + domainName + "#", e);
			}
		}
		return result.toString();
	}

	private String getTLD(String domain) {
		final int index;
		return (domain == null || (index = domain.lastIndexOf('.') + 1) < 1) ? domain
				: (index < (domain.length())) ? domain.substring(index) : "";
	}

	private String getServer(String domain) {
		String server = "";
		String tld = getTLD(domain);
		tld = tld.toLowerCase();
		if ("com".equals(tld)) {
			server = "whois.verisign-grs.com";
		} else if ("net".equals(tld)) {
			server = "whois.verisign-grs.com";
		} else if ("org".equals(tld)) {
			server = "whois.pir.org";
		} else if ("cn".equals(tld)) {
			server = "whois.cnnic.cn";
		} else if ("jp".equals(tld)) {
			server = "whois.jprs.jp";
		} else if ("kr".equals(tld)) {
			server = "whois.kr";
		} else if ("info".equals(tld)) {
			server = "whois.afilias.info";
		}

		return server;
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
