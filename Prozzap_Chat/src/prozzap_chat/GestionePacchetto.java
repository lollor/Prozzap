/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package prozzap_chat;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Lorenzo
 */
public class GestionePacchetto extends Thread {

    String nomeMittente, mioNome;
    DatagramSocket socketRicezione, socketInvio;

    public GestionePacchetto(String mioNome) throws SocketException {
        this.mioNome = mioNome;
        this.socketRicezione = new DatagramSocket(12345);
        this.socketInvio = new DatagramSocket();
    }

    @Override
    public void run() {
        while (true) {
            byte[] buf = new byte[1500];
            DatagramPacket p = new DatagramPacket(buf, buf.length);
            try {
                socketRicezione.receive(p);
            } catch (IOException ex) {
                Logger.getLogger(GestionePacchetto.class.getName()).log(Level.SEVERE, null, ex);
            }
            try {
                Invia(controlloPaccketto(new String(p.getData()).trim()), p.getAddress(), p.getPort());
            } catch (IOException ex) {
                Logger.getLogger(GestionePacchetto.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private String controlloPaccketto(String info) {
        char tipoRichiesta = info.charAt(0);
        switch (tipoRichiesta) {
            case 'a':
                
                break;
            case 'y':

                break;
            case 'n':

                break;
            case 'm':

                break;
            case 'c':

                break;
            default:
                throw new AssertionError();
        }
        return "";
    }
    
    public void Invia(String info, InetAddress address, int port) throws IOException{
        byte[] bufRisposta = info.getBytes();
        socketInvio.send(new DatagramPacket(bufRisposta, bufRisposta.length, address, port));
    }
}
