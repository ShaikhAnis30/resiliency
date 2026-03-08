package com.practice.resiliency.config;

import java.util.concurrent.ExecutorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.http11.Http11NioProtocol;
import org.springframework.boot.web.embedded.tomcat.TomcatProtocolHandlerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class VirtualThreadConfig {

  private final ExecutorService executorService;

  @Bean
  public TomcatProtocolHandlerCustomizer<Http11NioProtocol> customizer() {
    log.info("-----Configuring Tomcat to use virtual threads executor for request handling.-----");
    return protocol -> protocol.setExecutor(executorService);
  }
}
