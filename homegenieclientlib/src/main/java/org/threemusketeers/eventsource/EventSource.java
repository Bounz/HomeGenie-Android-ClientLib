/** Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 *
 * @author <a href='mailto:th33musk3t33rs@gmail.com'>3.musket33rs</a>
 *
 * @since 0.1
 */
package org.threemusketeers.eventsource;

import com.glabs.homegenie.client.Control;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.CharsetUtil;

import java.net.URI;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * Implementation of the EventSource java client compliant with
 * http://www.w3.org/TR/2012/WD-eventsource-20121023/
 */
public class EventSource {

    private EventLoopGroup group;
    private Bootstrap bootstrap;
    private URI uri;
    private EventSourceClientHandler handler;
    private ChannelFuture channelFuture;

    private boolean useSsl, sslAcceptAll;

    public EventSource(String url,String user, String password, EventLoopGroup group, EventSourceNotification notification, boolean disableAutoReconnect) {
        //System.out.println("[EventSource] constructor start");
        this.group = group;
        this.uri = URI.create(url);
        String protocol = uri.getScheme();
        if (!"http".equals(protocol) && !"https".equals(protocol)) {
            notification.onError("Unsupported protocol: " + protocol + " for URL " + url);
            return;
        }

        handler = new EventSourceClientHandler(uri, user, password, notification, this, disableAutoReconnect);
        channelFuture = createBootstrap();
        //System.out.println("[EventSource] constructor end");
    }

    public void setSsl(boolean enable) {
        useSsl = enable;
    }
    public void setSsl(boolean enable, boolean acceptAllCertificates) {
        useSsl = enable;
        sslAcceptAll = acceptAllCertificates;
    }

    public void close() {
        //System.out.println("[EventSource] closing...");
        if (channelFuture != null) {
            try {
                //System.out.println("[EventSource] waiting pending channelFuture operation");
                channelFuture.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            channelFuture = null;
        }
        if (handler != null) {
            //System.out.println("[EventSource] closing handler");
            handler.close();
            handler = null;
        }
        //System.out.println("[EventSource] closed");
    }

    /**
     * Bogus trust manager accepting any certificate
     */
    private static class BogusTrustManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] certs, String s) {
            // nothing
        }

        @Override
        public void checkServerTrusted(X509Certificate[] certs, String s) {
            // nothing
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

    ChannelFuture createBootstrap() {
        bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline p = ch.pipeline();

                        if (useSsl) {
                            SSLContext sslContext = SSLContext.getInstance("TLS");
                            if (sslAcceptAll) {
                                sslContext.init(null, new TrustManager[]{new BogusTrustManager()}, null);
                            } else {
                                TrustManagerFactory tmFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                                KeyStore tmpKS = null;
                                tmFactory.init(tmpKS);
                                TrustManager[] tm = tmFactory.getTrustManagers();
                                sslContext.init(null, tm, null);
                            }
                            SSLEngine sslEngine = sslContext.createSSLEngine();
                            sslEngine.setUseClientMode(true);
                            p.addFirst("ssl", new SslHandler(sslEngine));
                        }

                        //Lines must be separated by either a U+000D CARRIAGE RETURN U+000A LINE FEED (CRLF) character pair, 
                        //a single U+000A LINE FEED (LF) character, 
                        //or a single U+000D CARRIAGE RETURN (CR) character.
                        p.addLast(new HttpRequestEncoder(),
                                  new DelimiterBasedFrameDecoder(Integer.MAX_VALUE, new ByteBuf[] {
                                          Unpooled.wrappedBuffer(new byte[]{'\r', '\n'}),
                                          Unpooled.wrappedBuffer(new byte[] { '\n' }),
                                          Unpooled.wrappedBuffer(new byte[] { '\r' })}),
                                  new StringDecoder(CharsetUtil.UTF_8),
                                  handler);
                    }
                });

        int port = uri.getPort();
        if(port <= 0) {
            String protocol = uri.getScheme();
            if ("http".equals(protocol)) {
                port = 80;
            } else {
                port = 443;
            }
        }
        return bootstrap.connect(uri.getHost(), port);
    }

    protected void finalize() throws Throwable {
        this.close();
    }
}
