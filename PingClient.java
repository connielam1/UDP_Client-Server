import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;
import java.lang.*;
import java.awt.Toolkit;

/* 
 * Client to send ping requests over UDP.
 */
    
//*USAGE*: java PingClient host port passwd

public class PingClient{
	Toolkit toolkit;
    Timer timer;

    public PingClient(InetAddress serverIPAd, int port, String passw) {
		toolkit = Toolkit.getDefaultToolkit();
	    timer = new Timer();
	    timer.schedule(new RemindTask(serverIPAd, port, passw), 0, 1000); // initial delay, subsequent rate

    }

    class RemindTask extends TimerTask {
    	short sequence_number;
    	DatagramSocket clientSocket;
    	String passwd;
    	InetAddress serverIPAddress;
    	int port;
    	ArrayList<Long> rtts ;
    	int num_lost;

        public RemindTask(InetAddress serverIPAd, int port_num, String passw){

        	passwd = passw;
        	serverIPAddress = serverIPAd;
        	port = port_num;

	        sequence_number = 0;
			try {
				// Create a datagram socket for sending UDP packets to Server
				clientSocket = new DatagramSocket();
				System.out.println("created socket");
			}
			catch (SocketException ex)
			{
				System.out.println(ex);
			    System.exit(0);
			}

			// Report minimum, maximum, and average RTTs -- Floats?
			rtts = new ArrayList<Long>();
			num_lost = new Integer(0);

        }


        public void run() {
            if (sequence_number < 10) {
                
                sequence_number++;

                // Ping message format: PING sequence_number client_send_time passwd CRLF
				long client_send_time = System.currentTimeMillis();
				String sentence = String.format("PING %s %s %s CRLF", String.valueOf(sequence_number), Long.toString(client_send_time), passwd);

	            ByteBuffer b = ByteBuffer.allocate(19+ passwd.length());
	            b = b.order(ByteOrder.BIG_ENDIAN);
	            b.put(String.format("PING").getBytes());
	            b.put(String.format(" ").getBytes());
	            b.putShort(sequence_number);
	            b.put(String.format(" ").getBytes());
	            b.putLong(client_send_time);
	            b.put(String.format(" ").getBytes());
	            b.put(passwd.getBytes());
	            b.put(String.format("\r\n").getBytes());

				byte[] sendData = b.array();
				String temp = new String(sendData);
				System.out.println("sendData length = " + sendData.length);
				System.out.println("sendData message = " + sentence);

				// Create datagram packet
				DatagramPacket message = new DatagramPacket(sendData, sendData.length, serverIPAddress, port); 
				long client_receive_time = 0;	
				
				try {
					// Create a datagram socket for sending UDP packets to Server
					clientSocket.send(message);
					client_receive_time = 0;
					System.out.println("MESSAGE SENT!");
				}
				catch (IOException ex)
				{
					System.out.println(ex);
				    System.exit(0);
				}
				
				// Wait for a reply; Timeout value for datagram socket - 1 second
				try{
					clientSocket.setSoTimeout(1000); // wait for response
					while (true) {
				    	try{
				    		byte[] receiveData = new byte[1024];
							DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
				    		clientSocket.receive(receivePacket);

				    		//Get receive time
				       		client_receive_time = System.currentTimeMillis();

							// Parse response
							byte[] reply = receivePacket.getData();
							String sentenceFromServer = new String(reply);

							System.out.println("From Server: " + sentenceFromServer);

				            ByteBuffer bb = ByteBuffer.allocate(2);
							bb.order(ByteOrder.BIG_ENDIAN);
							bb.put(reply[9]);
							bb.put(reply[10]);
							short seq_num = bb.getShort(0);
				            System.out.println("sequence_number: " + seq_num);

		       				// validate sequence number 
							if(seq_num != sequence_number){
								// continue in while loop; keep waiting til correct packet
								System.out.println("Wrong packet number");		
							}
							else{
								//Get RTT and add to arraylist
								Long curRTT = new Long(client_receive_time - client_send_time);
								System.out.println("Current RTT: " + curRTT);
								rtts.add(curRTT);
								break;
							}  
				    	}
						catch(IOException ex){
				    		// Packet lost, continue and send next packet
				    		num_lost++;
							System.out.println(ex);
							break;
			    		} 

	          		}				
				}
				catch(SocketException ex){
					System.out.println(ex);
					System.exit(0);
				}
            } else {
 
                System.out.println("Close Socket!");
                // close socket
   				clientSocket.close();

		   		// calculate minimum, maximum, and average RTTs
		   		Long minimum;
		   		Long maximum;
		   		if (rtts.size()> 0){
			   		minimum = Collections.min(rtts);
			    	maximum = Collections.max(rtts);
		   		}
		   		else{
		   			minimum = new Long(0);
		   			maximum = new Long(0);
		   		}

		    	Long sum = new Long(0);
		    	double averageRTT = 0;
		    	for (Long rtt : rtts){
		    		sum+= rtt;
		    	}
		    	averageRTT = sum.doubleValue()/10;
		    	Double lossrate = (double) num_lost/10;

				System.out.println("Loss Rate: " + lossrate);
		    	System.out.println("Minimum RTT: " + minimum);
		    	System.out.println("Maximum RTT: " + maximum);
		    	System.out.println("Average RTT: " + averageRTT);
				
                System.exit(0);   // Stops the AWT thread 
                                  // (and everything else)
            }
        }
    }

    // sends 10 pings
    public static void main(String args[]) throws Exception{

    	// Get command line argument.
    	if (args.length != 3) {
        	System.out.println("Required arguments: host port passwd");
        	return;
    	}

    	// Get server address
		String host = args[0]; 

		// Translate hostname to IP address using DNS
		InetAddress serverIPAddress = InetAddress.getByName(host);

		// Get server port
		int port_num = Integer.parseInt(args[1]);

		// Get password
		String passwd = args[2]; 

		// Start scheduling of 10 pings, one ping per second
		System.out.println("About to schedule pings.");
	    new PingClient(serverIPAddress, port_num, passwd);
		System.out.println("Pings scheduled.");

    }

}