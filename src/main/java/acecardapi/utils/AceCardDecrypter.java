import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

public class AceCardDecrypter {

    //private static final String key = "C*F-JaNdRgUjXn2r5u8x/A?D(G+KbPeS";

    public static void main(String[] args) {
        //Test returns string of 16 characters (the length of our cardId)
        //Remove Main later
        System.out.println(decrypt("TVobdap635jvPSW9KShf1ZuBDLDuh8a7syXEjl58+Vs=", "C*F-JaNdRgUjXn2r5u8x/A?D(G+KbPeS"));
    }

    public static String decrypt(String encryptedCardId, String key) {
        try {
            //Decode the message from base64
            var decodedMessage = Base64.getDecoder().decode(encryptedCardId);
            //First 16 bytes are the initialization vector
            var iv = Arrays.copyOfRange(decodedMessage, 0, 16);

            //Setup iv and key
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            SecretKeySpec keySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");

            //Create instance of cipher (We use CBC Mode WITHOUT padding since our message is already 16 bytes long)
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

            var decrypted_byte_array = cipher.doFinal(Arrays.copyOfRange(decodedMessage, 16, decodedMessage.length));
            return new String(decrypted_byte_array, StandardCharsets.UTF_8);
        }
        catch (Exception e){
            //TODO Aaron API Exception Afhandelen
        }
    return null;
    }
}
