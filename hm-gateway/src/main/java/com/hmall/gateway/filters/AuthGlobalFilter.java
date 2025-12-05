package com.hmall.gateway.filters;

import cn.hutool.core.text.AntPathMatcher;
import com.hmall.common.exception.UnauthorizedException;
import com.hmall.gateway.config.AuthProperties;
import com.hmall.gateway.util.JwtTool;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private final AuthProperties authProperties;

    private final JwtTool jwtTool;

    private final AntPathMatcher antPathMatcher=new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        if(isExclude(request.getPath().toString())){
            //放行
            return chain.filter(exchange);
        }

        String token=null;
        List<String> headers = request.getHeaders().get("authorization");
        if(headers!=null&&!headers.isEmpty()){
            token= headers.get(0);
        }

        Long userId=null;
        try {
            userId = jwtTool.parseToken(token);
        } catch (UnauthorizedException e) {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);  //401状态码
            return response.setComplete();    //说明已完结
        }

        //传递用户信息
        String userInfo = userId.toString();
        ServerWebExchange.Builder swe = exchange.mutate()
                .request(builder -> builder.header("user-info", userInfo));

        return chain.filter(exchange);
    }

    //路径校验
    private boolean isExclude(String path) {
        for (String pathPatter:authProperties.getExcludePaths()){
            if(antPathMatcher.match(pathPatter,path)){
                return true;
            }
        }
        return false;
    }


    @Override
    public int getOrder() {
        return 0;
    }
}
