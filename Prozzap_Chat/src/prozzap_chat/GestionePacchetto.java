/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package prozzap_chat;

import java.awt.List;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.JOptionPane;

/**
 *
 * @author Lorenzo
 */
public class GestionePacchetto extends Thread {

    String nomeMittente, mioNome;
    DatagramSocket socketRicezione, socketInvio;
    InetAddress IPaddress;
    boolean connesso;
    int fase;

    ActionEvent eventoMessaggio;

    public GestionePacchetto(NewJFrame listener) throws SocketException {
        this.mioNome = Character.toString((char) 0);
        this.socketRicezione = new DatagramSocket(12345);
        this.socketInvio = new DatagramSocket();
        fase = 1;
        connesso = false;
        this.frame = listener;
    }

    public void SetNome(String nome) {
        this.mioNome = nome;
    }

    @Override
    public void run() {
        while (true) {
            byte[] buf = new byte[1500];
            DatagramPacket p = new DatagramPacket(buf, buf.length);
            try {
                socketRicezione.receive(p);
            } catch (IOException ex) {
                System.out.println("Eccezione ricevimento pacchetto nella run della classe GestionePacchetto.\nErrore: " + ex.getLocalizedMessage());
            }
            new Thread(() -> {
                try {
                    controlloPaccketto(new String(p.getData()).trim(), p.getAddress());
                } catch (IOException ex) {
                    System.out.println("Eccezione controllo pacchetto nella thread interno della run della classe GestionePacchetto.\nErrore: " + ex.getLocalizedMessage());
                }
            }).start();
        }
    }
    private String controlloPaccketto(String info, InetAddress address) throws IOException {
        System.out.println("IN [" + info + "] [" + address + "]");
        char tipoRichiesta = info.charAt(0);
        String resto = info.substring(info.indexOf(";") + 1);
        String risposta = "";
        switch (tipoRichiesta) {
            case 'a':
                if (fase == 1 && !connesso) {
                    if (JOptionPane.showConfirmDialog(null, resto.split(";")[0] + " (" + address + ") ha chiesto di connettersi. Accettare?", "Richiesta", JOptionPane.YES_NO_OPTION) == 0) {
                        mioNome = JOptionPane.showInputDialog("Inserire nome");
                        nomeMittente = resto.split(";")[0];
                        IPaddress = address;
                        Invia("y;" + mioNome + ";", IPaddress);
                        connesso = true;

                        return "OK,apertura permessa";
                    } else {
                        Invia("n;", address);
                    }
                } else {
                    Invia("n;", address);
                }
                return "NOTOK,apertura non permessa";
            case 'y':
                if (connesso) {
                    System.out.println("Mi sono connesso a " + nomeMittente);
                    JOptionPane.showMessageDialog(null, "Sono connesso a " + nomeMittente, "Connessione effettuata.", JOptionPane.INFORMATION_MESSAGE);
                    return "OK,sono connesso";
                }
                if (fase == 2 && !connesso) {
                    nomeMittente = resto.split(";")[0];
                    flag = true;
                    return "OK,connessione accettata";
                } else {
                    Invia("n;", address);
                    return "NOTOK, connessione rifiutata";
                }
            case 'n':
                connesso = false;
                fase = 1;
                return "NOTOK, mi hanno rifiutato";
            case 'm':
                if (connesso) {
                    new Thread(() -> {
                        frame.MessaggioRicevuto("MESSAGGIO," + resto);
                    }).start();
                    return "MESSAGGIO," + resto;
                } else {
                    Invia("c;", address);
                }
            case 'c':
                if (connesso) {
                    fase = 1;
                    connesso = false;
                    return "OK, connessione chiusa";
                }
                System.out.println("Arrivato una richiesta di chiusura da " + address);
                return "0";
            default:
                throw new AssertionError();
        }
    }
    //rimettere 12345
    public void Invia(String info, InetAddress address) throws IOException {
        System.out.println("OUT [" + info + "] [" + address + "]");
        byte[] bufRisposta = info.getBytes();
        socketInvio.send(new DatagramPacket(bufRisposta, bufRisposta.length, address, 12345));
    }

    boolean flag = false;

    public boolean ApriConnessione(String ip) throws UnknownHostException, IOException, InterruptedException {
        InetAddress indirizzo = InetAddress.getByName(ip);
        Invia("a;" + mioNome + ";", indirizzo);
        fase = 2;
        long time = System.currentTimeMillis();
        while (!flag){
            if ((System.currentTimeMillis() - time) > 15000 && !connesso && fase == 2){
                connesso = false;
                fase = 1;
                flag = false;
                JOptionPane.showConfirmDialog(null, "Richiesta scaduta.", "Errore",JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE );
                return false;
            }
            System.out.print("");
        }
        if (fase == 1) {
            return false;
        }
        fase = 3;
        Invia("y;", indirizzo);
        connesso = true;
        IPaddress = indirizzo;
        System.out.println("Sono connesso a " + nomeMittente);
        JOptionPane.showMessageDialog(null, "Sono connesso a " + nomeMittente, "Connessione effettuata.", JOptionPane.INFORMATION_MESSAGE);
        return true;
    }

    public boolean InviaChiusura(InetAddress address) throws IOException {
        if (connesso) {
            fase = 1;
            connesso = false;
            System.out.println("Sto chiudendo connessione");
            Invia("c;", address);
            return true;
        } else {
            return false;
        }
    }

    public boolean InviaMessaggio(String messaggio, InetAddress address) throws IOException {
        if ((mioNome.equals(Character.toString((char) 0)))) {
            return false;
        }
        Invia(messaggio, address);
        return true;
    }

    public boolean ChiudiConnessione(InetAddress address) throws IOException {
        Invia("c;", address);
        connesso = false;
        fase = 1;
        return true;
    }

    private NewJFrame frame;
}
