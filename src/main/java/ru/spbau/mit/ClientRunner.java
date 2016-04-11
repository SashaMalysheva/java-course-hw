package ru.spbau.mit;

import java.io.IOException;
import java.util.ArrayList;

public final class ClientRunner {
    private static final int NEW_FILE_PATH_ARG = 3;
    private static final int NEW_FILE_CNT_ARGS = 4;
    private static final int GET_FILE_ID_ARG = 3;
    private static final int GET_FILE_NAME_ARG = 4;
    private static final int GET_CNT_ARGS = 5;
    private static final int RUN_ARG_PORT = 3;
    private static final int RUN_CNT_ARGS = 4;

    private ClientRunner() {}

    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length < 3) {
            usage();
        }
        String host = args[0];
        String pathInfo = args[1];
        String query = args[2];

        Client client = new Client(host, pathInfo);
        String name;
        switch (query) {
            case "NEW_FILE":
                if (args.length < NEW_FILE_CNT_ARGS) {
                    usage();
                }
                name = args[NEW_FILE_PATH_ARG];
                client.newFile(name);
                break;
            case "GET" :
                if (args.length < GET_CNT_ARGS) {
                    usage();
                }
                int id = Integer.parseInt(args[GET_FILE_ID_ARG]);
                name = args[GET_FILE_NAME_ARG];
                client.get(id, name);
                break;
            case "LIST" :
                ArrayList<FInfo> files = client.list();
                for (FInfo fi : files) {
                    System.out.println(fi.getId() + " " + fi.getSize() + " " + fi.getName());
                }
                break;
            case "RUN" :
                if (args.length < RUN_CNT_ARGS) {
                    usage();
                }
                client.run(Integer.parseInt(args[RUN_ARG_PORT]));
                break;
            default:
                usage();
        }
        client.saveState();
    }

    private static void usage() {
        System.err.print("java ru.spbau.mit.ClientRunner host path LIST|GET|NEW_FILE|RUN\n");
        System.exit(1);
    }
}
