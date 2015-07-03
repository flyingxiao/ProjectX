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
import java.net.UnknownHostException;
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
import org.xbill.DNS.DClass;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.Section;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import cn.cnnic.registry.language.LanguageUtil;
import cn.cnnic.registry.language.exceptions.PunyException;

public class DomainARecord implements Callable<String> {

	private String domainName;

	private static ExecutorService threadPool = null;
	private static RandomAccessFile rf;
	private static BufferedWriter bufferedWriter;
	private static String offsetFilePath;

	private static final Log log = LogFactory.getLog(DomainARecord.class);

	public DomainARecord(String domainName) {
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
		
		int corePoolSize = 100;
		int maximumPoolSize = 500;
		long keepAliveTime = 3000;
		
		BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<Runnable>(
				10000);
		RejectedExecutionHandler handler = new ThreadPoolExecutor.CallerRunsPolicy();
		threadPool = new ThreadPoolExecutor(corePoolSize, maximumPoolSize,
				keepAliveTime, TimeUnit.MILLISECONDS, workQueue, handler);
		
		offsetFilePath = basePath + "offset";
		try {
			rf = new RandomAccessFile(basePath + "in", "r");
			bufferedWriter = new BufferedWriter(new FileWriter(basePath
					+ "domain_result", true));
		
			List<String> domainList = null;
		
			while (!isFinished()) {
				log.info("---------------start---------------");
				domainList = readTargets(10000);
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
			if (bufferedWriter != null) {
				try {
					bufferedWriter.close();
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
			DomainARecord task = new DomainARecord(d);
			Future<String> callable = (Future<String>) threadPool.submit(task);
			result.add(callable);
		}

		String domainInfo = null;
		try {
			for (Future<String> callable : result) {
				try {
					domainInfo = (String) callable.get();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
				try {
					bufferedWriter.write(domainInfo);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} finally {
			if (bufferedWriter != null) {
				try {
					bufferedWriter.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public String call() {
		String[] serverName = {"8.8.8.8", "101.226.4.6", "114.114.114.114", "223.6.6.6", "223.5.5.5", "180.76.76.76", "123.125.81.6"};
		
		StringBuilder result = new StringBuilder();
		int a_cnt = 0;
        int aaaa_cnt = 0;
        int mx_cnt = 0;
        int ns_cnt = 0;
        
        Record[] r = null;
        Message reponse = null;
        try {
			Name domain = Name.fromString(domainName, Name.root);
	        SimpleResolver sr = null;
	        Record rec = null;
	        Message query = null;
       
			sr = new SimpleResolver(serverName[(int)(Math.random() * 10 ) % 7]);
		
	        rec = Record.newRecord(domain, Type.ANY, DClass.IN);
	        query = Message.newQuery(rec);
	
	        reponse = sr.send(query);
	        r = reponse.getSectionArray(Section.ANSWER);
        } catch (UnknownHostException e) {
		} catch (TextParseException e) {
		} catch (IOException e) {
		}
        if (r != null) {
	        for (Record re : r) {
	        	if (re.getType() == Type.A) {
	        		a_cnt++;
	        		continue;
	        	}
	        	if (re.getType() == Type.AAAA) {
	        		aaaa_cnt++;
	        		continue;
	        	}
	        	if (re.getType() == Type.MX) {
	        		mx_cnt++;
	        		continue;
	        	}
	        	if (re.getType() == Type.NS) {
	        		ns_cnt++;
	        		continue;
	        	}
	        }
        }
        int rcode = reponse == null ? -1 : reponse.getRcode();
        result.append(domainName).append(",").append(rcode).append(",").append(a_cnt).append(",").append(aaaa_cnt).append(",").append(mx_cnt).append(",").append(ns_cnt).append("\n");

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
//				String target = new String(rf.readLine().getBytes("ISO-8859-1"),"GBK");
				String target = rf.readLine();
				if (target != null && !"".equals(target.trim())) {
					targets.add(target);
//					try {
//						targets.add(LanguageUtil.chinese2Punycode(target));
//					} catch (PunyException e) {
//					}
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
