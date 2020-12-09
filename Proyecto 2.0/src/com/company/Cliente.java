package com.company;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.chrono.IsoEra;

public class Cliente extends Thread {
    static int puerto = 5000;
    static boolean conexion = false;

    public static void main(String[] args) throws UnknownHostException, IOException, ClassNotFoundException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, IOException, SignatureException {
        KeyPairGenerator keygen = KeyPairGenerator.getInstance("RSA");
        keygen.initialize(1024);
        KeyPair keypair = keygen.generateKeyPair();
        PrivateKey prk = keypair.getPrivate();
        PublicKey puk = keypair.getPublic();
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        Socket socket = new Socket("localhost", puerto);
        ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
        ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
        boolean valido = false;
        //Izena
        do {
            valido = respuesta(oos, ois, br);
            if (valido) {
                System.out.println("Kaixo");
            } else {
                System.out.println("ez duzu zuzen idatzi");
            }
        } while (!valido);
        //abizena
        valido = false;
        do {
            valido = respuesta(oos, ois, br);
            if (valido) {
                System.out.println("Abizen zuzena, Jarraitu datuak sartzen");
            } else {
                System.out.println("ez duzu zuzen idatzi");
            }
        } while (!valido);
        //adina
        valido = false;
        do {
            valido = respuesta(oos, ois, br);
            if (valido) {
                System.out.println("Adina zuzena, Jarraitu datuak sartzen");
            } else {
                System.out.println("ez duzu zuzen idatzi");
            }
        } while (!valido);
        //Nick-a
        valido = false;
        do {
            valido = respuesta(oos, ois, br);
            if (valido) {
                System.out.println("Nick zuzena, Jarraitu datuak sartzen");
            } else {
                System.out.println("ez duzu zuzen idatzi");
            }
        } while (!valido);
        //Pasahitza
        valido = false;
        do {
            valido = respuesta(oos, ois, br);
            if (valido) {
                System.out.println("Pasahitza zuzena, Jarraitu datuak sartzen");
                System.out.println("Jolasaren arauak jasoko dituzu");
            } else {
                System.out.println("Pasahitza okerra");
            }
        } while (!valido);
        int eleccion = normas(oos, ois, br);
        if (eleccion == 1) {
            System.out.println("jokoa has dezala");
            int cont = 0;
            String resp = null;
            PublicKey Spuk = (PublicKey) ois.readObject();
            oos.writeObject(puk);
            do {
                byte[] textoE = (byte[]) ois.readObject();
                byte[] res1E = (byte[]) ois.readObject();
                byte[] res2E = (byte[]) ois.readObject();
                byte[] res3E = (byte[]) ois.readObject();
                String texto = desencriptar(textoE, Spuk);
                String res1 = desencriptar(res1E, Spuk);
                String res2 = desencriptar(res2E, Spuk);
                String res3 = desencriptar(res3E, Spuk);
                System.out.println(texto + "\n1-" + res1 + "\n2-" + res2 + "\n3-" + res3 + "\n4-Amaitu");
                resp = br.readLine();
                byte[] respE = encriptar(resp, prk);
                oos.writeObject(respE);
                String msg = (String) ois.readObject();
                System.out.println(msg);
                cont++;


            } while (cont <= 10 && !resp.equals("4"));
            if (cont == 10) {
                String msg = (String) ois.readObject();
                System.out.println(msg);
            }
        }
    }

    /**
     * La función respuesta, nos permite leer el texto de los datos que nos pide rellenar el servidor, después de enviárselos,
     * nos devuelve un booleano, con lo que sabremos si el dato metido es valido o no.
     *
     * @param oos flujo de salida del socket.
     * @param ois flujo de entrada del socket.
     * @param br  BufferedReader, para leer el teclado.
     **/
    public static boolean respuesta(ObjectOutputStream oos, ObjectInputStream ois, BufferedReader br) throws IOException, ClassNotFoundException {
        try {
            String texto = (String) ois.readObject();
            System.out.println(texto);
            String respuesta = br.readLine();
            oos.writeObject(respuesta);
        } catch (Exception e) {
            System.err.println(e);

        }

        boolean valido = (boolean) ois.readObject();
        return valido;
    }

    /**
     * La función normas, nos devuelve un 1 o un 2, recibimos el resumen de las normas firmadas, junto con la clave pública del Server y las normas en texto.
     * Resumimos el texto y lo comparamos con lo que nos ha llegado firmado. Si o es igual, no se seguirá jugando, en caso de que sea igual se le pregunta
     * al cliente a ver si quiere seguir jugando, en caso de que se siga con el juego devolverá un 1, en caso de que no se siga devolverá un 2.
     *
     * @param oos flujo de salida del socket.
     * @param ois flujo de entrada del socket.
     * @param br  BufferedReader, para leer el teclado.
     **/
    public static int normas(ObjectOutputStream oos, ObjectInputStream ois, BufferedReader br) throws IOException, ClassNotFoundException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        int eleccion = 2;
        //Se reciven las normas la clave pública y la firma
        String normas = (String) ois.readObject();
        PublicKey puk = (PublicKey) ois.readObject();
        byte[] firma = (byte[]) ois.readObject();
        MessageDigest md = MessageDigest.getInstance("SHA");
        byte dataBytes[] = normas.getBytes();
        md.update(dataBytes);
        byte resumen[] = md.digest();
        String hex = "";
        for (int i = 0; i < resumen.length; i++) {
            String h = Integer.toHexString(resumen[i] & 0xF);
            if (h.length() == 1) {
                hex += "0";
            }
            hex += h;
        }
        String Hexadecimal = hex.toUpperCase();
        Signature verificadsa = Signature.getInstance("SHA1WITHRSA");
        verificadsa.initVerify(puk);
        verificadsa.update(Hexadecimal.getBytes());
        boolean check = verificadsa.verify(firma);
        if (check) {

            do {
                System.out.println(normas + "\n Konexio fidagarria daukazu, jolastu nahi duzu?\n1-Bai\n2-Ez");
                eleccion = Integer.parseInt(br.readLine());
            } while (eleccion != 1 && eleccion != 2);
            if (eleccion == 1) {
                oos.writeObject(eleccion);
                System.out.println(eleccion);
            } else {
                oos.writeObject(eleccion);
                System.out.println("hurrengorate orduan!!");
            }
        } else {
            oos.writeObject(2);
            System.out.println("Konexioa ez da fidagarria, konexioa hizten");
        }
        return eleccion;
    }

    /**
     *La función desencriptar desencripta el texto enviado desde el servidor, usando la clave publica del servidor, y devuelve el texto desencriptado.
     *
     * @param texto el array de bytes recibido del server encriptado.
     * @param Spuk la clave pública del server.
     **/
    public static String desencriptar(byte[] texto, PublicKey Spuk) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher des = Cipher.getInstance("RSA");
        des.init(Cipher.DECRYPT_MODE, Spuk);
        String textoD = new String(des.doFinal(texto));
        return textoD;

    }

    /**
     * La función encriptar encripta mediante la clave privada del cliente la información que se va a enviar al servidor,
     * se le mete el String que se quiere encriptar y se devuelve un array de bytes encriptado.
     *
     * @param respuesta Texto que se desea encriptar.
     * @param prk clave privada del Cliente.
     * **/
    public static byte[] encriptar(String respuesta, PrivateKey prk) throws BadPaddingException, IllegalBlockSizeException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        Cipher des = Cipher.getInstance("RSA");
        des.init(Cipher.ENCRYPT_MODE, prk);
        byte[] encrip = des.doFinal(respuesta.getBytes());
        return encrip;
    }
}