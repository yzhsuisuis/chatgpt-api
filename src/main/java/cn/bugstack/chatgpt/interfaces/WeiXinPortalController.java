package cn.bugstack.chatgpt.interfaces;

import cn.bugstack.chatgpt.application.IWeiXinValidateService;
import cn.bugstack.chatgpt.common.Constants;
import cn.bugstack.chatgpt.domain.chat.ChatCompletionRequest;
import cn.bugstack.chatgpt.domain.chat.ChatCompletionResponse;
import cn.bugstack.chatgpt.domain.chat.Message;
import cn.bugstack.chatgpt.domain.receive.model.MessageTextEntity;
import cn.bugstack.chatgpt.infrastructure.util.XmlUtil;
import cn.bugstack.chatgpt.session.Configuration;
import cn.bugstack.chatgpt.session.OpenAiSession;
import cn.bugstack.chatgpt.session.defaults.DefaultOpenAiSessionFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/*
 *@auther:yangzihe @洋纸盒
 *@discription:处理微信的消息转发
 *@create 2025-01-08 14:22
 */
@RestController
@RequestMapping("/wx/portal/{appid}")
public class WeiXinPortalController {
    private Logger logger = LoggerFactory.getLogger(WeiXinPortalController.class);
    @Resource
    private ThreadPoolTaskExecutor taskExecutor;
    @Value("${wx.config.originalid:gh_3bbe530ed638}")
    private String originalId;

    //注意这里相当于暂时存储
    @Resource
    private IWeiXinValidateService weiXinValidateService;

    private OpenAiSession openAiSession;

    //开启线程够先让他置空

    private Map<String, String> chatGPTMap = new ConcurrentHashMap<>();

    public WeiXinPortalController()
    {
        Configuration configuration = new Configuration();
        configuration.setApiHost("https://pro-share-aws-api.zcyai.com/");
        configuration.setApiKey("sk-g5yElWC3hZGn5OCH326a179bE4444a639d81673eF7517eFb");
        //会话工厂
        DefaultOpenAiSessionFactory factory = new DefaultOpenAiSessionFactory(configuration);
        //开启会话
        this.openAiSession = factory.openSession();
    }
    /*
    *
    * consumes： 指定处理请求的提交内容类型(Content-Type)，例如application/json, text/html;

produces:    指定返回的内容类型，仅当request请求头中的(Accept)类型中包含该指定类型才返回；
    * */
    @GetMapping(produces = "text/plain;charset=utf-8")
    public String validate(@PathVariable String appid,
                           @RequestParam(value = "signature", required = false) String signature,
                           @RequestParam(value = "timestamp", required = false) String timestamp,
//                           随机支付串
                           @RequestParam(value = "nonce", required = false) String nonce,
//                           门户
                           @RequestParam(value = "echostr", required = false) String echostr) {
        try {
            logger.info("微信公众号验签信息{}开始 [{}, {}, {}, {}]", appid, signature, timestamp, nonce, echostr);
            if (StringUtils.isAnyBlank(signature, timestamp, nonce, echostr)) {
                throw new IllegalArgumentException("请求参数非法，请核实!");
            }
            boolean check = weiXinValidateService.checkSign(signature, timestamp, nonce);
            logger.info("微信公众号验签信息{}完成 check：{}", appid, check);
            if (!check) {
                return null;
            }
            return echostr;
        } catch (Exception e) {
            logger.error("微信公众号验签信息{}失败 [{}, {}, {}, {}]", appid, signature, timestamp, nonce, echostr, e);
            return null;

        }
    }

    /**
     * 刚开始的时候 map 获取对应的值是 null ,那没关系, 进入到我的 if 分支里
     * 很像一个饭店
     * 等号的(map. Get()= ="NULL")和刚来的 (map. Get= =null)都进屋里等着只有刚来的才会让后厨去在做一碗面 (开一个线程, 然后告诉顾客一会再问他好了没
     * 然后如果用户再叫他 (再次发送相同的请求) ,如果此时已经有答案了(面做好了), 则直接返回
     * @param appid
     * @param requestBody
     * @param signature
     * @param timestamp
     * @param nonce
     * @param openid
     * @param encType
     * @param msgSignature
     * @return
     */
    @PostMapping(produces = "application/xml; charset=UTF-8")
    public String post(@PathVariable String appid,
                       @RequestBody String requestBody,
                       @RequestParam("signature") String signature,
                       @RequestParam("timestamp") String timestamp,
                       @RequestParam("nonce") String nonce,
                       @RequestParam("openid") String openid,
                       @RequestParam(name = "encrypt_type", required = false) String encType,
                       @RequestParam(name = "msg_signature", required = false) String msgSignature) {
        logger.info("接收微信公众号信息请求{}开始 {}", openid, requestBody);
        MessageTextEntity message = XmlUtil.xmlToBean(requestBody, MessageTextEntity.class);
        //这里确实是一个比较巧妙的地方,当开线程池进行异步的时候


        if(chatGPTMap.get(message.getContent())==null || "NULL".equals(chatGPTMap.get(message.getContent())))
        {
            // 反馈信息[文本]
            MessageTextEntity res = new MessageTextEntity();
            res.setToUserName(openid);
            res.setFromUserName(originalId);
            res.setCreateTime(String.valueOf(System.currentTimeMillis() / 1000L));
            res.setMsgType("text");
            res.setContent("消息处理中，请再回复我一句【" + message.getContent().trim() + "】");
            if (chatGPTMap.get(message.getContent().trim()) == null) {
                doChatGPTTask(message.getContent().trim());
            }
            return XmlUtil.beanToXml(res);

        }
        // 反馈信息[文本]
        MessageTextEntity res = new MessageTextEntity();
        res.setToUserName(openid);
        res.setFromUserName(originalId);
        res.setCreateTime(String.valueOf(System.currentTimeMillis() / 1000L));
        res.setMsgType("text");
        res.setContent(chatGPTMap.get(message.getContent().trim()));
        String result = XmlUtil.beanToXml(res);
        logger.info("接收微信公众号信息请求{}完成 {}", openid, result);
        chatGPTMap.remove(message.getContent().trim());
        return result;
    }
    public void doChatGPTTask(String content) {
        chatGPTMap.put(content,"NULL");
        taskExecutor.execute(()->{
        //    发送消息
            ChatCompletionRequest chatCompletion = ChatCompletionRequest
                    .builder()
                    .messages(Collections.singletonList(Message.builder().role(Constants.Role.USER).content(content).build()))
                    .model(ChatCompletionRequest.Model.GPT_3_5_TURBO.getCode())
                    .build();
        //    发起请求
            ChatCompletionResponse chatCompletionResponse = openAiSession.completions(chatCompletion);

        //    3. 解析结果
            StringBuilder messages = null;
            chatCompletionResponse.getChoices().forEach(e->{
                messages.append(e.getMessage().getContent());

            });
            chatGPTMap.put(content,messages.toString());

        });

    }

}
