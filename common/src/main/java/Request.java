public class Request extends AbstractMessage {
    private Const.Action action;
    private String[] arg;

    public Request(Const.Action action, String ...arg) {
        this.action = action;
        this.arg = arg;
    }

    public Const.Action getAction() { return action; }
    public String getArg(int index) { return arg[index]; }
}
//AUTHORIZATION: login, password
//CREATE:        path,  directory_name
//DISCONNECT:    name,  null
//COPY:          name,  filename
//LIST:          name,  path     (null - переход в вышестоящий каталог)
//REMOVE:        null,  path
//name - необязательные параметры