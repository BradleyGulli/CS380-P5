import java.util.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.io.*;

public class UdpClient {
	static byte version;
	static byte hlen;
	static byte tos;
	static int dataSize;
	static int headerSize;
	static byte TTL;
	static byte flag;
	static byte protocol;
	static byte[] dst;
	static byte[] src;
	
	
	    /*
	     * just handles  some of the functions calls 
	     * also creates the data to be sent
	     */
		public static void main(String[] args) throws Exception{
			initInfo();
			int port = 0;
			try(Socket socket = new Socket("codebank.xyz", 38005)){
				byte[] handShakeData = new byte[4];
				handShakeData[0] = (byte)0xDE;
				handShakeData[1] = (byte)0xAD;
				handShakeData[2] = (byte)0xBE;
				handShakeData[3] = (byte)0xEF;
				byte[] handShakePacket = createPacket(handShakeData);
				System.out.print("Handshake ");
				sendPacket(socket, handShakePacket);
				dataSize = 2;
				
				InputStream in = socket.getInputStream();
				
				byte[] portNumber = new byte[2];
				portNumber[0] = (byte)in.read();
				portNumber[1] = (byte)in.read();
				
				port = (portNumber[0] & 0xFF) << 8 | (portNumber[1] & 0xFF);
				System.out.println("Portnumber: " + port + '\n');
					
				float totalTime = 0;
				for(int i = 0; i < 12; i++){
					if(socket.isClosed()){
						System.out.println("bad packet. Connection closed");
						break;
					}
					byte[] data = new byte[dataSize];
					new Random().nextBytes(data);
					byte[] udp = udpHeader(port, data);
					
					byte[] packet = udpPacket(udp);
					long start = System.currentTimeMillis();
					sendPacket(socket, packet);
					long end = System.currentTimeMillis();
					long elapsedTime = end - start;
					System.out.println("RTT: " + (elapsedTime) + "ms" + '\n');
					totalTime += elapsedTime;
					dataSize *= 2;

				}
				System.out.printf("Average RTT: %,.2f%n", totalTime/12);
			}
		}
		
		/*
		 * calculates the check sum
		 */
		public static long checkSum(byte[] b){
			long sum = 0;
			long highVal;
			long lowVal;
			long value;
			
			for(int i = 0; i < 19; i+=2){
				highVal = ((b[i] << 8) & 0xFF00); 
				lowVal = ((b[i + 1]) & 0x00FF);
				value = highVal | lowVal;
	
				sum += value;
			    
			    //check for the overflow
			    if ((sum & 0xFFFF0000) > 0) {
			        sum = sum & 0xFFFF;
			        sum += 1;
			      }
			
			}
			
			sum = ~sum;
			sum = sum & 0xFFFF;
			return sum;
		}
		
		/*
		 * calculates the UDP checksum for the 
		 * UDP header and pseudo header
		 */
		public static long udpCheckSum(byte[] b, int l){
			long sum = 0;
			long highVal;
			long lowVal;
			long value;
			byte[] udp = new byte[b.length + 12];
	
			udp[0] = src[0];
			udp[1] = src[1];
			udp[2] = src[2];
			udp[3] = src[3];
			udp[4] = dst[0];
			udp[5] = dst[1];
			udp[6] = dst[2];
			udp[7] = dst[3];
			udp[8] = 0;
			udp[9] = 17;
			udp[10] = (byte)((l & 0xFF00) >>> 8);
	        udp[11] = (byte)(( l & 0x00FF));
					
			
			for(int i=12, j=0; i< udp.length; ++i, ++j) 
				udp[i] = b[j];
			
			
			for(int i = 0; i < udp.length; i+=2){
				highVal = ((udp[i] << 8) & 0xFF00); 
				lowVal = ((udp[i + 1]) & 0x00FF);
				value = highVal | lowVal;
	
				sum += value;
			    
			    //check for the overflow
			    if ((sum & 0xFFFF0000) > 0) {
			        sum = sum & 0xFFFF;
			        sum += 1;
			      }
			
			}
			
			sum = ~sum;
			sum = sum & 0xFFFF;
			return sum;
		}
		
