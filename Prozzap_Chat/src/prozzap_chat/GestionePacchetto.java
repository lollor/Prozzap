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
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 *
 * @author Lorenzo
 */
public class GestionePacchetto extends Thread {

    String nomeMittente, mioNome;
    DatagramSocket socketRicezione, socketInvio;
    boolean connesso;
    int fase;

    public GestionePacchetto() throws SocketException {
        this.mioNome = Character.toString((char)0);
        this.socketRicezione = new DatagramSocket(12345);
        this.socketInvio = new DatagramSocket();
        fase = 1;
        connesso = false;
    }
    public void SetNome(String nome){
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
                    controlloPaccketto(new String(p.getData()).trim(), p.getAddress(), true);
                } catch (IOException ex) {
                    System.out.println("Eccezione controllo pacchetto nella thread interno della run della classe GestionePacchetto.\nErrore: " + ex.getLocalizedMessage());
                }
            }).start();
        }
    }

    private String controlloPaccketto(String info, InetAddress address, boolean sendPacket) throws IOException {
        System.out.println(info + " - " + address);
        char tipoRichiesta = info.charAt(0);
        String resto = info.substring(info.indexOf(";") + 1);
        String risposta = "";
        switch (tipoRichiesta) {
            case 'a':
                if (fase == 1 && !connesso) {
                    if (JOptionPane.showConfirmDialog(null, resto.split(";")[0] + " (" + address + ") ha chiesto di connettersi. Accettare?", "Richiesta", JOptionPane.YES_NO_OPTION) == 0) {
                        mioNome = JOptionPane.showInputDialog("Inserire nome");
                        nomeMittente = resto.split(";")[0];
                        Invia("y;" + mioNome + ";", address);
                        return "OK,apertura permessa";
                    } else {
                        Invia("n;", address);
                    }
                } else {
                    Invia("n;", address);
                }
                return "NOTOK,apertura non permessa";
            case 'y':
                if (fase == 2 && !connesso) {
                    nomeMittente = resto.split(";")[0];
                    return "OK,connessione accettata";
                } else {
                    Invia("n;", address);
                    return "NOTOK, connessione rifiutata";
                }
            case 'n':
                connesso = false;
                return "NOTOK, mi hanno rifiutato";
            case 'm':
                if (connesso) {
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

    public void Invia(String info, InetAddress address) throws IOException {
        byte[] bufRisposta = info.getBytes();
        socketInvio.send(new DatagramPacket(bufRisposta, bufRisposta.length, address, 12345));
    }

    public boolean ApriConnessione(String ip) throws UnknownHostException, IOException, InterruptedException {
        InetAddress indirizzo = InetAddress.getByName(ip);
        Invia("a;" + mioNome + ";", indirizzo);
        fase = 2;
        //boolean flag = false;
        Thread ricevimentoPacchetto = new Thread(() -> {
            byte[] buf = new byte[1500];
            DatagramPacket p = new DatagramPacket(buf, buf.length);
            try {
                socketRicezione.setSoTimeout(5000);
            } catch (SocketException ex) {
                System.out.println("Eccezione socketRicezione.setSoTimeout(5000) nel thread dentro ApriConnessione(String ip).\nErrore: " + ex.getLocalizedMessage());
            }
            try {
                socketRicezione.receive(p);
            } catch (SocketTimeoutException ex) {
                fase = 1;
                try {
                    socketRicezione.setSoTimeout(0);
                } catch (SocketException ex1) {
                    System.out.println("Eccezione 1° socketRicezione.setSoTimeout(0) nel thread dentro ApriConnessione(String ip).\nErrore: " + ex1.getLocalizedMessage());
                }
                return;
            } catch (IOException ex) {
                System.out.println("Eccezione socketRicezione.receive(p) nel thread dentro ApriConnessione(String ip).\nErrore: " + ex.getLocalizedMessage());
            }
            try {
                socketRicezione.setSoTimeout(0);
            } catch (SocketException ex) {
                System.out.println("Eccezione 2° socketRicezione.setSoTimeout(0) nel thread dentro ApriConnessione(String ip).\nErrore: " + ex.getLocalizedMessage());
            }
            try {
                if (controlloPaccketto(new String(p.getData()).trim(), null, false).equals("OK,connessione accettata")) {
                    return;
                } else {
                    fase = 1;
                    return;
                }
            } catch (IOException ex) {
                System.out.println("Eccezione controllo pacchetto nella thread ricevimentoPacchetto dentro ApriConnessione(String ip).\nErrore: " + ex.getLocalizedMessage());
            }
        });
        ricevimentoPacchetto.start();
        ricevimentoPacchetto.join();
        if (fase == 1) {
            return false;
        }
        fase = 3;
        Invia("y;", indirizzo);
        connesso = true;
        return true;
    }

    public boolean InviaChiusura(InetAddress address) throws IOException {
        if (connesso) {
            fase = 1;
            connesso = false;
            System.out.println("Sto chiudendo connessione");
            Invia("c;",address);
            return true;
        } else {
            return false;
        }
    }
    public boolean InviaMessaggio(String messaggio, InetAddress address) throws IOException{
        if (!(mioNome.equals(Character.toString((char)0)))){
            return false;
        }
        Invia(messaggio, address);
        return true;
    }
}
