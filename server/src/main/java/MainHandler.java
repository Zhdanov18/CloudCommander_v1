import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MainHandler extends ChannelInboundHandlerAdapter {
    private ClientHandler client;

    public MainHandler(ChannelHandlerContext ctx, String nick) {
        super();
        this.client = new ClientHandler(nick);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        try {
            if (msg == null) { return; }
            if (msg instanceof Request) {
                Request request = (Request) msg;
                Path path = Paths.get(client.getPath(), ((request.getArg(0) != null) ? File.separator + request.getArg(0) : ""), ((request.getArg(1) != null) ? File.separator + request.getArg(1) : ""));
                switch (request.getAction()) {
                    case COPY:
                        //надо будет передавать в request MyFileDescriptor, чтобы отличить каталог и файл с одинаковыми именами
                        try {
                            Files.walkFileTree(path, new MyFileVisitor(Const.Action.COPY, path,
                                    args -> ctx.writeAndFlush(args[0])));
                        } catch (IOException e) {
                            ctx.writeAndFlush(new ErrorMessage(e, ActionHandler.AlertText.COPY_ERR, path.toString()));
                        }
                        break;
                    case CREATE:
                        if (ActionHandler.createDirectory(path, null)) {
                            client.refreshData();
                            ctx.writeAndFlush(new FolderMessage(client.getPath(), client.getData()));
                        } else {
                            ctx.writeAndFlush(new ErrorMessage(null, ActionHandler.AlertText.CREATE_DIR_ERR, path.toString()));
                        }
                        break;
                    case LIST:
                        client.changeDirectory(request.getArg(1));
                        ctx.writeAndFlush(new FolderMessage(client.getPath(), client.getData()));
                        break;
                    case REMOVE:
                        try {
                            Files.walkFileTree(path, new MyFileVisitor(Const.Action.REMOVE, path, null));
                        } catch (IOException e) {
                            ctx.writeAndFlush(new ErrorMessage(e, ActionHandler.AlertText.DELETE_ERR, path.toString()));
                        }
                        client.refreshData();
                        ctx.writeAndFlush(new FolderMessage(client.getPath(), client.getData()));
                        break;
                }
            }
            if (msg instanceof FileMessage) {
                ActionHandler.writeFileMsg((FileMessage) msg, client.getPath());
                client.refreshData();
                ctx.writeAndFlush(new FolderMessage(client.getPath(), client.getData()));
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
