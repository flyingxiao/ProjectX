package cn.xiao.crawl;

public class Test {
	
	public static void main(String[] args) {
		int loop = 5;
		for (int i = 0; i < loop; i++) {
			if (i == 4) {
				loop = 10;
			}
			if (i == 7) {
				loop = 5;
			}
			System.out.println(i);
		}
	}
}
