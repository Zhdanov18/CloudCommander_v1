import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

public class AuthHandler extends ChannelInboundHandlerAdapter {
    private boolean authorized;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (msg == null) { return; }
            if (msg instanceof Request) {
                Request request = (Request) msg;
                switch (request.getAction()) {
                    case AUTHORIZATION:
                        AuthService authService = new SQLiteAuthService();
                        String nick = authService.getName(request.getArg(0), request.getArg(1));
                        authService.disconnect();
                        if (nick != null) {
                            authorized = true;
                            ctx.pipeline().addLast(new MainHandler(ctx, nick));
                            //возможно несколько подключений под одним логином
                        }
                        ctx.writeAndFlush(new AuthMessage(nick));
                        break;
                    case DISCONNECT:
                        ctx.pipeline().remove(ctx.pipeline().last());
                        authorized = false;
                        break;
                }
            }
            if (authorized) {
                ctx.fireChannelRead(msg);
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
