package cn.bugstack.chatgpt.domain.validate;

import cn.bugstack.chatgpt.application.IWeiXinValidateService;
import cn.bugstack.chatgpt.infrastructure.util.sdk.SignatureUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/*
 *@auther:yangzihe @洋纸盒
 *@discription:
 *@create 2025-01-08 15:43
 */
@Service
public class WeiXinValidateServiceImpl implements IWeiXinValidateService {

    @Value("${wx.config.token}")
    private String token;

    /**
     * 将token、timestamp（URL参数中的）、nonce（URL参数中的）三个参数进行字典序排序，排序后结果为:["1714037059","486452656","AAAAA"]
     * 将三个参数字符串拼接成一个字符串："1714037059486452656AAAAA"
     * 进行sha1计算签名：899cf89e464efb63f54ddac96b0a0a235f53aa78
     * 与URL链接中的signature参数进行对比，相等说明请求来自微信服务器，合法
     * @param signature
     * @param timestamp
     * @param nonce
     * @return
     */
    @Override
    public boolean checkSign(String signature, String timestamp, String nonce) {
        return SignatureUtil.check(token, signature, timestamp, nonce);
    }
}