		/*
		 * Creates the Ipv4 packets with UDP data to be sent
		 */
		private static byte[] udpPacket(byte[] data){
			byte[] packet = new byte[20 + data.length];
			byte finalV = (byte)(version << 4 | hlen);
			packet[0] = finalV;
			
			packet[1] = tos;
			
			int tempLen = headerSize + data.length;
			System.out.println("Sending packet with data size: " + dataSize);
			packet[2] = (byte) ((tempLen >>> 8) & 0xFF);
			packet[3] = (byte) (tempLen & 0xFF);
			
			packet[4] = 0;
			packet[5] = 0;
			packet[6] = flag;
			packet[7] = 0;
			packet[8] = TTL;
			packet[9] = protocol;
			
			for(int i = 12, j = 0; i < 16; i++, j++){
				packet[i] = src[j];
			}
			
			for(int i = 16, j = 0; i < 20; i++, j++) {
				packet[i] = dst[j];
			}
			
			for(int i = 20, j = 0; j < data.length; i++, j++){
				packet[i] = data[j];
			}
			
			short check = (short)checkSum(packet);
			byte[] asArray = new byte[2];
	        asArray[0] = (byte)((check & 0xFF00) >>> 8);
	        asArray[1] = (byte)((check & 0x00FF));
	        
	        packet[10] = asArray[0];
	        packet[11] = asArray[1];
			return packet;
		}
		
		/*
		 * creates the UDP Header
		 */
		private static byte[] udpHeader(int port, byte[] data){
			byte[] packet = new byte[8 + data.length];
			packet[0] = 0;
			packet[1] = 0;
			packet[2] = (byte) ((port & 0xFF00) >>> 8);
			packet[3] = (byte) (port & 0x00FF);
			packet[4] = (byte) ((packet.length & 0xFF00) >>> 8);
			packet[5] = (byte) (packet.length & 0x00FF);
			packet[6] = 0;
			packet[7] = 0;
			for(int i = 8; i < packet.length; i++){
				packet[i] = data[i - 8];
			}
			
			short check = (short)udpCheckSum(packet, packet.length);
			ByteBuffer buf = ByteBuffer.allocate(2);
			buf.putShort(check);
			byte[] asArray = buf.array();
			//asArray[0] = (byte)((check & 0xFF00) >>> 8);
	        //asArray[1] = (byte)((check & 0x00FF));
	        
	        packet[6] = asArray[0];
	        packet[7] = asArray[1];
			
			
			return packet;
			
		}
		
		/*
		 * puts all of the information together in an Ipv4 format
		 */
		private static byte[] createPacket(byte[] data) throws Exception{
			byte[] packet = new byte[20 + dataSize];
			byte finalV = (byte)(version << 4 | hlen);
			packet[0] = finalV;
			
			packet[1] = tos;
			
			int tempLen = headerSize + dataSize;
			System.out.println("Sending packet with data size: " + dataSize);
			packet[2] = (byte) ((tempLen >> 8) & 0xFF);
			packet[3] = (byte) (tempLen & 0xFF);
			
			packet[4] = 0;
			packet[5] = 0;
			packet[6] = flag;
			packet[7] = 0;
			packet[8] = TTL;
			packet[9] = protocol;
			
			for(int i = 12, j = 0; i < 16; i++, j++){
				packet[i] = src[j];
			}
			
			for(int i = 16, j = 0; i < 20; i++, j++) {
				packet[i] = dst[j];
			}
			
			for(int i = 20, j = 0; j < data.length; i++, j++){
				packet[i] = data[j];
			}
			
			short check = (short)checkSum(packet);
			byte[] asArray = new byte[2];
	        asArray[0] = (byte)((check & 0xFF00) >>> 8);
	        asArray[1] = (byte)((check & 0x00FF));
	        
	        packet[10] = asArray[0];
	        packet[11] = asArray[1];
			return packet;
			
			
		}
		
		/*
		 * sends the packets
		 */
		private static void sendPacket(Socket socket, byte[] packet) throws Exception{
			
				OutputStream out = socket.getOutputStream();
				InputStream in = socket.getInputStream();
				BufferedReader br = new BufferedReader(new InputStreamReader(in));
				
				out.write(packet);
				byte[] rec = new byte[4];
				in.read(rec);
				String magic = "";
				for(byte e: rec){
					magic += Integer.toHexString(Byte.toUnsignedInt(e));
				}
				System.out.println("Response: " + magic.toUpperCase());
			
		}
		
		/*
		 * just some info that will be constant 
		 */
		private static void initInfo() throws Exception{
			version = 4;
			hlen = 5;
			tos = 0;
			dataSize = 4;
			headerSize = 20;
			flag = 2 << 5;
			TTL = 50;
			protocol = 17;
			
			try(Socket socket = new Socket("codebank.xyz", 38005)){
				InetAddress address = socket.getInetAddress();
				dst = address.getAddress();
			}
			
			src = new byte[4];
			src[0] = 127;
			src[1] = 0;
			src[2] = 0;
			src[3] = 1;
			
			
		}		
		
}


