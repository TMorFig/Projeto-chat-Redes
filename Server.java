import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.*;

public class Server
{
  static private final ByteBuffer buffer = ByteBuffer.allocate( 16384 );
  static private final Charset charset = Charset.forName("UTF8");
  static private final CharsetDecoder decoder = charset.newDecoder();

  static private final List<SocketChannel> socketChannels = new ArrayList<>();
  static private final Map<SocketChannel, String> socketNicknames = new HashMap<>();
  static private final Map<SocketChannel, String> chatRoom = new HashMap<>();
  static private final Map<SocketChannel, String> socketStates = new HashMap<>();




  static public void main( String args[] ) throws Exception {
    int port = Integer.parseInt( args[0] );
    
    
    try {

      ServerSocketChannel ssc = ServerSocketChannel.open();

  
      ssc.configureBlocking( false );

 
      ServerSocket ss = ssc.socket();
      InetSocketAddress isa = new InetSocketAddress( port );
      ss.bind( isa );
      

      // Create a new Selector for selecting
      Selector selector = Selector.open();

      ssc.register( selector, SelectionKey.OP_ACCEPT );
      System.out.println( "Listening on port "+port );


      while (true) {

        int num = selector.select();

        if (num == 0) {
          continue;
        }
        Set<SelectionKey> keys = selector.selectedKeys();
        Iterator<SelectionKey> it = keys.iterator();

        while (it.hasNext()) {
  
          SelectionKey key = it.next();


          if (key.isAcceptable()) {

      
            Socket s = ss.accept();
            System.out.println( "Got connection from "+s );

            
           


           
            SocketChannel sc = s.getChannel();
            sc.configureBlocking( false );

            // Register it with the selector, for reading
            sc.register( selector, SelectionKey.OP_READ );

            socketChannels.add(sc);

            socketNicknames.put(sc, "unnamed");

            socketStates.put(sc , "init");

          } else if (key.isReadable()) {

            SocketChannel sc = null;

            try {

              // It's incoming data on a connection -- process it
              sc = (SocketChannel)key.channel();
              boolean ok = processInput( sc ,selector );




              if (!ok) {
                key.cancel();
 
                Socket s = null;
                try {
                  s = sc.socket();
                  System.out.println( "Closing connection to "+s );
                  s.close();
                } catch( IOException ie ) {
                  System.err.println( "Error closing socket "+s+": "+ie );
                }
              }

            } catch( IOException ie ) {
              key.cancel();

              try {
                
                sc.close();
              } catch( IOException ie2 ) { System.out.println( ie2 ); }
              //
              for ( SocketChannel channel : socketChannels ) {
                if (channel != sc && chatRoom.containsKey(channel) == true &&chatRoom.get(channel).equals(chatRoom.get(sc))){
                  channel.write(Charset.forName("UTF-8").newEncoder().encode(CharBuffer.wrap("LEFT "+socketNicknames.get(sc) + "\n")));  
                }
              }
              socketChannels.remove(sc);
              socketNicknames.remove(sc);
              chatRoom.remove(sc);

              
              System.out.println( "Closed "+sc );
            }
          }
        }
        keys.clear();
      }
    } catch( IOException ie ) {
      System.err.println( ie );
    }
  }


