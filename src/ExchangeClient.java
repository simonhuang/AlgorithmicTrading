import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

public class ExchangeClient {

	static int count = 0;
	static Timer timer;
	static class RunTask extends TimerTask {
		Socket socket;
		PrintWriter pout;
		BufferedReader bin;
		HashMap<String, Double> dividend_ratios;
		
		public RunTask() throws UnknownHostException, IOException {	 
			String host = "codebb.cloudapp.net";    
	        int port =17429 ;
	        String user = "Brain_It_On";
	        String password = "abc1";
	        dividend_ratios = new HashMap<String, Double>();
	        
	        count = 0;
	        
	        socket = new Socket(host, port);
	        pout = new PrintWriter(socket.getOutputStream());
	        bin = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	        pout.println(user + " " + password);
		}

		static class Order{
			public double price;
			public int shares;
			public Order(double price, int shares){
				this.price = price;
				this.shares = shares;
			}
		}
		String get_output(){
			String line = "";
			try {
				line = bin.readLine();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return line;
		}
		
		Order highest_bid(String ticker){
			pout.println("ORDERS " + ticker);
			pout.flush();
			String line = get_output();
			String[] info = line.split(" ");
			
			double highest = 0;
			int shares = 0;
			for (int i=0;i<(info.length - 1)/4;i++){
				String order = info[1 + i*4];
				if (order.equals("BID")){
					double price = Double.parseDouble(info[3 + i*4]);
					if (price > highest){
						highest = price;
						shares = Integer.parseInt(info[4 + i*4]);
					}
				}
			}
			return new Order(highest, shares);
		}
		Order lowest_ask(String ticker){
			pout.println("ORDERS " + ticker);
			pout.flush();
			String line = get_output();
			String[] info = line.split(" ");
			
			double lowest = Double.MAX_VALUE;
			int shares = 0;
			for (int i=0;i<(info.length - 1)/4;i++){
				String order = info[1 + i*4];
				if (order.equals("ASK")){
					double price = Double.parseDouble(info[3 + i*4]);
					if (price < lowest){
						lowest = price;
						shares = Integer.parseInt(info[4 + i*4]);
					}
				}
			}
			return new Order(lowest, shares);
		}
		
		int shares(PrintWriter pout, String ticker) {
			pout.println("MY_SECURITIES");
			pout.flush();
			String line = get_output();
			String[] info1 = line.split(" ");
			
			int shares_owned = 0;
			for (int i=0;i<(info1.length - 1)/3;i++){
				String cur_ticker = info1[1 + i*3];
				if (cur_ticker.equals(ticker)){
					shares_owned = Integer.parseInt(info1[2 + i*3]);
					break;
				}
			}
			return shares_owned;
		}
		
		int num_orders(){
			pout.println("MY_ORDERS");
			pout.flush();
			String line = get_output();
			String[] info = line.split(" ");
			return (info.length - 1) / 4;
		}

		
		double net_worth(PrintWriter pout, String ticker){
			pout.println("SECURITIES");
	        pout.flush();
	        String line = get_output();
			String [] info = line.split(" ");
			for (int i=0;i<(info.length-1)/4;i++){
				String cur_ticker = info[1 + i * 4];
				if (ticker.equals(cur_ticker)){
					Double net_worth = Double.parseDouble(info[2 + i * 4]);
					return net_worth;
				}
			}
			return -1;
		}
		
		double my_cash(PrintWriter pout){
			pout.println("MY_CASH");
			pout.flush();
			String line = get_output();
			String[] info = line.split(" ");
			return Double.parseDouble(info[1]);
		}
		
		boolean buy1(PrintWriter pout, String ticker){
			while (true){
				System.out.println("ggerino");
				Order ask = lowest_ask(ticker);
				double bid_price = ask.price + 0.1;
				int orders_before = num_orders();
				pout.println("BID " + ticker + " " + bid_price + " 1");
				pout.flush();
				get_output();
				int orders_after = num_orders();
				
				if (orders_before != orders_after){
					pout.println("CLEAR_BID " + ticker);
					pout.flush();
				} else {
					break;
				}
			}
			
			return true;
		}
		
		boolean buy(PrintWriter pout, String ticker, double cash){
			double total_cash = cash;
			while (total_cash > 0){
				Order ask = lowest_ask(ticker);
				
				int shares = 0;
				
				double bid_price = ask.price + 0.1;
				
				if (bid_price * ask.shares > total_cash){
					shares = (int) (total_cash / bid_price);
				} else {
					shares = ask.shares;
				}
				int orders_before = num_orders();
				pout.println("BID " + ticker + " " + bid_price + " " + shares);
				pout.flush();
				get_output();
				int orders_after = num_orders();
				
				if (orders_before != orders_after){
					pout.println("CLEAR_BID " + ticker);
					pout.flush();
					get_output();
				} else {
					total_cash -= bid_price * ask.shares;
				}
			}
			return true;
		}
		
		boolean sell1(PrintWriter pout, String ticker){
			while (true){
				Order bid = highest_bid(ticker);
				double ask_price = bid.price - 0.1;
				int orders_before = num_orders();
				pout.println("ASK " + ticker + " " + ask_price + " 1");
				pout.flush();
				get_output();
				int orders_after = num_orders();
				
				if (orders_before != orders_after){
					pout.println("CLEAR_ASK " + ticker);
					pout.flush();
					get_output();
				} else {
					break;
				}
			}
			return true;
		}
		
		
		boolean sell(PrintWriter pout, String ticker, int shares){
			int i = 0;
			
			while (shares > 0 && i < 100){
				i++;
				//Order ask = lowest_ask(ticker);
				
				Order bid = highest_bid(ticker);
				
				
				double ask_price = bid.price - 0.1;
				
				int s;
				if (bid.shares > shares)
					s = shares;
				else
					s = bid.shares;
				
				int orders_before = num_orders();
				pout.println("ASK " + ticker + " " + ask_price + " " + s);
				pout.flush();
				get_output();
				int orders_after = num_orders();
				
				if (orders_before != orders_after){
					pout.println("CLEAR_BID " + ticker);
					pout.flush();
					get_output();
				} else {
					shares -= s;
				}
			}
			return true;
		}
		
		
		double sell_profit(PrintWriter pout, String ticker){
			
			pout.println("MY_SECURITIES");
			pout.flush();
			String line = get_output();
			String[] info1 = line.split(" ");
			
			int shares_owned = 0;
			for (int i=0;i<(info1.length - 1)/3;i++){
				String cur_ticker = info1[1 + i*3];
				if (cur_ticker.equals(ticker)){
					shares_owned = Integer.parseInt(info1[2 + i*3]);
					break;
				}
			}
			System.out.println("shares owned: " + shares_owned);
			pout.println("ORDERS " + ticker);
			pout.flush();
			line = get_output();
			String[] info = line.split(" ");
			
			int bid_count = 0;
			for (int i=0;i<(info.length - 1)/4;i++){
				String order = info[1 + i*4];
				if (order.equals("BID")){
					bid_count ++;
				}
			}
			
			int [] shares = new int [bid_count];
			double [] prices = new double [bid_count];
			int order_index = 0;
			for (int i=0;i<(info.length - 1)/4;i++){
				String order = info[1 + i*4];
				if (order.equals("BID")){
					shares[order_index] = Integer.parseInt(info[4 + i*4]);
					prices[order_index++] = Double.parseDouble(info[3 + i*4]);
				}
			}
			
			for (int i=0;i<prices.length;i++){
				double max_price = Double.MIN_VALUE;
				int max_index = 0;
				for (int j=i;j<prices.length;j++){
					if (max_price < prices[j]){
						max_index = j;
						max_price = prices[j];
					}
				}
				if (max_index != i){
					int shares_temp = shares[max_index];
					double prices_temp = prices[max_index];
					shares[max_index] = shares[i];
					prices[max_index] = prices[i];
					shares[i] = shares_temp;
					prices[i] = prices_temp;
				}
			}
			
			double profit = 0;
			for (int i=0;i<prices.length;i++){
				if (shares_owned <= shares[i]){
					profit += shares_owned * prices[i];
					System.out.println("profit: " + profit);
					break;
				} else {
					profit += shares[i] * prices[i];
					shares_owned -= shares[i];
					System.out.println("profit: " + profit);
				}
			}
			
			return profit;
		}
		
		int num_buy1(PrintWriter pout, String ticker, double sell_profit){
			pout.println("MY_SECURITIES");
			pout.flush();
			String line = get_output();
			String[] info1 = line.split(" ");
			
			int shares_owned = 0;
			for (int i=0;i<(info1.length - 1)/3;i++){
				String cur_ticker = info1[1 + i*3];
				if (cur_ticker.equals(ticker)){
					shares_owned = Integer.parseInt(info1[2 + i*3]);
					break;
				}
			}
			System.out.println("shares owned: " + shares_owned);
			pout.println("ORDERS " + ticker);
			pout.flush();
			line = get_output();
			String[] info = line.split(" ");
			
			int ask_count = 0;
			for (int i=0;i<(info.length - 1)/4;i++){
				String order = info[1 + i*4];
				if (order.equals("ASK")){
					ask_count ++;
				}
			}
			
			int [] shares = new int [ask_count];
			double [] prices = new double [ask_count];
			int order_index = 0;
			for (int i=0;i<(info.length - 1)/4;i++){
				String order = info[1 + i*4];
				if (order.equals("ASK")){
					shares[order_index] = Integer.parseInt(info[4 + i*4]);
					prices[order_index++] = Double.parseDouble(info[3 + i*4]);
				}
			}
			
			for (int i=0;i<prices.length;i++){
				double min_price = Double.MAX_VALUE;
				int min_index = 0;
				for (int j=i;j<prices.length;j++){
					if (min_price > prices[j]){
						min_index = j;
						min_price = prices[j];
					}
				}
				if (min_index != i){
					int shares_temp = shares[min_index];
					double prices_temp = prices[min_index];
					shares[min_index] = shares[i];
					prices[min_index] = prices[i];
					shares[i] = shares_temp;
					prices[i] = prices_temp;
				}
			}
			
			int buy_num = 0;
			
			double cash = sell_profit + my_cash(pout);
			for (int i=0;i<prices.length;i++){
				if (cash <= shares[i] * prices[i]){
					buy_num += cash / prices[i];
					break;
				} else {
					buy_num += shares[i];
					cash -= shares[i] * prices[i];
				}
			}
			return buy_num;
		}
		
		static double cash1 = 0;
		static double nw1 = 0;

		static double cash2 = 0;
		static double nw2 = 0;

		static double cash3 = 0;
		static double nw3 = 0;

		static double cash4 = 0;
		static double nw4 = 0;
		
		static double cash_difference = 0;
		static double cash_difference2 = 0;
		static double cash_difference3 = 0;
		
		static double r = 0;
		static String ticker = "MMM";
		static boolean finished = true;
		static String[] ticker_array = new String[10];
		static int [] ticker_shares = new int[10];
		
		static int[] decay = new int[11];
		static int currentStock = 11;
	
		static double sell_profit;
		static int num_buy;
		static boolean fuck = false;
		static int pre_count = 0;
		
		public void run() {
			if (pre_count < 30){
				pre_count ++;
				return;
			}
//			if (fuck){
//				return;
//			}
			if (!finished){
				return;
			}
			finished = false;
			count ++;
			
			/*if (count == 1) {
				pout.println("BID MSFT 100 5");
				pout.flush();
				System.out.println(get_output());
				pout.println("MY_SECURITIES");
				pout.flush();
				System.out.println(get_output());
			}
			else if (count == 2){ 
				sell(pout,"MSFT",shares(pout,"MSFT"));
			}
			else {
				pout.println("MY_SECURITIES");
				pout.flush();
				System.out.println(get_output());
				pout.println("CLOSE_CONNECTION");
				pout.flush();
			}*/
			
			if (count == 1){

	        	pout.println("SECURITIES");
		        pout.flush();
		        String line = get_output();
				String [] info = line.split(" ");
				for (int i=0;i<(info.length-1)/4;i++){
					String ticker = info[1 + i * 4];
					
					ticker_array[i] = ticker;
					
					Double dividend_ratio = Double.parseDouble(info[3 + i * 4]);
					dividend_ratios.put(ticker, dividend_ratio);
				}
			}
			for (int j = 0; j < 10; j++) {
				
				String ticker = ticker_array[j];
				if (count == j*3+1) {
					
					
					buy1(pout, ticker);
					nw1 = net_worth(pout, ticker);
					cash1 = my_cash(pout);
					System.out.println(System.nanoTime());
					System.out.println(cash1 + " " + nw1);
					
					double cash2 = my_cash(pout);
					while (cash1 == cash2){
						cash2 = my_cash(pout);
					}
					
					cash_difference = cash2 - cash1;
					break;
					
				} else if (count == j*3+2){
					
					cash2 = my_cash(pout);
					nw2 = net_worth(pout, ticker);
					System.out.println(cash2 + " " + nw2);
					System.out.println(dividend_ratios.get(ticker));
					
					double shares = dividend_ratios.get(ticker) * nw2 / cash_difference;
					ticker_shares[j] = (int) shares;
					break;
					
				} else if (count == j*3+3){
					cash3 = my_cash(pout);
					nw3 = net_worth(pout, ticker);
					cash_difference2 = cash3 - cash2;
					r = (cash_difference2/nw3) / (cash_difference/nw2);
					if (r < 0.95){
						fuck = true;
					}
					System.out.println(r);
					
					sell1(pout,ticker);
					break;
				} 
			}
			
			int canBuy = 0;
			//static int[] decay = new int[11];
			
			double max = -1;
			int maxIndex = 0;
			
			
			// double r = 0;
			if (count % 60 == 0 && count > 31) {
				for (int j = 0; j < 10; j++) {
					if (currentStock == 11)
						num_buy = num_buy1(pout,ticker_array[j],0);
					else
						num_buy = num_buy1(pout,ticker_array[j],sell_profit(pout,ticker_array[currentStock]));
					
					double tmp = num_buy * dividend_ratios.get(ticker_array[j]) * net_worth(pout, ticker_array[j]) / ticker_shares[j] * Math.pow(r,decay[j]);

					System.out.println("j: " + j);
					System.out.println("num_buy: " + num_buy);
					System.out.println("dividends: " + dividend_ratios.get(ticker_array[j]) * net_worth(pout, ticker_array[j]) / ticker_shares[j] );
					System.out.println("decay[j]: " + decay[j]);
					System.out.println("tmp" + tmp);
					
					if (tmp > max){
						max = tmp;
						maxIndex = j;
					}
					
				}
				
				System.out.println("current stock: " + currentStock);
				System.out.println("max index: " + maxIndex);
				System.out.println("max: " + max);
				
				if (currentStock != maxIndex) {
					if (currentStock != 11) {
						System.out.println("current ticket: " + ticker_array[currentStock]);
						System.out.println("shares currently owed: " + shares(pout,ticker_array[currentStock]));
					}

					System.out.println("new ticket: " + ticker_array[maxIndex]);
					
					if (currentStock != 11)
						sell(pout,ticker_array[currentStock],shares(pout,ticker_array[currentStock]));
					buy (pout,ticker_array[maxIndex],my_cash(pout));
					pout.println("MY_SECURITIES");
					pout.flush();
					System.out.println("***" + get_output());
				}
				currentStock = maxIndex;
				//change decay
				for (int j = 0; j < 10; j++) {
					if (j == currentStock) {
						decay[j] += 60;
					}
					else if (decay[j] > 0) {
						decay[j] -= 60;
						if (decay[j] < 0)
							decay[j] = 0;
					}
				}
				System.out.println(my_cash(pout));
			}
			
			
		    if (count == 3001){
		    	for (int i=0;i<ticker_shares.length;i++){
		    		System.out.println(ticker_shares[i]);
		    	}
		    	pout.println("CLOSE_CONNECTION");
				pout.flush(); 	
				pout.close();
				try {
					bin.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				timer.cancel();
				timer.purge();
		    }

			finished = true;
		}
		
	}
	
    public static void main(String[] args) throws IOException {
    	Scanner sc = new Scanner(System.in);
		System.out.println("ok");
    	String command = sc.nextLine();
    	
    	if(command.equals("RUN")){
	    	timer = new Timer();
	    	timer.schedule(new RunTask(), 0, 1000);
	    	
    	} else {
    		String host = "codebb.cloudapp.net";    
			int port = 17429 ;
			String user = "Brain_It_On";
			String password = "abc1";
			String[] commands = {"MY_CASH"};
			
			
			Socket socket = new Socket(host, port);
			PrintWriter pout = new PrintWriter(socket.getOutputStream());;
			BufferedReader bin = new BufferedReader(new InputStreamReader(socket.getInputStream()));;
			pout.println(user + " " + password);	
			
			while(!command.equals("END")){
				command = sc.nextLine();
				if (command.equals("END")){
					break;
				}
				System.out.println(command);
	
				pout.println(command);
				pout.flush();
				String line = bin.readLine();
			    System.out.println(line);
			}
			pout.println("CLOSE_CONNECTION");
			pout.flush();
			pout.close();
			bin.close();
    	}
    }
}