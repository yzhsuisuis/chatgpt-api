package cn.bugstack.chatgpt.application;

/*
 *@auther:yangzihe @洋纸盒
 *@discription:
 *@create 2025-01-08 15:41
 */
public interface IWeiXinValidateService {
    boolean checkSign(String signature,String timestamp,String nonce);
}
