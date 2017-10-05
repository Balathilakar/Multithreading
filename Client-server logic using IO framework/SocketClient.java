import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;

public class SocketClient {
	public static void main(String arg[]){
		String host="192.168.1.7";
		//"localhost";
		int port=1999;
		
		StringBuffer instr=new StringBuffer();
		String timeStamp;
		char ch = (char)10;
		System.out.print("Hi");
		System.out.print(ch);
		System.out.println("clientSocket Initialized");
		try{
			InetAddress address=InetAddress.getByName(host);
			Socket connection=new Socket(address,port);
			BufferedOutputStream bos=new BufferedOutputStream(connection.getOutputStream());
			OutputStreamWriter osw=new OutputStreamWriter(bos, "US-ASCII");
			timeStamp=new java.util.Date().toString();
			String process="Calling the socket server on "+host+"Port "+port+ "at "+timeStamp+ (char)13;
			osw.write(process);
			osw.flush();
//			Reading Data from the server.
			BufferedInputStream bis=new BufferedInputStream(connection.getInputStream());
			InputStreamReader isr=new InputStreamReader(bis,"US-ASCII");
			int c;
			while((c=isr.read())!=13)
				instr.append((char)c);
			connection.close();
			System.out.println(instr);
		}
		catch (IOException io)
		{
		System.out.println("IOException: "+io);
		}
		catch(Exception e) 
		{
		System.out.println("Exception: "+e);	
		}
	}
}
