import com.sun.org.apache.xalan.internal.xsltc.util.IntegerArray;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;
import javax.swing.*;
import javax.swing.JProgressBar;

public class Receiver extends JFrame implements ActionListener {

    public JFrame frame1;
    public JFrame frame2;
    public JFrame download_frame = new JFrame("File Download");

    public JLabel num_packets_received;

    public int num_packets_dropped;

    public static DatagramSocket socket;

    public final JProgressBar download_bar = new JProgressBar(0, 100);

    public static int packets_received_count;

    private SwingWorker <Integer, Integer> A_SwingWorker = null;

    //Original GUI
    public JTextField host_field;
    public JTextField port_sender;
    public JTextField port_receiver;
    public JTextField file_name;

    private JButton transfer_button;
    private JButton reliable_button;
    private JButton unreliable_button;

    private JButton cancel_button;

    //Message Box Buttons
    private JButton yes_button;
    private JButton no_button;


    InetAddress sender_address;
    int sender_port;
    int the_receiver_port;

    public int total_packets;

    public static boolean reliable;

    public static String the_file_name = "";


    public static String End_Transmission_String = "STOP_TRANSMISSION";


    public static void main(String[] args) throws IOException {

        try{
            new Receiver();
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    public Receiver()throws Exception{

        //Download_Box(24, "a_file.txt");

        //Initialize
        frame1 = new JFrame("UDP Transfer");
        frame1.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame1.setLocationRelativeTo(frame1);

        //Size
        frame1.setSize(300,450);
        frame1.setResizable(false);

        //Buttons
        transfer_button = new JButton("TRANSFER");
        reliable_button = new JButton("Reliable");
        unreliable_button = new JButton("UnReliable");

        //Text Fields
        host_field = new JTextField(22);
        JLabel host_address = new JLabel("Host address of the Sender");

        port_sender = new JTextField(22);
        JLabel sender_port = new JLabel("Port # of sender");

        port_receiver = new JTextField(22);
        JLabel receiver_port = new JLabel("Port # to receive data");

        file_name = new JTextField(22);
        JLabel name_file = new JLabel("File Name");

        JPanel panel = new JPanel();


        //Disable Transfer Button
        transfer_button.setEnabled(false);

        panel.add(host_address);
        panel.add(host_field);

        panel.add(sender_port);
        panel.add(port_sender);

        panel.add(receiver_port);
        panel.add(port_receiver);

        panel.add(name_file);
        panel.add(file_name);

        panel.add(reliable_button);
        panel.add(unreliable_button);
        panel.add(transfer_button);
        frame1.add(panel);

        //Adding Action Listener
        reliable_button.addActionListener(this);
        unreliable_button.addActionListener(this);
        transfer_button.addActionListener(this);

        frame1.setVisible(true);
    }

    public void actionPerformed(ActionEvent a){
        String action = a.getActionCommand();

        A_SwingWorker = new SwingWorker<Integer, Integer>() {
            @Override
            protected Integer doInBackground() throws Exception {

                packets_received_count = 0;
                num_packets_dropped = 0;


                if (reliable == true){

                    byte[] EOT = End_Transmission_String.getBytes();

                    DatagramPacket end_packet = new DatagramPacket(EOT, EOT.length);

                    boolean keep_receiving = true;

                    int sequence_number;

                    FileOutputStream out = new FileOutputStream(the_file_name);

                    int count = 0;
                    int prev_d_progress = 0;
                    int d_progress;

                    while (keep_receiving == true){
                        byte[] buff = new byte[124];
                        DatagramPacket packet;

                        // receive request
                        packet = new DatagramPacket(buff, buff.length);
                        socket.receive(packet);
                        count = count+1;

                        sender_address = packet.getAddress();
                        int sender_port = packet.getPort();

                        buff = packet.getData();

                        //Get Sequence #
                        sequence_number = buff[buff.length-1];
                        byte[] buff_data = Arrays.copyOfRange(buff, 0, buff.length-1);

                        String buff_string = new String(buff_data);

                        //Check if packet is EOT
                        if ((end_packet.getLength())==(packet.getLength())){
                            System.out.println("");
                            System.out.println("");
                            System.out.println("End of Transmission");
                            DatagramPacket ack = new DatagramPacket(EOT, 17, sender_address, sender_port);
                            socket.send(ack);
                            keep_receiving = false;
                            socket.close();
                            System.out.println("packets received: "+packets_received_count);
                            publish(packets_received_count);
                        }
                        else{
                            //System.out.print(buff_string);
                            byte[] ack_buffer = new byte[1];
                            ack_buffer[0] = (byte)(sequence_number);
                            DatagramPacket ack = new DatagramPacket(ack_buffer, 1, sender_address, sender_port);
                            socket.send(ack);
                            packets_received_count = packets_received_count+1;
                            publish(count);

                            //Write the data
                            out.write(buff_data);
                        }
                    }

                    out.close();

                }

                else{
                    byte[] EOT = End_Transmission_String.getBytes();

                    DatagramPacket end_packet = new DatagramPacket(EOT, EOT.length);

                    boolean keep_receiving = true;

                    int sequence_number;

                    int count = 0;

                    FileOutputStream out = new FileOutputStream(the_file_name);

                    while (keep_receiving == true){
                        byte[] buff = new byte[124];
                        DatagramPacket packet;

                        // receive request
                        packet = new DatagramPacket(buff, buff.length);
                        try{
                            socket.receive(packet);
                        }catch(SocketTimeoutException e){
                            System.out.println("Check timeout in sender: Receiver TimedOut");
                        }

                        count = count+1;

                        sender_address = packet.getAddress();
                        int sender_port = packet.getPort();

                        buff = packet.getData();

                        if (count%10 != 0){
                            //Get Sequence #
                            sequence_number = buff[buff.length-1];
                            byte[] buff_data = Arrays.copyOfRange(buff, 0, buff.length-2);

                            String buff_string = new String(buff);

                            //Check if packet is EOT
                            if ((end_packet.getLength())==(packet.getLength())){
                                System.out.println("");
                                System.out.println("");
                                System.out.println("End of Transmission");
                                DatagramPacket ack = new DatagramPacket(EOT, 17, sender_address, sender_port);
                                socket.send(ack);
                                keep_receiving = false;
                                socket.close();

                            }
                            else{
                                byte[] ack_buffer = new byte[1];
                                ack_buffer[0] = (byte)(sequence_number);
                                DatagramPacket ack = new DatagramPacket(ack_buffer, 1, sender_address, sender_port);
                                socket.send(ack);
                                packets_received_count = packets_received_count+1;
                                publish(packets_received_count);

                                //Write the data
                                out.write(buff);
                            }
                        }

                        else{
                            num_packets_dropped = num_packets_dropped+1;
                        }
                    }
                    out.close();
                }
                return 0;
            }

            @Override
            protected void done() {
                int d_progress = (int)((packets_received_count/ (double)(total_packets-1)) *100);
                download_bar.setValue(d_progress);
                num_packets_received.setText("In-Order Packets Received:  "+packets_received_count+"  Out of:  " + total_packets);
                System.out.println("Packets Dropped: " + num_packets_dropped);
            }

            @Override
            protected void process(List<Integer> integer_list){
                int i = integer_list.get(0);
                int d_progress = (int)((i/ (double)(total_packets-1)) *100);
                download_bar.setValue(d_progress);
                num_packets_received.setText("In-Order Packets Received:  "+i+"  Out of:  " + total_packets);
            }
        };

        if(action.equals("UnReliable")){
            transfer_button.setEnabled(true);
            reliable = false;
        }
        else if(action.equals("Reliable")){
            transfer_button.setEnabled(true);
            reliable = true;
        }
        else if(action.equals("TRANSFER")){
            try{
                frame1.setVisible(false);
                Send_Request(file_name.getText());
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
        else if(action.equals("YES")){
            try{
                frame2.setVisible(false);
                byte[] ack_buffer = new byte[1];
                ack_buffer[0] = (byte)(1);
                DatagramPacket the_packet = new DatagramPacket(ack_buffer, 1, sender_address, the_receiver_port);
                socket.send(the_packet);
                try{
                    Download_Box(total_packets, file_name.getText(), 0);
                    A_SwingWorker.execute();
                    A_SwingWorker = null;
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
        else if(action.equals("NO")){
            try{
                System.out.println("Not Downloading");
                frame2.setVisible(false);
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
        else if(action.equals("Cancel")){
            System.out.println("Exit");
        }

    }


    public void Send_Request(String file_name) throws Exception{
        sender_port = Integer.parseInt(port_sender.getText());
        socket = new DatagramSocket(sender_port);

        the_file_name = file_name;
        byte[] request = (file_name.getBytes());

        if (request.length > 99){
            System.out.println("File Name too Long");
            return;
        }

        request = Arrays.copyOf(request, 100);

        the_receiver_port = Integer.parseInt(port_receiver.getText());
        sender_address = InetAddress.getByName(host_field.getText());

        DatagramPacket packet = new DatagramPacket(request, request.length, sender_address, the_receiver_port);
        socket.send(packet);

        int timer = 5000;

        socket.setSoTimeout(timer);

        //Receive file size ( in number of packets)
        try{
            socket.receive(packet);
        }
        catch(SocketTimeoutException a){
            System.out.println("Sender Not Responding - Wrong data in fields");
            System.exit(0);
        }

        byte[] file_size_buffer;

        file_size_buffer = packet.getData();

        int num_required_packets = fromByteArray(file_size_buffer);
        total_packets = num_required_packets;


        double file_size = (double)((num_required_packets*123))/1000;
        file_size = Math.round(file_size*100.0)/100.0;

        try{
            Message_Box(file_size, file_name);
        }catch(Exception e){
            e.printStackTrace();
        }

    }

    public void Message_Box(double file_size, String file_name) throws Exception{
        frame2 = new JFrame("Accept Transfer?");
        frame2.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame2.setLocationRelativeTo(frame2);

        //Size
        frame2.setSize(170,190);
        frame2.setResizable(false);

        //Label
        JLabel confirm = new JLabel("Confirm download of file");

        JLabel the_file_name = new JLabel("file: " + file_name);
        JLabel the_file_size = new JLabel("file size (KB): " + file_size);

        //Buttons
        yes_button = new JButton("YES");
        no_button = new JButton("NO");

        JPanel message_panel = new JPanel();

        message_panel.add(confirm);
        message_panel.add(the_file_name);
        message_panel.add(the_file_size);
        message_panel.add(yes_button);
        message_panel.add(no_button);
        frame2.add(message_panel);


        //Adding Action Listener
        no_button.addActionListener(this);
        yes_button.addActionListener(this);

        frame2.setVisible(true);
    }


    public void Download_Box(int num_total_packets, String file_name, int progress) throws Exception{
        download_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        download_frame.setLocationRelativeTo(download_frame);

        download_bar.setSize(50, 100);
        download_bar.setValue(progress);
        download_bar.setStringPainted(true);

        //Size
        download_frame.setSize(400,230);
        download_frame.setResizable(false);

        //Label
        JLabel download_progress = new JLabel("Download Progress: ");

        JLabel the_file_name = new JLabel("file: " + file_name);

        num_packets_received = new JLabel("In-Order Packets Received:      " );

        //Buttons
        cancel_button = new JButton("Cancel");

        JPanel downloading_panel = new JPanel();

        downloading_panel.add(the_file_name);
        downloading_panel.add(download_bar);
        downloading_panel.add(num_packets_received);

        download_frame.add(downloading_panel);

        //Adding Action Listener
        cancel_button.addActionListener(this);

        download_frame.setVisible(true);
    }

    int fromByteArray(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getInt();
    }

}