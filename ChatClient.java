import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


public class ChatClient {

    // Variáveis relacionadas com a interface gráfica --- * NÃO MODIFICAR *
    JFrame frame = new JFrame("Chat Client");
    private JTextField chatBox = new JTextField();
    private JTextArea chatArea = new JTextArea();
    // --- Fim das variáveis relacionadas com a interface gráfica

    // Se for necessário adicionar variáveis ao objecto ChatClient, devem
    // ser colocadas aqui
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;



    
    // Método a usar para acrescentar uma string à caixa de texto
    // * NÃO MODIFICAR *
    public void printMessage(final String message) {
        chatArea.append(message);
    }

    
    // Construtor
    public ChatClient(String server, int port) throws IOException {

        // Inicialização da interface gráfica --- * NÃO MODIFICAR *
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(chatBox);
        frame.setLayout(new BorderLayout());
        frame.add(panel, BorderLayout.SOUTH);
        frame.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        frame.setSize(500, 300);
        frame.setVisible(true);
        chatArea.setEditable(false);
        chatBox.setEditable(true);
        chatBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    newMessage(chatBox.getText());
                } catch (IOException ex) {
                } finally {
                    chatBox.setText("");
                }
            }
        });
        frame.addWindowListener(new WindowAdapter() {
            public void windowOpened(WindowEvent e) {
                chatBox.requestFocusInWindow();
            }
        });
        // --- Fim da inicialização da interface gráfica

        // Se for necessário adicionar código de inicialização ao
        // construtor, deve ser colocado aqui
        socket = new Socket(server, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new PrintWriter(socket.getOutputStream(), true);


    }


    // Método invocado sempre que o utilizador insere uma mensagem
    // na caixa de entrada
    public void newMessage(String message) throws IOException {

        if ("/join".equals(message.trim())) {
            writer.print("/join");
            writer.flush();

        } else {
            // Send the regular message to the server
            writer.println(message);
        }

        


    }

    
    // Método principal do objecto
    public void run() throws IOException {
        try {
            while (true) {
                // Aguarda por mensagens do servidor e exibe na interface gráfica
                String message = reader.readLine();

                if(message.equals("OK") 
                || message.equals("ERROR") 
                || message.contains("JOINED") 
                || message.contains("LEFT")) {
                    printMessage(message+"\n");
                }
                    
                else if(message.contains("MESSAGE")) {
                    String[] parts = message.split(" ");
                    String nick = parts[1];
                    String text = "";
                    for (int i=2; i < parts.length; i++) {
                        text += parts[i] + " ";
                    }

                    printMessage(nick + ": " + text + "\n");
                } 

                else if(message.contains("PRIVATE")) {
                    printMessage(message+"\n");
                }

                else if(message.contains("NEWNICK")) {
                    String[] parts = message.split(" ");
                    String old_nick = parts[1];
                    String new_nick = parts[2];

                    printMessage(old_nick + " mudou de nome para " + new_nick + "\n");
                } 

                else if(message.contains("BYE")) {
                    printMessage(message+"\n");
                    break;
                }

                else
                    printMessage(message+"\n");

            }

        } finally {
            // Fecha os recursos quando a execução termina
            socket.close();
            reader.close();
            writer.close();
        }



    }
    

    // Instancia o ChatClient e arranca-o invocando o seu método run()
    // * NÃO MODIFICAR *
    public static void main(String[] args) throws IOException {
        ChatClient client = new ChatClient(args[0], Integer.parseInt(args[1]));
        client.run();
    }

}