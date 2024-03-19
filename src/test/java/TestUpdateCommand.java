import cn.powernukkitx.cli.cmd.UpdateCommand;

public class TestUpdateCommand {
    public static void main(String[] args) throws Exception {
        UpdateCommand updateCommand = new UpdateCommand();
        updateCommand.all = true;
        System.exit(updateCommand.call());
    }
}
