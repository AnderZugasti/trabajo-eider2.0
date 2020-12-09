package com.company;

import javax.crypto.*;
import java.io.*;
import java.net.Socket;
import java.security.*;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Hilo extends Thread {
    private Socket c;
    private String texto;
    private String res1;
    private String res2;
    private String res3;
    private String respCorrec;


    public Hilo(Socket c) throws IOException {
        this.c = c;
    }

    public void run() {
        String nombre;
        int puntos = 0, aciertos = 0, errores = 0;
        PrivateKey prk = null;
        PublicKey puk = null, Cpuk = null;
        System.out.println("entra al hilo");
        ObjectInputStream ois = null;
        ObjectOutputStream oos = null;
        try {
            System.out.println("pasa");
            oos = new ObjectOutputStream(c.getOutputStream());

            System.out.println("pasa2");
            ois = new ObjectInputStream(c.getInputStream());

        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("se generan los flujos");
        boolean valido = false;
        //Patrones a validar
        Pattern patNombre = Pattern.compile("[a-zA-Z]{2,10}");
        Pattern patApellido = Pattern.compile("[a-zA-Z]{2,15}");
        Pattern patEdad = Pattern.compile("[0-9]{1,2}");
        Pattern patNick = Pattern.compile("[a-zA-z]{1,5}");
        Pattern patContrasenia = Pattern.compile("[A-Z]{2}[a-z0-9]");
        Matcher mat = null;
        //Se generan la clave pública y privada
        try {
            System.out.println("se generan llaves");
            KeyPairGenerator keygen = KeyPairGenerator.getInstance("RSA");
            keygen.initialize(1024);
            KeyPair keypair = keygen.generateKeyPair();
            prk = keypair.getPrivate();
            puk = keypair.getPublic();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        //Se empiezan a pedir datos datos
        //Se pide el nombre
        do {
            try {
                String recibido = validar("Izena idatzi: ", oos, ois);
                mat = patNombre.matcher(recibido);
                if (mat.find()) {
                    valido = true;
                    System.out.println(recibido + " valido: " + valido);
                }
                oos.writeObject(valido);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        } while (!valido);
        //Se pide el apellido
        valido = false;
        do {
            try {
                String recibido = validar("Abizena idatzi: ", oos, ois);
                mat = patApellido.matcher(recibido);
                if (mat.find()) {
                    valido = true;
                    System.out.println(recibido + " valido: " + valido);
                }
                oos.writeObject(valido);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        } while (!valido);
        valido = false;
        //Se pide la edad
        do {
            try {
                String recibido = validar("Adina idatzi: ", oos, ois);
                mat = patEdad.matcher(recibido);
                if (mat.matches()) {
                    valido = true;
                    System.out.println(recibido + " valido: " + valido);
                }
                oos.writeObject(valido);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        } while (!valido);
        //Se pide el nick
        valido = false;

        do {
            try {
                String recibido = validar("Nick-a idatzi, gehienez 5 letra zenbakirik gabe: ", oos, ois);

                mat = patNick.matcher(recibido);
                if (mat.matches()) {
                    valido = true;
                    System.out.println(recibido + " valido: " + valido);
                }
                oos.writeObject(valido);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        } while (!valido);
        //Se pide contraseña
        valido = false;
        do {
            try {
                String recibido = validar("Pasahitza, gutxienez bi letra larri: ", oos, ois);
                mat = patContrasenia.matcher(recibido);
                if (mat.find()) {
                    valido = true;
                    System.out.println(recibido + " valido: " + valido);
                }
                oos.writeObject(valido);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        } while (!valido);
        int respuesta = firma(oos, ois, prk, puk);
        //Comienza el juego
        if (respuesta == 1) {
            System.out.println(" que empiece el juego");
            int cont = 1;
            String resp = null;
            try {
                // Se envia la clave publica del server y se recibe la del cliente
                oos.writeObject(puk);
                Cpuk = (PublicKey) ois.readObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
            do {
                juego(cont);
                try {
                    //Se mandan preguntas
                    byte[] textoE = encriptar(texto, prk);
                    byte[] res1E = encriptar(res1, prk);
                    byte[] res2E = encriptar(res2, prk);
                    byte[] res3E = encriptar(res3, prk);
                    oos.writeObject(textoE);
                    oos.writeObject(res1E);
                    oos.writeObject(res2E);
                    oos.writeObject(res3E);
                    //Se recibe respuesta encriptada
                    byte[] respE = (byte[]) ois.readObject();
                    //Se desencripta respuesta
                    resp = desencriptar(respE, Cpuk);
                    //Se compara respuesta
                    if (resp.equals(respCorrec)) {
                        String msg = "erantzun zuzena";
                        oos.writeObject(msg);
                        aciertos++;
                        puntos += 2;
                    } else if (resp.equals("4")) {
                        String msg = "Milesker jolasteagtik,\nzure puntuaketa: " + puntos + "\neratzun zuzenak: " + aciertos + "\nerantzun okerrak: " + errores;
                        oos.writeObject(msg);
                    } else {
                        String msg = "erantzu okerra";
                        oos.writeObject(msg);
                        errores++;
                        puntos--;
                    }


                } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException | IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
                cont++;
                System.out.println(cont);
                System.out.println(resp);

            } while (cont <= 10 && !Objects.equals(resp, "4"));
            if (cont == 10) {
                String msg = "Milesker jolasteagtik,\nzure puntuaketa: " + puntos + "\neratzun zuzenak: " + aciertos + "\nerantzun okerrak: " + errores;
                try {
                    oos.writeObject(msg);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println(msg);
            }

        }
    }

    /**
     * La función validar, manda la petición de los datos requeridos y recibe una respuesta, la cual devuelve.
     *
     * @param requerido es el texto que va a pedir información al cliente.
     * @param oos       flujo de salida del socket.
     * @param ois       flujo de entrada del socket.
     **/
    public String validar(String requerido, ObjectOutputStream oos, ObjectInputStream ois) throws IOException, ClassNotFoundException {
        String recogida = requerido;
        oos.writeObject(recogida);
        String respuesta = (String) ois.readObject();
        return respuesta;
    }

    /**
     * La función firma, por un lado, genera las normas, las resume generando el hash y firma el resumen con su clave privada. Y después manda las normas en formato String,
     * el hash firmado y la clave pública del servidor, por último recibe un int que es un 1 o un 2 y lo devuelve.
     *
     * @param oos flujo de salida del socket.
     * @param ois flujo de entrada del socket.
     * @param prk la clave privada del servidor.
     * @param puk la clave pública del servidor.
     **/
    public int firma(ObjectOutputStream oos, ObjectInputStream ois, PrivateKey prk, PublicKey puk) {
        String normas = "Joko hau Egunean behin jolaserako entrenamendu moduan erabili daiteke, bertan agertzen diren galderekin egin baitut jolasa.\n" +
                "Kaixo, jolasten hasi baino lehen jolasaren jarraibideak onartu behar dituzu:\n" +
                "\nGalderak:\n" +
                "Galdera guztiak 3 erantzun posible izango ditu, laugarren aukera jolasetik ateratzeko izango da.\n" +
                "Galdera bat asmatzeagatik 2 puntu gehituko zaizkizu, baina akats bakoitzagatik puntu bat kenduko zaizu.\n" +
                "\nAmaiera:\n" +
                "Laugarren aukera aukeratzean jolasetik aterako zara eta zure puntuaketa ikusi ahal izango duzu.\n";
        int check = 2;
        //Se resume el texto(HASH)
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA");
            byte dataBytes[] = normas.getBytes();
            md.update(dataBytes);
            byte resumen[] = md.digest();

            //Se pasa a Hexadecimal

            String hex = "";
            for (int i = 0; i < resumen.length; i++) {
                String h = Integer.toHexString(resumen[i] & 0xF);
                if (h.length() == 1) {
                    hex += "0";
                }
                hex += h;
            }
            String Hexadecimal = hex.toUpperCase();
            Signature dsa = Signature.getInstance("SHA1WITHRSA");
            dsa.initSign(prk);
            dsa.update(Hexadecimal.getBytes());
            byte[] firma = dsa.sign();
            //Se envian clave pública, normas y el HexaCifrado
            System.out.println("Se le envía las normas");
            oos.writeObject(normas);//normas
            oos.writeObject(puk);//clave pública
            oos.writeObject(firma);
            //se recibe respuesta
            check = (int) ois.readObject();


        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException | IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return check;
    }

    /**
     * En la función juego lo que hacemos es mediante el contador que recivimos y un switch,
     * cambiamos el contenido de los valores de texto, res1, res2, res3, y respCorr, que son las preguntas.
     *
     * @param cont Contador que lleva el control de cual es la pregunta que toca.
     **/

    public void juego(int cont) {
        switch (cont) {
            case 1:
                Pregunta1();
                break;
            case 2:
                Pregunta2();
                break;
            case 3:
                Pregunta3();
                break;
            case 4:
                Pregunta4();
                break;
            case 5:
                Pregunta5();
                break;
            case 6:
                Pregunta6();
                break;
            case 7:
                Pregunta7();
                break;
            case 8:
                Pregunta8();
                break;
            case 9:
                Pregunta9();
                break;
            case 10:
                Pregunta10();
                break;
        }


    }

    /**
     * La función encriptar, encripta el texto que se le mete usando la clave privada del servidor y devuelve el texto encriptado.
     *
     * @param texto texto que se desea encriptar.
     * @param prk   clave privada del Server.
     **/

    public byte[] encriptar(String texto, PrivateKey prk) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher des = Cipher.getInstance("RSA");
        des.init(Cipher.ENCRYPT_MODE, prk);
        byte[] encrip = (des.doFinal(texto.getBytes()));
        return encrip;

    }

    /**
     *La función desencripta, desencripta la información recibida del cliente y devuelve un String, que es el texto desencriptado.
     * @param textoE es el texto encriptado que se recive del cliente
     * @param Cpuk es la clave pública del cliente, que se usará para desencriptar.
     **/
    public String desencriptar(byte[] textoE, PublicKey Cpuk) throws NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException, IllegalBlockSizeException, InvalidKeyException {
        Cipher des = Cipher.getInstance("RSA");
        des.init(Cipher.DECRYPT_MODE, Cpuk);
        String textoD = new String(des.doFinal(textoE));
        return textoD;
    }

    public void Pregunta1() {
        texto = "Istorioa kontatzen duen pertsonaia....";
        res1 = "Gidoilaria";
        res2 = "Narratzailea";
        res3 = "Protagonista";
        respCorrec = "2";

    }

    public void Pregunta2() {
        texto = "Sinonimoak, zein da okerra?";
        res1 = "Aldia - Garaia";
        res2 = "Umorea - Aldartea";
        res3 = "Laguntza - Gogoa";
        respCorrec = "3";

    }

    public void Pregunta3() {
        texto = "Euskal elkarteen federazioaren izena";
        res1 = "UZEI";
        res2 = "Euskaltzaleen topagunea";
        res3 = "Txioak";
        respCorrec = "2";

    }

    public void Pregunta4() {
        texto = "Noiz ospatzen dira San Lorentzoak?";
        res1 = "Abuztuak 10";
        res2 = "Abuztuak 11";
        res3 = "Abuztuak 12";
        respCorrec = "1";

    }

    public void Pregunta5() {
        texto = "Zein herrialdek du azalerarik handiena";
        res1 = "Guinea Bisau";
        res2 = "Bielorrusia";
        res3 = "Belgika";
        respCorrec = "2";

    }

    public void Pregunta6() {
        texto = "Non dago Ibarrola herria?";
        res1 = "Gipuzkoa";
        res2 = "Behe Nafarroa";
        res3 = "Araba";
        respCorrec = "2";

    }

    public void Pregunta7() {
        texto = "Berria egunkariaren Podkast feminista";
        res1 = "Berria FM";
        res2 = "Emakunde";
        res3 = "Xerezaderen Artxiboa";
        respCorrec = "1";

    }

    public void Pregunta8() {
        texto = "Zein dago gaizki idatzita?";
        res1 = "Biologolaria";
        res2 = "Zientzialaria";
        res3 = "Historialaria";
        respCorrec = "2";

    }

    public void Pregunta9() {
        texto = "Egun hartan itsasoa ..... zegoen.";
        res1 = "zurrun";
        res2 = "bare";
        res3 = "motel";
        respCorrec = "2";

    }

    public void Pregunta10() {
        texto = "Lehenengoari beti .... bigarrena";
        res1 = "darraio";
        res2 = "darraikio";
        res3 = "jarraitzen dio";
        respCorrec = "2";

    }


}