  // Just read the message from the socket and send it to stdout
  static private boolean processInput( SocketChannel sc, Selector selector ) throws IOException {
    // Read the message to the buffer
    buffer.clear();
    sc.read( buffer );
    buffer.flip();

    if (buffer.limit()==0) {
      return false;
    }
    
    String message = decoder.decode(buffer).toString();

    //comandos
    if(message.charAt(0) == '/'){ 
      if(message.contains("/join")){
        if(!message.startsWith("/join ") || message.length() <= 7){
          sc.write(Charset.forName("UTF-8").newEncoder().encode(CharBuffer.wrap("ERROR\n")));

        }
        else{
          String room = message.substring(6).trim();

          if(chatRoom.containsKey(sc) == false && socketNicknames.get(sc) != "unnamed") {
            sc.write(Charset.forName("UTF-8").newEncoder().encode(CharBuffer.wrap("OK\n")));
            chatRoom.put (sc, room);
            socketStates.put(sc, "inside");
            for ( SocketChannel channel : socketChannels ) {
              if (channel != sc && chatRoom.containsKey(channel) == true &&chatRoom.get(channel).equals(chatRoom.get(sc)))
                channel.write(Charset.forName("UTF-8").newEncoder().encode(CharBuffer.wrap("JOINED "+socketNicknames.get(sc) + "\n")));
            }
          }
          else if(socketNicknames.get(sc) == "unnamed"){
              sc.write(Charset.forName("UTF-8").newEncoder().encode(CharBuffer.wrap("ERROR\n")));
          }
          else if(chatRoom.containsKey(sc) == true && socketNicknames.get(sc) != "unnamed"){
            sc.write(Charset.forName("UTF-8").newEncoder().encode(CharBuffer.wrap("OK\n")));
            socketStates.put(sc, "inside");
            for ( SocketChannel channel : socketChannels ) {
              if (channel != sc && chatRoom.containsKey(channel) == true &&chatRoom.get(channel).equals(chatRoom.get(sc)))
                channel.write(Charset.forName("UTF-8").newEncoder().encode(CharBuffer.wrap("LEFT "+socketNicknames.get(sc) + "\n")));
            }
            chatRoom.remove(sc);
            
            chatRoom.put (sc, room);
            for ( SocketChannel channel : socketChannels ) {
              if (channel != sc && chatRoom.containsKey(channel) == true &&chatRoom.get(channel).equals(chatRoom.get(sc)))
                channel.write(Charset.forName("UTF-8").newEncoder().encode(CharBuffer.wrap("JOINED "+socketNicknames.get(sc) + "\n")));
            }

          }
        }
      }
      else if(message.contains("/bye")){
             
        for (SocketChannel channel : socketChannels) {
          if (channel != sc && chatRoom.containsKey(channel) == true &&chatRoom.get(channel).equals(chatRoom.get(sc))){
              channel.write(Charset.forName("UTF-8").newEncoder().encode(CharBuffer.wrap("LEFT "+socketNicknames.get(sc) + "\n")));
          }
          if(channel == sc){
            channel.write(Charset.forName("UTF-8").newEncoder().encode(CharBuffer.wrap("BYE\n")));
          }
            
        }
          socketChannels.remove(sc);
          socketNicknames.remove(sc);
          chatRoom.remove(sc);
          System.out.println( "Closed "+sc );
          sc.close();
        
        
      }
      else if(message.contains("/leave")){

        if(chatRoom.containsKey(sc) == true) {
          sc.write(Charset.forName("UTF-8").newEncoder().encode(CharBuffer.wrap("OK\n")));
          socketStates.put(sc, "outside");
          for ( SocketChannel channel : socketChannels ) {
            if (channel != sc && chatRoom.containsKey(channel) == true &&chatRoom.get(channel).equals(chatRoom.get(sc))){
              channel.write(Charset.forName("UTF-8").newEncoder().encode(CharBuffer.wrap("LEFT "+socketNicknames.get(sc) + "\n")));  
            }
          }
        }
        else{
          sc.write(Charset.forName("UTF-8").newEncoder().encode(CharBuffer.wrap("ERROR\n")));
        }
        chatRoom.remove(sc);

      }

      else if(message.contains("/nick") ){
        if(!message.startsWith("/nick ") || message.length() <= 6){
          sc.write(Charset.forName("UTF-8").newEncoder().encode(CharBuffer.wrap("ERROR\n")));

        }
        else {
          if(socketNicknames.containsValue(message.substring(6, message.length()-2)) == true){
            sc.write(Charset.forName("UTF-8").newEncoder().encode(CharBuffer.wrap("ERROR\n")));
          }
          else if(message.substring(6, message.length()-2).contains(" ")){
            sc.write(Charset.forName("UTF-8").newEncoder().encode(CharBuffer.wrap("ERROR\n")));
            
          }
          else{
            
            if(socketNicknames.containsKey(sc)){
              //
              for ( SocketChannel channel : socketChannels ) {
                if (channel != sc && socketNicknames.get(sc) != "unnamed" && chatRoom.containsKey(channel) == true && (chatRoom.get(channel).equals(chatRoom.get(sc))))
                  channel.write(Charset.forName("UTF-8").newEncoder().encode(CharBuffer.wrap("NEWNICK "+socketNicknames.get(sc) +" "+message.substring(6, message.length()-2)+"\n")));
              }
              socketStates.put(sc, "outside");
              socketNicknames.put(sc, message.substring(6, message.length()-2));
              sc.write(Charset.forName("UTF-8").newEncoder().encode(CharBuffer.wrap("OK\n")));
            }
          }
        }
        
        
      }


      else if(message.contains("/priv") ){
        if(!message.startsWith("/priv ") || message.length() <= 7){
          sc.write(Charset.forName("UTF-8").newEncoder().encode(CharBuffer.wrap("ERROR\n")));

        }
        else{ 
          String names =message.substring(6);
          if(socketNicknames.containsValue(names.split(" ")[0]) == false  || socketNicknames.get(sc) == "unnamed"){
            sc.write(Charset.forName("UTF-8").newEncoder().encode(CharBuffer.wrap("ERROR\n")));
          }

          else{
            SocketChannel sc2= socketChannels.get(0);
            for (SocketChannel channel : socketChannels) {
              if (socketNicknames.get(channel).equals(names.split(" ")[0])) {
                sc2 = channel;
              }
            }
            sc2.write(Charset.forName("UTF-8").newEncoder().encode(CharBuffer.wrap("PRIVATE "+socketNicknames.get(sc)+" "+names.substring(names.split(" ")[0].length(), names.length()) ) ));
          }
        }
        
      }
      else if(socketNicknames.get(sc) != "unnamed" && chatRoom.containsKey(sc)==true){
        for ( SocketChannel channel : socketChannels ) {
          if(chatRoom.get(sc).equals( chatRoom.get(channel)) == false){

          }
          else{

            if (channel != sc)
              channel.write(Charset.forName("UTF-8").newEncoder().encode(CharBuffer.wrap("MESSAGE "+socketNicknames.get(sc) +" "+ message)));
            else
              channel.write(Charset.forName("UTF-8").newEncoder().encode(CharBuffer.wrap(message)));
          }
        }
      }
      else{
        sc.write(Charset.forName("UTF-8").newEncoder().encode(CharBuffer.wrap("ERROR\n")));
      }

    }
	
	
	
    else if(socketNicknames.get(sc) != "unnamed" && chatRoom.containsKey(sc)==true){
      for ( SocketChannel channel : socketChannels ) {
        if(chatRoom.get(sc).equals( chatRoom.get(channel)) == false){

        }
        else{

          if (channel != sc)
            channel.write(Charset.forName("UTF-8").newEncoder().encode(CharBuffer.wrap("MESSAGE "+socketNicknames.get(sc) +" "+ message)));
          else
            channel.write(Charset.forName("UTF-8").newEncoder().encode(CharBuffer.wrap(message)));
        }
      }
    }
    else{
      sc.write(Charset.forName("UTF-8").newEncoder().encode(CharBuffer.wrap("ERROR\n")));
    }
    return true;
  }
  
}