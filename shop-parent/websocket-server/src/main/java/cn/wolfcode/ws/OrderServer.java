package cn.wolfcode.ws;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OrderServer
 * description:
 * 2023/4/20 11:45
 * Create by 杰瑞
 */
@ServerEndpoint("/{token}")
@Component
@Slf4j
public class OrderServer {

    public static ConcurrentHashMap<String, Session> clients = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("token") String token) {
        log.info("浏览器与服务器建立连接");
        //建立浏览器的会话映射关系
        clients.put(token, session);
    }

//    @OnMessage
//    public void onMessage(@PathParam("token") String token, String msg) {
//        log.info("收到客户端标识为:{},发送的消息为:{}", token, msg);
//    }

    @OnClose
    public void onClose(@PathParam("token") String token) {
        log.info("浏览器与服务器连接断开");
        clients.remove(token);
    }

    @OnError
    public void onError(Throwable error){
        error.printStackTrace();
    }
}
