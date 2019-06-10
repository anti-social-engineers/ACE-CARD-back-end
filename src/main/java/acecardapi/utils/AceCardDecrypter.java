/*
 * Copyright (c) 2019. Ralph Verburg & Anti-Social Engineers
 *
 *  All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT license.
 *
 */

package acecardapi.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

public class AceCardDecrypter {

    //private static final String key = "C*F-JaNdRgUjXn2r5u8x/A?D(G+KbPeS";

    public static String decrypt(String encryptedCardId, String key) {
        try {

            var removePadding = encryptedCardId.substring(0, (encryptedCardId.length() - 4));

            //Decode the message from base64
            var decodedMessage = Base64.getDecoder().decode(removePadding);
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
