package io.github.cspigot.utils.netty;

import io.github.cspigot.utils.traffic.StaticConstants;

import javax.crypto.Cipher;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * This Project is property of Refine Development.
 * Copyright © 2023, All Rights Reserved.
 * Redistribution of this Project is not allowed.
 *
 * @author mathssw , Sakiio_
 * @since 8/2/2023
 * @version cSpigot
 */
public class DecryptionUtil {

    public static String decrypt(String json) {
        try {
            Cipher cipher = Cipher.getInstance("RSA");

            X509EncodedKeySpec spec = new X509EncodedKeySpec(Base64.getDecoder().decode(StaticConstants.KEY));
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(spec);

            cipher.init(Cipher.DECRYPT_MODE, publicKey);

            return new String(cipher.doFinal(Base64.getUrlDecoder().decode(json)));
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalStateException("Decrypting failed");
        }
    }

}
