// PingServer.java
import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;
import java.nio.charset.*;

/* 
 * Server to process ping requests over UDP.
 */

// current cmd line args: java PingServer port
// *GOAL USAGE*: java PingServer port passwd [-delay delay] [-loss loss]


public class PingServer
{
    private static final double LOSS_RATE = 0.3;
    private static final int AVERAGE_DELAY = 100; // milliseconds

    public static void main(String[] args) throws Exception
    {
        // Get command line argument.
        if (args.length < 2) {
            System.out.println("Required arguments: port passwd");
            return;
        }

        int port = Integer.parseInt(args[0]);
        String passwd = args[1]; 
        String delay = new String("");
        String loss = new String("");

        if (args.length > 2){
            for(int i = 2; i< args.length; i++){
                if (args[i].equals("-delay") && (i + 1) < args.length){
                    // make sure it's an int greater than 0 // milleseconds
                    delay = args[i+1]; 
                    i++;
                }
                else if (args[i].equals("-loss") && (i + 1) < args.length){
                    // make sure it's a double between 0 and 1
                    loss = args[i+1];
                    i++;
                }
                else{
                    // Error in Usage 
                    System.out.println("USAGE: java PingServer port passwd [-delay delay] [-loss loss]");
                    return;
                }
            }
        }
        // If loss is empty string, then use LOSS_RATE
        double loss_val;
        if (loss.equals("")){
            loss_val = LOSS_RATE;
        }  
        else{
            loss_val = Double.parseDouble(loss);
        }

        // If delay is empty string, then AVERAGE_DELAY
        int delay_val;
        if (delay.equals("")){
            delay_val = AVERAGE_DELAY;
        }  
        else{
            delay_val = Integer.parseInt(delay);
        }

        System.out.println(port); // For debugging purposes
        System.out.println(passwd); // For debugging purposes
        System.out.println(loss_val); // For debugging purposes
        System.out.println(delay_val); // For debugging purposes

        // Create random number generator for use in simulating
        // packet loss and network delay.
        Random random = new Random();

        // Create a datagram socket for receiving and sending
        // UDP packets through the port specified on the
        // command line.
        DatagramSocket socket = new DatagramSocket(port);

        // Processing loop.
        while (true) {

            // Create a datagram packet to hold incomming UDP packet.
            DatagramPacket
            request = new DatagramPacket(new byte[1024], 1024);

            // Block until receives a UDP packet.
            socket.receive(request);

            // Print the received data, for debugging
            printData(request);

            // Decide whether to reply, or simulate packet loss.
            if (random.nextDouble() < loss_val) {
                System.out.println(" Reply not sent.");
                continue;
            }

            // Simulate prorogation delay.
            Thread.sleep((int) (random.nextDouble() * 2 * delay_val));

            // Send reply.
            InetAddress clientHost = request.getAddress();
            int clientPort = request.getPort();
            byte[] buf = request.getData();

            // Turn byte array
            String s = new String(buf);
            String[] chunks = s.split("\\s+");
            // System.out.println(chunks[3]);
            // Check if password of received packet EQUALS the necessary password
            if (!chunks[3].equals(passwd)){
                // Password Incorrect
                // validate password
                System.out.println("PASSWORD INCORRECT");
                continue;
            }
            else{
                // Reformat sentence, then send back
                // new data format: PINGECHO sequence_number client_send_time passwd CRLF
                ByteBuffer b = ByteBuffer.allocate(buf.length + 4);
                b.order(ByteOrder.BIG_ENDIAN);
                b.put(String.format("PINGECHO").getBytes());
                b.put(Arrays.copyOfRange(buf, 4, buf.length));
                byte[] sendData = b.array();

                DatagramPacket reply = new DatagramPacket(sendData, sendData.length, clientHost, clientPort);

                socket.send(reply);
                
                System.out.println(" Reply sent.");
            }

        } // end of while
    } // end of main

    /* 
    * Print ping data to the standard output stream.
    */
    private static void printData(DatagramPacket request) 
           throws Exception

    {
        // Obtain references to the packet's array of bytes.
        byte[] buf = request.getData();

        // Wrap the bytes in a byte array input stream,
        // so that you can read the data as a stream of bytes.
        ByteArrayInputStream bais 
        = new ByteArrayInputStream(buf);

        // Wrap the byte array output stream in an input 
        // stream reader, so you can read the data as a
        // stream of **characters**: reader/writer handles 
        // characters
        InputStreamReader isr 
        = new InputStreamReader(bais);

        // Wrap the input stream reader in a bufferred reader,
        // so you can read the character data a line at a time.
        // (A line is a sequence of chars terminated by any 
        // combination of \r and \n.)
        BufferedReader br 
        = new BufferedReader(isr);

        // The message data is contained in a single line, 
        // so read this line.
        String line = br.readLine();

        // Print host address and data received from it.
        System.out.println("Received from " + request.getAddress().getHostAddress() 
            + ": " + new String(line) );
        } // end of printData

} // end of class
    
