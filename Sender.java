import javax.swing.*;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.math.*;

public class Sender {


    public static int num_packets_dropped;
    public static String receiver_ip;
    public static int receiver_port;
    public static int sender_port;
    public static String file_name;
    public static int time_out;
    public static String End_Transmission_String = "STOP_TRANSMISSION";

    public static long the_time;


    public static void main(String[] args) throws IOException {


        num_packets_dropped = 0;

        if (args.length != 5) {
            System.out.println("invalid number of arguments");
            //Exit;
            return;
        }

        String receiver_port2 = args[1];
        String sender_port2 = args[2];
        String time_out2 = args[4];
        receiver_port = Integer.parseInt(receiver_port2);
        sender_port = Integer.parseInt(sender_port2);
        time_out = Integer.parseInt(time_out2);

        receiver_ip = args[0];
        file_name = args[3];

        DatagramSocket socket = new DatagramSocket(receiver_port);
        boolean keep_receiving = true;

        byte[] buff = new byte[100];

        DatagramPacket packet;

        System.out.println("Receiver port: "  +  receiver_port);
        System.out.println("Sender port: "  +  sender_port);

        // receive request
        packet = new DatagramPacket(buff, buff.length);
        socket.receive(packet);

        buff = packet.getData();

        buff = Arrays.copyOf(buff, file_name.length());
        String check_filename = new String(buff);

        InetAddress receiver_address = packet.getAddress();
        int check_receiver_port = packet.getPort();

        InetAddress address = InetAddress.getByName(receiver_ip);

        if (address.equals(receiver_address)!= true){
            System.out.println("Not Correct Address");
            return;
        }

        if (check_filename.equals(file_name) != true){
            System.out.println("Not correct file name");
            return;
        }

        byte[] file_data = Get_Data(file_name);

        double file_size2 = file_data.length;
        int num_packets = (int)Math.ceil(file_data.length/123);
        System.out.println("num packets: "+num_packets);

        if (num_packets > 100000000){
            System.out.println("File is Too Large!");
            return;
        }


        byte[] file_size_array;

        file_size_array = toByteArray(num_packets);

        //Send # of packets needed to send file
        packet = new DatagramPacket(file_size_array, file_size_array.length, address, sender_port);
        socket.send(packet);

        socket.receive(packet);

        buff = packet.getData();

        if (buff[0] == (byte)1){
            System.out.println("Sending");
            try{
                data_transfer(socket);
            }catch(Exception e){
                e.printStackTrace();
            }
        }
        else{
            System.out.println("Not Sending");
        }
    }

    public static byte[] Get_Data(String file_name){

        String file_path = System.getProperty("user.dir")  +  "/" + file_name;

        try
        {
            Path path = Paths.get(file_path);
            byte[] data = Files.readAllBytes(path);
            return data;
        }
        catch (Exception e)
        {
            System.err.format("Error while trying to read file");
            e.printStackTrace();
            return null;
        }
    }

    public static void Send_Packet(DatagramPacket packet, DatagramSocket socket, int timer) throws IOException {

            byte[] original_data = packet.getData();
            int original_ack = (int)(original_data[original_data.length-1]);

            //Identify EOT byte array
            byte[] EOT = End_Transmission_String.getBytes();

            try {
                socket.send(packet);
            } catch (IOException ex) {
                System.out.println("Could not deliver packet!");
            }

            boolean resend = true;
            socket.setSoTimeout(timer);


            byte[] ack_buffer = new byte[1];

            while(resend == true) {
                ack_buffer[0] = 0;
                DatagramPacket ack_packet = new DatagramPacket(ack_buffer, 1);
                try {
                    socket.receive(ack_packet);
                    ack_buffer = ack_packet.getData();
                    resend = false;
                    if (ack_buffer[0] == original_ack){
                        resend = false;
                    }
                } catch (SocketTimeoutException e) {
                    // resend
                    socket.send(packet);
                    num_packets_dropped = num_packets_dropped+1;
                    continue;
                }


            }
    }


    public static void Send_EOT_Packet(DatagramSocket socket, InetAddress address, int timer) throws IOException {

        byte[] EOT = End_Transmission_String.getBytes();
        DatagramPacket packet = new DatagramPacket(EOT, EOT.length, address, sender_port);

        try {
            socket.send(packet);
        } catch (IOException ex) {
            System.out.println("Could not deliver packet!");
        }

        boolean resend = true;
        socket.setSoTimeout(timer);

        byte[] ack_buffer = new byte[17];

        while(resend == true) {

            DatagramPacket ack_packet = new DatagramPacket(ack_buffer, ack_buffer.length);
            try {
                socket.receive(ack_packet);
                ack_buffer = ack_packet.getData();
                resend = false;

            } catch (SocketTimeoutException e) {
                // resend
                socket.send(packet);
                System.out.println("Packet Timed out, Resending");
                continue;
            }

            ack_buffer = ack_packet.getData();
            String ack_string = new String (ack_buffer);

            if (ack_string.equals(End_Transmission_String)){
                System.out.println("End of Transmission!");
                resend = false;
                socket.close();
                System.out.println("Packets Dropped: "+num_packets_dropped);

                the_time = java.lang.System.currentTimeMillis() - the_time;
                System.out.println("Total Time (Seconds)= " + (long)the_time/1000);
                System.out.println("Total Time (MilliSeconds)= " + the_time);

                return;
            }
        }

    }

    public static void data_transfer(DatagramSocket socket) throws Exception{

        //Get byte array of file
        byte[] file_data = Get_Data(file_name);

        InetAddress address = InetAddress.getByName(receiver_ip);

        int seq_num = 0;

        the_time = java.lang.System.currentTimeMillis();


        try {
            //send packets
            int y;
            for (int x = 0; x < (file_data.length); x = x + 123) {
                if (seq_num == 2){
                    seq_num = 0;
                }

                byte[] temp_data = new byte[124];
                y = x+123;

                temp_data = Arrays.copyOfRange(file_data, x, y);
                temp_data = Arrays.copyOf(temp_data, temp_data.length+1);
                temp_data[temp_data.length-1] = (byte)seq_num;

                DatagramPacket packet = new DatagramPacket(temp_data, temp_data.length, address, sender_port);

                Send_Packet(packet, socket, time_out);

                seq_num = seq_num + 1;
            }

            //End of Transmission
            Send_EOT_Packet(socket, address, time_out);
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    public static byte[] toByteArray(int value) {
        return new byte[] {
                (byte)(value >> 24),
                (byte)(value >> 16),
                (byte)(value >> 8),
                (byte)value };
    }
}

