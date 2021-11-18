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
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Lorenzo
 */
public class GestionePacchetto extends Thread {

    String nomeMittente, mioNome;
    DatagramSocket socketRicezione, socketInvio;
    boolean connesso;
    int fase;

    public GestionePacchetto(String mioNome) throws SocketException {
        this.mioNome = mioNome;
        this.socketRicezione = new DatagramSocket(12345);
        this.socketInvio = new DatagramSocket();
        fase = 1;
        connesso = false;
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
            new Thread(() -> {
                try {
                    controlloPaccketto(new String(p.getData()).trim(), p.getAddress(), true);
                } catch (IOException ex) {
                    Logger.getLogger(GestionePacchetto.class.getName()).log(Level.SEVERE, null, ex);
                }
            });
        }
    }

    private String controlloPaccketto(String info, InetAddress address, boolean sendPacket) throws IOException {
        char tipoRichiesta = info.charAt(0);
        String resto = info.substring(info.indexOf(";") + 1);
        String risposta = "";
        switch (tipoRichiesta) {
            case 'a':
                if (fase == 1 && !connesso) {
                    nomeMittente = resto.split(";")[0];
                    Invia("y;" + mioNome + ";", address);
                    return "OK,apertura permessa";
                } else {
                    Invia("n;", address);
                }
                return "NOTOK,apertura non permessa";
            case 'y':
                if (fase == 2 && !connesso) {
                    
                }
            case 'n':

                break;
            case 'm':

                break;
            case 'c':

                break;
            default:
                throw new AssertionError();
        }
        if (sendPacket) {
            Invia(risposta, address);
            return "";
        } else {
            return risposta;
        }
    }

    public void Invia(String info, InetAddress address) throws IOException {
        byte[] bufRisposta = info.getBytes();
        socketInvio.send(new DatagramPacket(bufRisposta, bufRisposta.length, address, 12345));
    }

    private boolean flag = false;

    public boolean ApriConnessione(String ip) throws UnknownHostException, IOException {
        InetAddress indirizzo = InetAddress.getByName(ip);
        Invia("a;" + mioNome + ";", indirizzo);
        fase = 2;
        //boolean flag = false;
        new Thread(() -> {
            byte[] buf = new byte[1500];
            DatagramPacket p = new DatagramPacket(buf, buf.length);
            try {
                socketRicezione.receive(p);
            } catch (IOException ex) {
                Logger.getLogger(GestionePacchetto.class.getName()).log(Level.SEVERE, null, ex);
            }
            try {
                if (controlloPaccketto(new String(p.getData()).trim(), null, false).equals("OK,connessione accettata")) {
                    flag = true;
                } else {
                    fase = 1;
                    return;
                }
            } catch (IOException ex) {
                Logger.getLogger(GestionePacchetto.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        while (!flag) {
            if (fase == 1) {
                return false;
            }
        }
        fase = 3;
        Invia("y;", indirizzo);
        connesso = true;
        return true;
    }
}
