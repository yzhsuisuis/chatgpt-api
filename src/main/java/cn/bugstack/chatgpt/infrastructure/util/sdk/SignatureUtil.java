package cn.bugstack.chatgpt.infrastructure.util.sdk;

import org.apache.commons.codec.binary.Hex;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/*
 *@auther:yangzihe @洋纸盒
 *@discription:
 *@create 2025-01-08 15:48
 */
public class SignatureUtil {
    /**
     * 将token、timestamp（URL参数中的）、nonce（URL参数中的）三个参数进行字典序排序，排序后结果为:["1714037059","486452656","AAAAA"]
     * 将三个参数字符串拼接成一个字符串："1714037059486452656AAAAA"
     * 进行sha1计算签名：899cf89e464efb63f54ddac96b0a0a235f53aa78
     * 与URL链接中的signature参数进行对比，相等说明请求来自微信服务器，合法
     * @param token
     * @param signature
     * @param timestamp
     * @param nonce
     * @return
     */
    public static boolean check(String token, String signature, String timestamp, String nonce) {
    //   1. 将除signature,以外的所有字符串按字典序排列,并串成一串
        String[] array = {token,timestamp,nonce};
        Arrays.sort(array);
        StringBuilder sb = new StringBuilder();
        for (String s : array) {
            sb.append(s);
        }
    //sha1加密
        String res = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] bytes = md.digest(sb.toString().getBytes());
            res = Hex.encodeHexString(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        return res !=null && res.equals(signature.toUpperCase());
    }
}
