package com.adtiming.om.server.cp.util;

import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ECDSAUtil {

    private static final Logger LOG = LogManager.getLogger();

    public static final String KEY_ALGORITHM = "EC";
    public static final String SIGNATURE_ALGORITHM = "SHA256withECDSA";

    public static PrivateKey buildSkPrivateKey(String strPrivateKey) {
        byte[] keyBytes = Base64.decodeBase64(strPrivateKey);
        PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(keyBytes);
        try {
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
            return keyFactory.generatePrivate(pkcs8KeySpec);
        } catch (Exception e) {
            LOG.error("buildSkPrivateKey error", e);
        }
        return null;
    }

    /**
     * 加密<br>
     * 用私钥加密
     * The Elliptic Curve Digital Signature Algorithm (ECDSA) with a SHA-256 hash.
     *
     * @param data data
     * @param key  private key
     * @return signature
     * @throws Exception runtime exception
     */
    public static String encodeSHA256WithECDSA(String data, String key)
            throws Exception {
        byte[] keyBytes = Base64.decodeBase64(key);
        PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
        PrivateKey privateKey = keyFactory.generatePrivate(pkcs8KeySpec);
        Signature ecdsaSign = Signature.getInstance(SIGNATURE_ALGORITHM);
        ecdsaSign.initSign(privateKey);
        ecdsaSign.update(data.getBytes(UTF_8));
        byte[] signature = ecdsaSign.sign();
        return Base64.encodeBase64String(signature);
    }

    /**
     * 加密<br>
     * 用私钥加密
     * The Elliptic Curve Digital Signature Algorithm (ECDSA) with a SHA-256 hash.
     *
     * @param data       data
     * @param privateKey private key
     * @return signature
     * @throws Exception runtime exception
     */
    public static String encodeSHA256WithECDSA(String data, PrivateKey privateKey)
            throws Exception {
        Signature ecdsaSign = Signature.getInstance(SIGNATURE_ALGORITHM);
        ecdsaSign.initSign(privateKey);
        ecdsaSign.update(data.getBytes(UTF_8));
        byte[] signature = ecdsaSign.sign();
        return Base64.encodeBase64String(signature);
    }

    /**
     * 签名校验<br>
     * 用公钥验签
     * The Elliptic Curve Digital Signature Algorithm (ECDSA) with a SHA-256 hash.
     *
     * @param data data
     * @param key  public key
     * @param sign sign
     * @return true or false
     * @throws Exception exception
     */
    public static boolean verifySHA256WithECDSA(String data, String key, String sign)
            throws Exception {
        byte[] keyBytes = Base64.decodeBase64(key);
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
        PublicKey publicKey = keyFactory.generatePublic(x509KeySpec);
        Signature ecdsaSign = Signature.getInstance(SIGNATURE_ALGORITHM);
        ecdsaSign.initVerify(publicKey);
        ecdsaSign.update(data.getBytes(UTF_8));
        return ecdsaSign.verify(Base64.decodeBase64(sign));
    }
}
