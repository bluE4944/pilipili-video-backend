package com.pilipili.utils;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.crypto.Cipher;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Li
 * @version 1.0
 * @ClassName RSAUtil
 * @Description
 * @since 2023/3/23 13:42
 */
public class RSAUtil {

    /**
     * 公钥，可以写前端
     */
    public static String public_key="MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCCvcEblIslDk91/zsyPW9X2ZG9xhEEmPeT6LsK\n" +
            "7o9hzXn8Ue1ywPOQCHIWdkaHnTnEQbBQAVHh70zHKyN9XUzVVLXxl3Pz+mscBVLpJO/1xrVZf3Rb\n" +
            "9d9Yxww0AhOtx49RSfJuugWkF3/fCR3E0VKLNWDpzq0/SBdmuM1797uJyQIDAQAB";

    /**
     * 私钥，只能放后端
     */
    public static String private_key="MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAIK9wRuUiyUOT3X/OzI9b1fZkb3G\n" +
            "EQSY95Pouwruj2HNefxR7XLA85AIchZ2RoedOcRBsFABUeHvTMcrI31dTNVUtfGXc/P6axwFUukk\n" +
            "7/XGtVl/dFv131jHDDQCE63Hj1FJ8m66BaQXf98JHcTRUos1YOnOrT9IF2a4zXv3u4nJAgMBAAEC\n" +
            "gYB0ZdIF5wLWk94EtJ4URYPal7ZcWXmPOUy6H2pe+jMnJNOk45/eGZD+u7Yu71AtrrneU4lQQUyQ\n" +
            "NQeLDooDM8yRn1WebMGjrfz2mYz/VGWQm8KZHxu0MN3Gj8HWCy2gf1PAfc02t1z//k7lzl6LiiIg\n" +
            "e5+pgZyKt4L3l29JGqK5sQJBAL7BfzDIBmsbVIjx4ii1B7aOXvQ9/OyD1VWVjxHm5BND1WdgQaxI\n" +
            "KL3pNb3sjiT8zrA90d//ooYUI4I7u/21qh0CQQCvdWb4SPyosxR2inxXcFyFwCy3dibSItDUEB8W\n" +
            "W1dWuZpxHWP2FFKq1Ck3PHRxQzJCybtYXpdbR6D5I88R9S6dAkEAl96T9fF6crGqpvD03vXp8yT8\n" +
            "YjYr9N2s7luJMXaC5PefopMXFiPJFBHk8JWyQa5onBZLzqvG6DqGXrxSGlcU3QJAWjkgreO6KHWN\n" +
            "vDcSIVRh/1UGqYBUDhJhF+sCUVi+3JWsWSYn1M42hCl82C56IhPPsJBTiel6IzH+EJ7dR/qv8QJA\n" +
            "IvIaD7Qp8fXfcwUfZt1T5bePo6fB66Voj5mkqq2Ti0BbHL6BSsYRLl3sZwEM3+B2tkZLwkVBiyt0\n" +
            "59plceN6Mw==";


    public static void main(String[] args) {
        //解密数据
        try {
            //生成公钥和私钥
            genKeyPair();
            String publicKey = keyMap.get(0);
            //打印出来自己记录下
            System.out.println("公钥:" + publicKey);
            String privateKey = keyMap.get(1);
            //打印出来自己记录下
            System.out.println("私钥:" + privateKey);



            //获取到后，可以放这里，测试下能不能正确加解密
            publicKey = public_key;
            privateKey = private_key;



            String orgData = "test";
            System.out.println("原数据：" + orgData);

            //加密
            String encryptStr =encrypt(orgData,publicKey);
            System.out.println("加密结果：" + encryptStr);

            //解密
            String decryptStr = decrypt(encryptStr,privateKey);
            System.out.println("解密结果：" + decryptStr);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * RSA公钥加密
     *
     * @param str       加密字符串
     * @param publicKey 公钥
     * @return 密文
     * @throws Exception 加密过程中的异常信息
     */
    public static String encrypt(String str,String publicKey) throws Exception {
        //base64编码的公钥
        byte[] decoded = decryptBASE64(publicKey);
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        RSAPublicKey pubKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
        //RSA加密
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, pubKey);
        return encryptBASE64(cipher.doFinal(str.getBytes("UTF-8")));
    }

    /**
     * RSA私钥解密
     *
     * @param str        加密字符串
     * @param privateKey 私钥
     * @return 明文
     * @throws Exception 解密过程中的异常信息
     */
    public static String decrypt(String str, String privateKey) throws Exception {
        //64位解码加密后的字符串
        byte[] inputByte = decryptBASE64(str);
        //base64编码的私钥
        byte[] decoded = decryptBASE64(privateKey);
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        RSAPrivateKey priKey = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
        //RSA解密
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, priKey);
        String outStr = new String(cipher.doFinal(inputByte));
        return outStr;
    }

    /**
     * 编码返回字符串
     * @param key 密钥
     * @return
     * @throws Exception
     */
    public static String encryptBASE64(byte[] key) throws Exception {
        return (new BASE64Encoder()).encodeBuffer(key);
    }

    /**
     * 解码返回byte
     * @param key 密钥
     * @return
     * @throws Exception
     */
    public static byte[] decryptBASE64(String key) throws Exception {
        return (new BASE64Decoder()).decodeBuffer(key);
    }

    /**
     * 密钥长度 于原文长度对应 以及越长速度越慢
     */
    private final static int KEY_SIZE = 1024;
    /**
     * 用于封装随机产生的公钥与私钥
     */
    private static final Map<Integer, String> keyMap = new HashMap<Integer, String>();

    /**
     * 随机生成密钥对
     * @throws Exception
     */
    public static void genKeyPair() throws Exception {
        // KeyPairGenerator类用于生成公钥和私钥对，基于RSA算法生成对象
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
        // 初始化密钥对生成器
        keyPairGen.initialize(KEY_SIZE, new SecureRandom());
        // 生成一个密钥对，保存在keyPair中
        KeyPair keyPair = keyPairGen.generateKeyPair();
        // 得到私钥
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        // 得到公钥
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        String publicKeyString = encryptBASE64(publicKey.getEncoded());
        // 得到私钥字符串
        String privateKeyString = encryptBASE64(privateKey.getEncoded());
        // 将公钥和私钥保存到Map
        //0表示公钥
        keyMap.put(0, publicKeyString);
        //1表示私钥
        keyMap.put(1, privateKeyString);
    }
}
